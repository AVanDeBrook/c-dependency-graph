/**
 *
 * @copyright &copy; 2010 - 2020, Fraunhofer-Gesellschaft zur Foerderung der
 *  angewandten Forschung e.V. All rights reserved.
 *
 * BSD 3-Clause License
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1.  Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 3.  Neither the name of the copyright holder nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * We kindly request you to use one or more of the following phrases to refer
 * to foxBMS in your hardware, software, documentation or advertising
 * materials:
 *
 * &Prime;This product uses parts of foxBMS&reg;&Prime;
 *
 * &Prime;This product includes parts of foxBMS&reg;&Prime;
 *
 * &Prime;This product is derived from foxBMS&reg;&Prime;
 *
 */

/**
 * @file    contactor.c
 * @author  foxBMS Team
 * @date    23.09.2015 (date of creation)
 * @ingroup DRIVERS
 * @prefix  CONT
 *
 * @brief   Driver for the contactors.
 *
 */

/*================== Includes =============================================*/
#include "contactor.h"
#include "bms.h"
#include "database.h"
#include "diag.h"
#include "FreeRTOS.h"
#include "task.h"


//#if BUILD_MODULE_ENABLE_CONTACTOR == 1
/*================== Macros and Definitions ===============================*/
/**
 * Saves the last state and the last substate
 */
#define CONT_SAVELASTSTATES()   cont_state.laststate = cont_state.state; \
                                cont_state.lastsubstate = cont_state.substate;


/*================== Constant and Variable Definitions ====================*/
/**
 * used to locally copy the current-sensor value from the global database
 * current table
 */
static DATA_BLOCK_CURRENT_SENSOR_s cont_current_tab = {0};

/**
 * contains the state of the contactor state machine
 *
 */
static CONT_STATE_s cont_state = {
    .timer                  = 0,
    .statereq               = CONT_STATE_NO_REQUEST,
    .state                  = CONT_STATEMACH_UNINITIALIZED,
    .substate               = CONT_ENTRY,
    .laststate              = CONT_STATEMACH_UNINITIALIZED,
    .lastsubstate           = 0,
    .triggerentry           = 0,
    .ErrRequestCounter      = 0,
    .initFinished           = E_NOT_OK,
    .OscillationCounter     = 0,
    .PrechargeTryCounter    = 0,
    .PrechargeTimeOut       = 0,
    .counter                = 0,
    .activePowerLine        = CONT_POWER_LINE_NONE,
};

static DATA_BLOCK_CONTFEEDBACK_s cont_feedback_tab = {
        .contactor_feedback = 0,
        .timestamp = 0,
        .previous_timestamp = 0,
};


/*================== Static Function Prototypes ==================================*/

static int prnt(char *string);

static bool cont_NewStandbyRequestExists(void);
static bool cont_NewErrorRequestExists(void);
static void cont_CloseMainPlusWithPrecharge (void);

static bool cont_IsReentry(void);
static void cont_CheckAllContactorsFeedback(void);
static CONT_RETURN_TYPE_e cont_CheckStateRequest(CONT_STATE_REQUEST_e statereq);
static CONT_STATE_REQUEST_e cont_GetStateRequest(void);
static CONT_STATE_REQUEST_e cont_TransferStateRequest(void);

// The following two functions are used for testing output channels only
static void cont_TestLatchingContactorChannels(void);
static void cont_TestAllContactorChannels(void);

// LMM: function to save contactor state to database
static void cont_SaveContStateToDatabase(DATA_BLOCK_CONTACTORSTATE_s *data_block, CONT_STATEMACH_e contactor_state);
// LMM: function to save contactor sub-state to database
static void cont_SaveContSubStateToDatabase(DATA_BLOCK_CONTACTORSTATE_s *data_block, CONT_STATEMACH_SUB_e contactor_substate);

/*================== Static Function Implementations =============================*/
// This is a temporary function for printing.
// tested
static int prnt(char *string) {
    // vTaskDelay(500);
    static int prntNumber = 0;
    taskENTER_CRITICAL();
    printf("%d. ", prntNumber++);
    int rtnVal = printf(string);
    taskEXIT_CRITICAL();
    return rtnVal;
}

/**
 * @brief   checks if there is new Standby request
 *
 * @details This function checks if we have the following request: 
 *          (1) CONT_STATE_STANDBY_REQUEST.
 *          If so, the new state will be the transit OPEN CONTACTOR state. 
 *
 * @return  true if there is a new Standby request, false else.
 */
static bool cont_NewStandbyRequestExists(void) {
    bool stateRequestExists = true;
    CONT_STATE_REQUEST_e statereq = cont_TransferStateRequest();
    if (statereq == CONT_STATE_STANDBY_REQUEST) {
        CONT_SAVELASTSTATES();
        cont_state.state = CONT_STATEMACH_OPEN_CONT;
        cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
        cont_state.substate = CONT_ENTRY;
    } else {
        stateRequestExists = false;
    }
    return stateRequestExists;
}

/**
 * @brief   checks if there is new Error request
 *
 * @details This function checks if we have the following request:
 *          (1) CONT_STATE_ERROR_REQUEST.
 *          If so, the new state will be the Error State.
 *
 * @return  true if there is a new Standby request, false else.
 */
static bool cont_NewErrorRequestExists(void) {
    bool stateRequestExists = true;
    CONT_STATE_REQUEST_e statereq = cont_TransferStateRequest();
    if (statereq == CONT_STATE_ERROR_REQUEST) {
        CONT_SAVELASTSTATES();
        cont_state.state = CONT_STATEMACH_ERROR;
        cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
        cont_state.substate = CONT_ENTRY;
    } else {
        stateRequestExists = false;
    }
    return stateRequestExists;
}

/**
 * @brief   closes the Main Plus contactor along with the Precharge contactor
 *
 * @details closes the Main Plus contactor in three steps:
 *          (1) Close the precharge contactor
 *          (2) Close the Main Plus contactor
 *          (3) Open the precharge contactor
 *
 *          Note that this code only works stand alone. It does not work in the
 *          context of state machine since it uses blocking delay.
 *          It is not removed since it provides a clear step-by-step flow.
 */
static void cont_CloseMainPlusWithPrecharge(void) {
    DATA_BLOCK_CONTACTORSTATE_s contactorstate;
    if (cont_state.substate == CONT_PRECHARGE_CLOSE_PRECHARGE) {
        CONT_CLOSEPRECHARGE();
        cont_state.timer = CONT_PRECHARGE_TIME_MS;
        cont_state.substate = CONT_PRECHARGE_CLOSE_MAIN_PLUS;
    } else if (cont_state.substate == CONT_PRECHARGE_CLOSE_MAIN_PLUS) {
        CONT_CLOSEPLUS();
        cont_state.timer = CONT_STATEMACH_WAIT_AFTER_CLOSING_PLUS_MS;
        cont_state.substate = CONT_PRECHARGE_OPEN_PRECHARGE;
    } else if (cont_state.substate == CONT_PRECHARGE_OPEN_PRECHARGE) {
        CONT_OPENPRECHARGE();
        cont_state.timer = CONT_STATEMACH_WAIT_AFTER_OPENING_PRECHARGE_MS;
        cont_state.substate = CONT_STANDBY;
    }
}

/**
 * @brief   checks reentry of CONT state machine trigger function
 *
 * @details This function is not re-entrant and should only be called time- or event-triggered.
 *          It increments the triggerentry counter from the state variable ltc_state.
 *          It should never be called by two different processes, so if it is the case,
 *          triggerentry should never be higher than 0 when this function is called.
 *
 * @return  true if this is the reentry of the function, false else.
 */
// tested
static bool cont_IsReentry(void) { //CONT_CheckReEntrance
    bool reEntry = false;
    taskENTER_CRITICAL();
    if (cont_state.triggerentry) {
        reEntry = true;
    } else {
        cont_state.triggerentry++;
    }
    taskEXIT_CRITICAL();
    return reEntry;
}

/**
 * @brief   checks the feedback of all the contactors
 *
 * @details makes a DIAG entry for each contactor when the feedback does not
 *          match the set value. This is for diagnosis only.
 */
static void cont_CheckAllContactorsFeedback(void) {
    CONT_ELECTRICAL_STATE_TYPE_e feedback;
    uint16_t contactor_feedback_state = 0;

    for (uint8_t i = 0; i < BS_NR_OF_CONTACTORS - 2; i++) {
        feedback = CONT_GetOneContactorFeedback(i);
        contactor_feedback_state |= feedback << i;
        cont_feedback_tab.contactor_feedback &= (~0x3F);
        cont_feedback_tab.contactor_feedback |= contactor_feedback_state;

        if (feedback != CONT_GetContactorSetValue(i)) {
            switch (i) {
            case CONT_CN_MAIN_PLUS:
                DIAG_Handler(DIAG_CH_CONTACTOR_MAIN_PLUS_FEEDBACK, DIAG_EVENT_NOK, 0);
                break;
            case CONT_CN_MAIN_MINUS:
                DIAG_Handler(DIAG_CH_CONTACTOR_MAIN_MINUS_FEEDBACK, DIAG_EVENT_NOK, 0);
                break;
            case CONT_CN_PRECHARGE:
                DIAG_Handler(DIAG_CH_CONTACTOR_PRECHARGE_FEEDBACK, DIAG_EVENT_NOK, 0);
                break;
            case CONT_CN_ENGINE:
                DIAG_Handler(DIAG_CH_CONTACTOR_ENGINE_FEEDBACK, DIAG_EVENT_NOK, 0); // change
                break;
            default:
                break;
            }
        } else {
            switch (i) {
                case CONT_CN_MAIN_PLUS:
                    DIAG_Handler(DIAG_CH_CONTACTOR_MAIN_PLUS_FEEDBACK, DIAG_EVENT_OK, 0);
                    break;
                case CONT_CN_MAIN_MINUS:
                    DIAG_Handler(DIAG_CH_CONTACTOR_MAIN_MINUS_FEEDBACK, DIAG_EVENT_OK, 0);
                    break;
                case CONT_CN_PRECHARGE:
                    DIAG_Handler(DIAG_CH_CONTACTOR_PRECHARGE_FEEDBACK, DIAG_EVENT_OK, 0);
                    break;
                case CONT_CN_ENGINE:
                    DIAG_Handler(DIAG_CH_CONTACTOR_ENGINE_FEEDBACK, DIAG_EVENT_OK, 0);
                    break;
                default:
                    break;
            }
        }
    }

    DB_WriteBlock(&cont_feedback_tab, DATA_BLOCK_ID_CONTFEEDBACK);
}

/**
 * @brief   checks the state requests that are made.
 *
 * @details This function checks the validity of the state requests. The results of the checked is
 *          returned immediately. We have the following possible requests:
 *              * CONT_STATE_INIT_REQUEST
 *              * CONT_STATE_STANDBY_REQUEST
 *              * CONT_STATE_NORMAL_REQUEST
 *              * CONT_STATE_CHARGE_REQUEST
 *              * CONT_STATE_ENGINE_REQUEST
 *              * CONT_STATE_ERROR_REQUEST
 *              * CONT_STATE_NO_REQUEST
 *
 *          Part of the state transition is implemented here.
 * @param   statereq    state request to be checked
 *
 * @return  result of the state request that was made, taken from (type: CONT_RETURN_TYPE_e)
 */
static CONT_RETURN_TYPE_e cont_CheckStateRequest(CONT_STATE_REQUEST_e statereq) {
    if (statereq == CONT_STATE_ERROR_REQUEST) {
        return CONT_OK;
    } else if (statereq == CONT_STATE_NO_REQUEST) {
        return CONT_ILLEGAL_REQUEST;
    }

    if (cont_state.statereq == CONT_STATE_NO_REQUEST) {
        /* init only allowed from the uninitialized state */
        if (statereq == CONT_STATE_INIT_REQUEST) {
            if (cont_state.state == CONT_STATEMACH_UNINITIALIZED) {
                return CONT_OK;
            } else {
                return CONT_ALREADY_INITIALIZED;
            }
        } else if ((statereq == CONT_STATE_STANDBY_REQUEST)) {
            if (cont_state.state == CONT_STATEMACH_STANDBY){
                return CONT_ILLEGAL_REQUEST;
            } else {
                return CONT_OK;
            }
        } else if (statereq == CONT_STATE_NORMAL_REQUEST) {
            if (cont_state.state == CONT_STATEMACH_STANDBY) {
                // more code will be added to refine the state machine.
                return CONT_OK;
            } else {
                return CONT_REQUEST_IMPOSSIBLE;
            }
        } else if (statereq == CONT_STATE_CHARGE_REQUEST) {
            if (cont_state.state == CONT_STATEMACH_STANDBY) {
                // more code will be added to refine the state machine.
                return CONT_OK;
            } else {
                return CONT_REQUEST_IMPOSSIBLE;
            }
        } else if (statereq == CONT_STATE_ENGINE_REQUEST) {
            if (cont_state.state == CONT_STATEMACH_STANDBY) {
                // more code will be added to refine the state machine.
                return CONT_OK;
            } else {
                return CONT_REQUEST_IMPOSSIBLE;
            }
        } else {
            return CONT_ILLEGAL_REQUEST;
        }
    } else {
        return CONT_REQUEST_PENDING;
    }
}

/**
 * @brief   gets the current state request.
 *
 * @details This function is used in the functioning of the CONT state machine.
 *
 * @return  return the current pending state request
 */
static CONT_STATE_REQUEST_e cont_GetStateRequest(void) {
    CONT_STATE_REQUEST_e retval = CONT_STATE_NO_REQUEST;

    taskENTER_CRITICAL();
    retval = cont_state.statereq;
    taskEXIT_CRITICAL();

    return retval;
}

/**
 * @brief   transfers the current state request to the state machine.
 *
 * @details This function takes the current state request from cont_state and transfers it to the
 *          state machine. It resets the value from cont_state to CONT_STATE_NO_REQUEST
 *
 * @return  current state request, taken from CONT_STATE_REQUEST_e
 *
 */
static CONT_STATE_REQUEST_e cont_TransferStateRequest(void) {
    CONT_STATE_REQUEST_e retval = CONT_STATE_NO_REQUEST;

    taskENTER_CRITICAL();
    retval = cont_state.statereq;
    cont_state.statereq = CONT_STATE_NO_REQUEST;
    taskEXIT_CRITICAL();

    return (retval);
}


// The following two static functions are only used for initial testing purpose.
// These functions will lock up the system for repeated flashing of LEDs.
/**
 * @brief   tests latching type contactors.
 *
 * @details This function tests the two latching type contactors in series.
 *          Note that the vTaskDelay calls used here may interfere with
 *          the timing counts for the timers.
 *
 *          Due to the usage of delay, this function does not work well with
 *          state machine. It works standalone.
 */
// tested
static void cont_TestLatchingContactorChannels(void) {
    static bool contactorsOn = false;
    contactorsOn = !contactorsOn;
    if (contactorsOn) {
        CONT_OPENPLUS();
        vTaskDelay(200);
        CONT_OPENMINUS()
        cont_state.timer = 500 * CONT_STATEMACH_SHORTTIME_MS;
    } else {
        CONT_CLOSEPLUS();
        vTaskDelay(200);
        CONT_CLOSEMINUS();
        cont_state.timer = 200 * CONT_STATEMACH_SHORTTIME_MS;
    }
    cont_state.activePowerLine = CONT_POWER_LINE_NONE;
}

/**
 * @brief   tests all contactors.
 *
 * @details This function tests all contactors as if they are all nonlatching type
 */
 // tested
static void cont_TestAllContactorChannels(void) {
    static bool contactorsOn = false;
    static int i = 0;

    CONT_SetContactorState(i++, contactorsOn);
    cont_state.activePowerLine = CONT_POWER_LINE_NONE;
    cont_state.timer = 50 * CONT_STATEMACH_SHORTTIME_MS;

    if (i >= 6) {
        i = 0;
        contactorsOn = !contactorsOn;
        cont_state.timer = 400 * CONT_STATEMACH_SHORTTIME_MS;
    }
}


/*================= Extern Function Implementations ==========================*/
// Not used
STD_RETURN_TYPE_e CONT_CheckPrecharge(CONT_WHICH_POWERLINE_e caller) {
    DATA_BLOCK_CURRENT_SENSOR_s current_tab = {0};
    STD_RETURN_TYPE_e retVal = E_NOT_OK;

    DB_ReadBlock(&current_tab, DATA_BLOCK_ID_CURRENT_SENSOR);
    float cont_prechargeVoltDiff_mV = 0.0;
    int32_t current_mA = 0;

    /* Only current not current direction is checked */
    if (current_tab.current > 0) {
        current_mA = current_tab.current;
    } else {
        current_mA = -current_tab.current;
    }

    if (caller == CONT_POWERLINE_NORMAL) {
        cont_prechargeVoltDiff_mV = 0.0;
        /* Voltage difference between V2 and V3 of Isabellenhuette current sensor */
        if (current_tab.voltage[1] > current_tab.voltage[2]) {
            cont_prechargeVoltDiff_mV = current_tab.voltage[1] - current_tab.voltage[2];
        } else {
            cont_prechargeVoltDiff_mV = current_tab.voltage[2] - current_tab.voltage[1];
        }

        if ((cont_prechargeVoltDiff_mV < CONT_PRECHARGE_VOLTAGE_THRESHOLD_mV) && (current_mA < CONT_PRECHARGE_CURRENT_THRESHOLD_mA)) {
            retVal = E_OK;
        } else {
            retVal = E_NOT_OK;
        }
    } else if (caller == CONT_POWERLINE_CHARGE) {
        cont_prechargeVoltDiff_mV = 0.0;
        /* Voltage difference between V1 and V3 of Isabellenhuette current sensor */
        if (current_tab.voltage[0] > current_tab.voltage[2]) {
            cont_prechargeVoltDiff_mV = current_tab.voltage[0] - current_tab.voltage[2];
        } else {
            cont_prechargeVoltDiff_mV = current_tab.voltage[2] - current_tab.voltage[0];
        }

        if ((cont_prechargeVoltDiff_mV < CONT_CHARGE_PRECHARGE_VOLTAGE_THRESHOLD_mV) && (current_mA < CONT_CHARGE_PRECHARGE_CURRENT_THRESHOLD_mA)) {
            retVal = E_OK;
        } else {
            retVal = E_NOT_OK;
        }
    }
    return retVal;
}

// Not used
STD_RETURN_TYPE_e CONT_CheckFuse(CONT_WHICH_POWERLINE_e caller) {
#if (BS_CHECK_FUSE_PLACED_IN_NORMAL_PATH == TRUE) || (BS_CHECK_FUSE_PLACED_IN_CHARGE_PATH == TRUE)
    STD_RETURN_TYPE_e fuseState = E_NOT_OK;
    DATA_BLOCK_CURRENT_SENSOR_s curSensTab;
    DATA_BLOCK_CONTFEEDBACK_s contFeedbackTab;
    uint32_t voltDiff_mV = 0;
    STD_RETURN_TYPE_e checkFuseState = E_NOT_OK;

    DB_ReadBlock(&curSensTab, DATA_BLOCK_ID_CURRENT_SENSOR);
    DB_ReadBlock(&contFeedbackTab, DATA_BLOCK_ID_CONTFEEDBACK);

    if (caller == CONT_POWERLINE_NORMAL) {
        /* Fuse state can only be checked if plus and minus contactors are closed. */
        if ((((contFeedbackTab.contactor_feedback & 0x01) == 0x01) ||
                ((contFeedbackTab.contactor_feedback & 0x02) == 0x02)) &&
                ((contFeedbackTab.contactor_feedback & 0x04) == 0x04)) {
                    /* main plus OR main precharge AND minus are closed */
                checkFuseState = E_OK;
        }  else {
            /* Fuse state can't be checked if no plus contactors are closed */
            checkFuseState = E_NOT_OK;
        }
        /* Check voltage difference between battery voltage and voltage after fuse */
        if (checkFuseState == E_OK) {
            if (curSensTab.voltage[0] > curSensTab.voltage[1]) {
                voltDiff_mV = curSensTab.voltage[0] - curSensTab.voltage[1];
            } else {
                voltDiff_mV = curSensTab.voltage[1] - curSensTab.voltage[0];
            }

            /* If voltage difference is larger than max. allowed voltage drop over fuse*/
            if (voltDiff_mV > BS_MAX_VOLTAGE_DROP_OVER_FUSE_mV) {
                fuseState = E_NOT_OK;
            } else {
                fuseState = E_OK;
            }
        } else {
            /* Can't draw any conclusions about fuse state -> do not return E_NOT_OK */
            fuseState = E_OK;
        }
    } else if (caller == CONT_POWERLINE_CHARGE) {
        /* Fuse state can only be checked if plus and minus contactors are closed. */
        if ((((contFeedbackTab.contactor_feedback & 0x08) == 0x08) ||
                ((contFeedbackTab.contactor_feedback & 0x10) == 0x10)) &&
                ((contFeedbackTab.contactor_feedback & 0x20) == 0x20)) {
            /* charge plus OR charge precharge AND minus are closed */
                checkFuseState = E_OK;
        } else {
            /* Fuse state can't be checked if no plus contactors are closed */
            checkFuseState = E_NOT_OK;
        }
        /* Check voltage difference between battery voltage and voltage after fuse */
        if (checkFuseState == E_OK) {
            if (curSensTab.voltage[0] > curSensTab.voltage[1]) {
                voltDiff_mV = curSensTab.voltage[0] - curSensTab.voltage[2];
            } else {
                voltDiff_mV = curSensTab.voltage[2] - curSensTab.voltage[0];
            }

            /* If voltage difference is larger than max. allowed voltage drop over fuse*/
            if (voltDiff_mV > BS_MAX_VOLTAGE_DROP_OVER_FUSE_mV) {
                fuseState = E_NOT_OK;
            } else {
                fuseState = E_OK;
            }
        } else {
            /* Can't draw any conclusions about fuse state -> do not return E_NOT_OK */
            fuseState = E_OK;
        }
    }
#if BS_CHECK_FUSE_PLACED_IN_NORMAL_PATH == TRUE
    if (fuseState == E_OK) {
        /* Fuse state ok -> check precharging */
        DIAG_Handler(DIAG_CH_FUSE_STATE_NORMAL, DIAG_EVENT_OK, 0);
    } else {
        /* Fuse tripped -> switch to error state */
        DIAG_Handler(DIAG_CH_FUSE_STATE_NORMAL, DIAG_EVENT_NOK, 0);
    }
#endif  /* BS_CHECK_FUSE_PLACED_IN_NORMAL_PATH == TRUE */
#if BS_CHECK_FUSE_PLACED_IN_CHARGE_PATH == TRUE
    if (fuseState == E_OK) {
        /* Fuse state ok -> check precharging */
        DIAG_Handler(DIAG_CH_FUSE_STATE_CHARGE, DIAG_EVENT_OK, 0);
    } else {
        /* Fuse tripped -> switch to error state */
        DIAG_Handler(DIAG_CH_FUSE_STATE_CHARGE, DIAG_EVENT_NOK, 0);
    }
#endif  /* BS_CHECK_FUSE_PLACED_IN_CHARGE_PATH == TRUE */
    return fuseState;
#else /* BS_CHECK_FUSE_PLACED_IN_NORMAL_PATH == FALSE && BS_CHECK_FUSE_PLACED_IN_CHARGE_PATH == FALSE */
    return E_OK;
#endif /* BS_CHECK_FUSE_PLACED_IN_NORMAL_PATH || BS_CHECK_FUSE_PLACED_IN_CHARGE_PATH */
}

// tested
STD_RETURN_TYPE_e CONT_AcquireContactorFeedbacks(void) {
    STD_RETURN_TYPE_e retVal = E_NOT_OK;
    taskENTER_CRITICAL();
    for (uint8_t i = 0; i < BS_NR_OF_CONTACTORS - 2; i++) {
        cont_contactor_states[i].feedback = CONT_GetOneContactorFeedback(i);
    }
    retVal = E_OK;
    taskEXIT_CRITICAL();
    return retVal;
}

STD_RETURN_TYPE_e CONT_GetAllContactorFeedbacks(void) {
    return CONT_AcquireContactorFeedbacks();
}

/**
 * @brief   gets the feedback of all the contactors
 *
 * @details makes a DIAG entry for each contactor when the feedback does not
 *          match the set value. This is for diagnosis only.
 */

// to be tested
CONT_ELECTRICAL_STATE_TYPE_e CONT_GetOneContactorFeedback(CONT_NAMES_e contactor) {
    CONT_ELECTRICAL_STATE_TYPE_e measuredContactorState = CONT_SWITCH_UNDEF;
    IO_PIN_STATE_e pinstate = IO_PIN_RESET;

    switch (cont_contactors_config[contactor].feedback_pin_type) {
    case CONT_HAS_NO_FEEDBACK:
        measuredContactorState = cont_contactor_states[contactor].set;
        break;
    case CONT_FEEDBACK_NORMALLY_OPEN:
        taskENTER_CRITICAL();
        pinstate = IO_ReadPin(cont_contactors_config[contactor].feedback_pin);
        taskEXIT_CRITICAL();
        if (IO_PIN_RESET == pinstate) {
            measuredContactorState = CONT_SWITCH_ON;
        } else if (IO_PIN_SET == pinstate) {
            measuredContactorState = CONT_SWITCH_OFF;
        } else {
            measuredContactorState = CONT_SWITCH_UNDEF;
        }
        break;
    case CONT_FEEDBACK_NORMALLY_CLOSED:
        taskENTER_CRITICAL();
        pinstate = IO_ReadPin(cont_contactors_config[contactor].feedback_pin);
        taskEXIT_CRITICAL();
        if (IO_PIN_SET == pinstate) {
            measuredContactorState = CONT_SWITCH_ON;
        } else if (IO_PIN_RESET == pinstate) {
            measuredContactorState = CONT_SWITCH_OFF;
        } else {
            measuredContactorState = CONT_SWITCH_UNDEF;
        }
        break;
    }
    cont_contactor_states[contactor].feedback = measuredContactorState;
    return measuredContactorState;
}

CONT_ELECTRICAL_STATE_TYPE_e CONT_GetContactorSetValue(CONT_NAMES_e contactor) {
    CONT_ELECTRICAL_STATE_TYPE_e contactorSetInformation = FALSE;
    taskENTER_CRITICAL();
    contactorSetInformation = cont_contactor_states[contactor].set;
    taskEXIT_CRITICAL();
    return contactorSetInformation;
}

STD_RETURN_TYPE_e CONT_SetContactorState(CONT_NAMES_e contactor,
        CONT_ELECTRICAL_STATE_TYPE_e requestedContactorState) {
    STD_RETURN_TYPE_e retVal = E_OK;

    if (requestedContactorState  ==  CONT_SWITCH_ON) {
        cont_contactor_states[contactor].set = CONT_SWITCH_ON;
        IO_WritePin(cont_contactors_config[contactor].control_pin, IO_PIN_SET);
        // printf("CONT: %d High\r\n", contactor);
        if (DIAG_HANDLER_RETURN_OK != DIAG_ContHandler(DIAG_EVENT_OK, (uint8_t) contactor, NULL)) {
            /* TODO: explain why empty if */
        }
    } else if (requestedContactorState  ==  CONT_SWITCH_OFF) {
        DB_ReadBlock(&cont_current_tab, DATA_BLOCK_ID_CURRENT_SENSOR);
        float currentAtSwitchOff = cont_current_tab.current;
        if (((BAD_SWITCHOFF_CURRENT_POS < currentAtSwitchOff) && (0 < currentAtSwitchOff)) ||
             ((BAD_SWITCHOFF_CURRENT_NEG > currentAtSwitchOff) && (0 > currentAtSwitchOff))) {
            if (DIAG_HANDLER_RETURN_OK != DIAG_ContHandler(DIAG_EVENT_NOK, (uint8_t) contactor, &currentAtSwitchOff)) {
                /* currently no error handling, just logging */
            }
        } else {
            if (DIAG_HANDLER_RETURN_OK != DIAG_ContHandler(DIAG_EVENT_OK, (uint8_t) contactor, NULL)) {
                /* TODO: explain why empty if */
            }
       }
        cont_contactor_states[contactor].set = CONT_SWITCH_OFF;
        IO_WritePin(cont_contactors_config[contactor].control_pin, IO_PIN_RESET);
        // printf("CONT: %d Low\r\n", contactor);
    } else {
        retVal = E_NOT_OK;
    }

    return retVal;
}

// The following functions works in the standalone mode. Yet, it does not work
// in the state machine; the delay may have messed up the transition of states.
STD_RETURN_TYPE_e CONT_SetContactorState_pulse(CONT_NAMES_e contactor) {
    STD_RETURN_TYPE_e retVal = E_OK;
    cont_contactor_states[contactor].set = CONT_SWITCH_ON;

    IO_WritePin(cont_contactors_config[contactor].control_pin, IO_PIN_SET);
    printf("CONT: %d High\r\n", contactor);
    vTaskDelay(CONT_LATCHING_PULSE_WIDTH_MS);
    IO_WritePin(cont_contactors_config[contactor].control_pin, IO_PIN_RESET);
    printf("CONT: %d Low\r\n", contactor);

    if (contactor == CONT_CN_MAIN_PLUS2) {
        cont_contactor_states[CONT_CN_MAIN_PLUS].set = CONT_SWITCH_OFF;
    } else if (contactor == CONT_CN_MAIN_MINUS2) {
        cont_contactor_states[CONT_CN_MAIN_MINUS].set = CONT_SWITCH_OFF;
    }

    return retVal;
}

// This function should not be called. Need to remove it from the extern list later.
STD_RETURN_TYPE_e CONT_SwitchAllContactorsOff(void) {
    STD_RETURN_TYPE_e retVal = E_NOT_OK;
    uint8_t offCounter = 0;
    STD_RETURN_TYPE_e successfullSet = E_NOT_OK;

    successfullSet = CONT_OPENENGINE();
    if (E_OK == successfullSet) {
        offCounter = offCounter + 1;
    }
    successfullSet = CONT_OPENPLUS();
    prnt("Normal precharge: Main Plus Opened\r\n");
    if (E_OK == successfullSet) {
        offCounter = offCounter + 1;
    }
    successfullSet = CONT_OPENPRECHARGE();
    if (E_OK == successfullSet) {
        offCounter = offCounter + 1;
    }
    successfullSet = CONT_OPENMINUS();
    if (E_OK == successfullSet) {
        offCounter = offCounter + 1;
    }
    prnt("Normal precharge: Main Minus Opened\r\n");
    if (BS_NR_OF_CONTACTORS - 2 == offCounter) {
        retVal = E_OK;
    } else {
        retVal = E_NOT_OK;
    }
    return retVal;
}

CONT_STATEMACH_e CONT_GetState(void) {
    return (cont_state.state);
}

STD_RETURN_TYPE_e CONT_GetInitializationState(void) {
    return (cont_state.initFinished);
}

CONT_POWER_LINE_e CONT_GetActivePowerLine() {
    return (cont_state.activePowerLine);
}

CONT_RETURN_TYPE_e CONT_SetStateRequest(CONT_STATE_REQUEST_e statereq) {
    CONT_RETURN_TYPE_e retVal = CONT_STATE_NO_REQUEST;

    taskENTER_CRITICAL();
    retVal = cont_CheckStateRequest(statereq);

    if (retVal == CONT_OK) {
        cont_state.statereq = statereq;
    }
    taskEXIT_CRITICAL();

    return retVal;
}


void CONT_Trigger(void) {
    STD_RETURN_TYPE_e retVal = E_OK;
    CONT_STATE_REQUEST_e statereq = CONT_STATE_NO_REQUEST;
    DATA_BLOCK_CONTACTORSTATE_s contactorstate;

    if (cont_IsReentry()) {
        return;
    }

    DIAG_SysMonNotify(DIAG_SYSMON_CONT_ID, 0);  /* task is running, state = ok */

    if (cont_state.state != CONT_STATEMACH_UNINITIALIZED) {
        cont_CheckAllContactorsFeedback();
    }

    if (cont_state.OscillationCounter > 0) {
        cont_state.OscillationCounter--;
    }

    if (cont_state.PrechargeTimeOut > 0) {
        if (cont_state.PrechargeTimeOut > CONT_TASK_CYCLE_CONTEXT_MS) {
            cont_state.PrechargeTimeOut -= CONT_TASK_CYCLE_CONTEXT_MS;
        } else {
            cont_state.PrechargeTimeOut = 0;
        }
    }

    if (cont_state.timer) {
        if (cont_state.timer > CONT_TASK_CYCLE_CONTEXT_MS) {
            cont_state.timer -= CONT_TASK_CYCLE_CONTEXT_MS;
        } else {
            cont_state.timer = 0;
        }
        if (cont_state.timer) {
            cont_state.triggerentry--;
            return;    /* handle state machine only if timer has elapsed */
        }
    }

    switch (cont_state.state) {

    /****************************UNINITIALIZED***********************************/
    case CONT_STATEMACH_UNINITIALIZED:
        /* waiting for Initialization Request */
        prnt("S: Uninitialized\r\n");
        statereq = cont_TransferStateRequest();
        if (statereq == CONT_STATE_INIT_REQUEST) {
            CONT_SAVELASTSTATES();
            cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
            cont_state.state = CONT_STATEMACH_INITIALIZATION;
            cont_state.substate = CONT_ENTRY;
        } else if (statereq == CONT_STATE_NO_REQUEST) {
            /* no actual request pending */
        } else {
            cont_state.ErrRequestCounter++;   /* illegal request pending */
        }
        break;

    /****************************INITIALIZATION**********************************/
    case CONT_STATEMACH_INITIALIZATION:
        prnt("S: Initialization\r\n");
        CONT_SAVELASTSTATES();
        // CONT_OPENALLCONTACTORS();
        cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
        cont_state.state = CONT_STATEMACH_INITIALIZED;
        cont_state.substate = CONT_ENTRY;
        break;

    /****************************INITIALIZED*************************************/
    case CONT_STATEMACH_INITIALIZED:
        prnt("S: Initialized\r\n");
        CONT_SAVELASTSTATES();
        cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
        cont_state.state = CONT_STATEMACH_IDLE;
        cont_state.substate = CONT_ENTRY;
        break;

    /****************************IDLE*************************************/
    case CONT_STATEMACH_IDLE:
        prnt("S: Idle\r\n");
        CONT_SAVELASTSTATES();
        cont_state.initFinished = E_OK;
        cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
        cont_state.state = CONT_STATEMACH_STANDBY;
        cont_state.substate = CONT_ENTRY;
        cont_SaveContStateToDatabase(&contactorstate, CONT_STATEMACH_IDLE);
        break;

        /****************************STANDBY*************************************/
    case CONT_STATEMACH_STANDBY:
        if (cont_state.laststate != cont_state.state) {
            printf("Current state: Standby\r\n");
        }
        if (cont_state.lastsubstate != cont_state.substate) {
            printf("Current substate: %d\r\n", cont_state.substate);
        }
        CONT_SAVELASTSTATES();
        // cont_TestLatchingContactorChannels();
        // cont_TestAllContactorChannels();
        // break;
        cont_SaveContStateToDatabase(&contactorstate, CONT_STATEMACH_STANDBY);
        cont_SaveContSubStateToDatabase(&contactorstate, cont_state.substate);
        if (cont_state.substate == CONT_ENTRY) {
            cont_state.OscillationCounter = CONT_OSCILLATION_LIMIT;
            // CONT_OPENALLCONTACTORS();
            cont_state.activePowerLine = CONT_POWER_LINE_NONE;
            cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
            cont_state.substate = CONT_STANDBY;
            break;
        } else if (cont_state.substate == CONT_STANDBY) {
            statereq = cont_TransferStateRequest();
            switch (statereq) {
            case CONT_STATE_CHARGE_REQUEST:
                CONT_SAVELASTSTATES();
                cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
                cont_state.state = CONT_STATEMACH_CHARGE_PRECHARGE;
                cont_state.substate = CONT_ENTRY;
                break;
            case CONT_STATE_NORMAL_REQUEST:
                CONT_SAVELASTSTATES();
                cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
                cont_state.state = CONT_STATEMACH_NORMAL_PRECHARGE;
                cont_state.substate = CONT_ENTRY;
                break;
            case CONT_STATE_ENGINE_REQUEST:
                CONT_SAVELASTSTATES();
                cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
                cont_state.state = CONT_STATEMACH_ENGINE_PRECHARGE;
                cont_state.substate = CONT_ENTRY;
                break;
            case CONT_STATE_ERROR_REQUEST:
                CONT_SAVELASTSTATES();
                cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
                cont_state.state = CONT_STATEMACH_ERROR;
                cont_state.substate = CONT_ENTRY;
                break;
            case CONT_STATE_NO_REQUEST:
            case CONT_STATEMACH_STANDBY:
                break;
            default:
                cont_state.ErrRequestCounter++; /*Illegal request pending*/
            }
        }
        break;

    /**************************** NORMAL PRECHARGE*************************************/
    case CONT_STATEMACH_NORMAL_PRECHARGE:
        if (cont_state.laststate != cont_state.state) {
            printf("Current state: (normal) Precharge\r\n");
        }
        if (cont_state.lastsubstate != cont_state.substate) {
            printf("Current substate: %d\r\n", cont_state.substate);
        }

        CONT_SAVELASTSTATES();

        /* Check if the process should be interrupted by request */
        if (cont_NewStandbyRequestExists()) {
            break;
        }

        cont_SaveContSubStateToDatabase(&contactorstate, cont_state.substate);

        if (cont_state.substate == CONT_ENTRY) {
            cont_SaveContStateToDatabase(&contactorstate, CONT_STATEMACH_NORMAL_PRECHARGE);

            if (cont_state.OscillationCounter > 0) {
                // cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
                // break;
            } else {
                cont_state.PrechargeTryCounter = 0;
                cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
                cont_state.substate = CONT_CLOSE_MAIN_MINUS_STEP1;
                break;
            }
        } else if (cont_state.substate == CONT_CLOSE_MAIN_MINUS_STEP1) {
            prnt("Normal precharge: Closing Main Minus\r\n");
            // CONT_CLOSEMINUS();
            CONT_CLOSEMINUS_STEP1();
            cont_state.timer = CONT_LATCHING_PULSE_WIDTH_MS;
            cont_state.substate = CONT_CLOSE_MAIN_MINUS_STEP2;
        } else if (cont_state.substate == CONT_CLOSE_MAIN_MINUS_STEP2) {
            prnt("Normal precharge: Main Minus Closed\r\n");
            CONT_CLOSEMINUS_STEP2();
            cont_contactor_states[CONT_CN_MAIN_MINUS].set = CONT_SWITCH_ON;
            cont_state.PrechargeTryCounter++;
            cont_state.PrechargeTimeOut = CONT_PRECHARGE_TIMEOUT_MS;
            cont_state.timer = CONT_STATEMACH_WAIT_AFTER_CLOSING_MINUS_MS;
            cont_state.substate = CONT_PRECHARGE_CLOSE_PRECHARGE;
        } else if (cont_state.substate == CONT_PRECHARGE_CLOSE_PRECHARGE) {
            // cont_CloseMainPlusWithPrecharge();
            prnt("Normal precharge: Closing Precharge\r\n");
            CONT_CLOSEPRECHARGE();
            cont_state.timer = CONT_PRECHARGE_TIME_MS;
            cont_state.substate = CONT_CLOSE_MAIN_PLUS_STEP1;
        } else if (cont_state.substate == CONT_CLOSE_MAIN_PLUS_STEP1) {
            prnt("Normal precharge: Closing Main Plus\r\n");
            // CONT_CLOSEPLUS();
            CONT_CLOSEPLUS_STEP1();
            cont_state.timer = CONT_LATCHING_PULSE_WIDTH_MS;
            cont_state.substate = CONT_CLOSE_MAIN_PLUS_STEP2;
        } else if (cont_state.substate == CONT_CLOSE_MAIN_PLUS_STEP2) {
            prnt("Normal precharge: Main Plus Closed\r\n");
            CONT_CLOSEPLUS_STEP2();
            cont_contactor_states[CONT_CN_MAIN_PLUS].set = CONT_SWITCH_ON;
            cont_state.PrechargeTryCounter++;
            cont_state.PrechargeTimeOut = CONT_PRECHARGE_TIMEOUT_MS;
            cont_state.timer = CONT_STATEMACH_WAIT_AFTER_CLOSING_PLUS_MS;
            cont_state.substate = CONT_PRECHARGE_OPEN_PRECHARGE;
        } else if (cont_state.substate == CONT_PRECHARGE_OPEN_PRECHARGE) {
            prnt("Normal precharge: Opening Precharge\r\n");
            CONT_OPENPRECHARGE();
            cont_state.timer = CONT_STATEMACH_WAIT_AFTER_OPENING_PRECHARGE_MS;
            cont_state.substate = CONT_STANDBY;
            cont_state.state = CONT_STATEMACH_NORMAL;
            cont_state.activePowerLine = CONT_POWER_LINE_0;
        } else {
            // Record error
            cont_state.ErrRequestCounter++;
        }
        break;

    /****************************NORMAL*************************************/
    case CONT_STATEMACH_NORMAL:
        if (cont_state.laststate != cont_state.state) {
            printf("Current state: Normal\r\n");
        }
        if (cont_state.lastsubstate != cont_state.substate) {
            printf("Current substate: %d\r\n", cont_state.substate);
        }

        CONT_SAVELASTSTATES();
        // LMM: double-check with JL that this is the place to put the save --- before the two if statements that could break out 
        cont_SaveContStateToDatabase(&contactorstate, CONT_STATEMACH_NORMAL);
        cont_SaveContSubStateToDatabase(&contactorstate, cont_state.substate);

        if (cont_NewStandbyRequestExists()) {
            break;
        }
        if (cont_NewErrorRequestExists()) {
            break;
        }
        /* check fuse state */
        //CONT_CheckFuse(CONT_POWERLINE_NORMAL);
        
        
        break;

    /****************************CHARGE_PRECHARGE*************************************/
    case CONT_STATEMACH_CHARGE_PRECHARGE:
        if (cont_state.laststate != cont_state.state) {
            printf("Current state: Charge precharge\r\n");
        }
        if (cont_state.lastsubstate != cont_state.substate) {
            printf("Current substate: %d\r\n", cont_state.substate);
        }

        CONT_SAVELASTSTATES();

        /* Check if the process should be interrupted by request */
        if (cont_NewStandbyRequestExists()) {
            break;
        }

        cont_SaveContSubStateToDatabase(&contactorstate, cont_state.substate);

        if (cont_state.substate == CONT_ENTRY) {
            if (cont_state.OscillationCounter > 0) {
                // cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
                // break;
            } else {
                cont_state.PrechargeTryCounter = 0;
                cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
                cont_state.substate = CONT_PRECHARGE_CLOSE_PRECHARGE;
            }
            cont_SaveContStateToDatabase(&contactorstate, CONT_STATEMACH_CHARGE_PRECHARGE);
        } else if (cont_state.substate == CONT_PRECHARGE_CLOSE_PRECHARGE) {
            // cont_CloseMainPlusWithPrecharge();
            prnt("Normal precharge: Closing Precharge\r\n");
            CONT_CLOSEPRECHARGE();
            cont_state.timer = CONT_PRECHARGE_TIME_MS;
            cont_state.substate = CONT_CLOSE_MAIN_PLUS_STEP1;
        } else if (cont_state.substate == CONT_CLOSE_MAIN_PLUS_STEP1) {
            prnt("Normal precharge: Closing Main Plus\r\n");
            // CONT_CLOSEPLUS();
            CONT_CLOSEPLUS_STEP1();
            cont_state.timer = CONT_LATCHING_PULSE_WIDTH_MS;
            cont_state.substate = CONT_CLOSE_MAIN_PLUS_STEP2;
        } else if (cont_state.substate == CONT_CLOSE_MAIN_PLUS_STEP2) {
            prnt("Normal precharge: Main Plus Closed\r\n");
            CONT_CLOSEPLUS_STEP2();
            cont_contactor_states[CONT_CN_MAIN_PLUS].set = CONT_SWITCH_ON;
            cont_state.PrechargeTryCounter++;
            cont_state.PrechargeTimeOut = CONT_PRECHARGE_TIMEOUT_MS;
            cont_state.timer = CONT_STATEMACH_WAIT_AFTER_CLOSING_PLUS_MS;
            cont_state.substate = CONT_PRECHARGE_OPEN_PRECHARGE;
        } else if (cont_state.substate == CONT_PRECHARGE_OPEN_PRECHARGE) {
            prnt("Normal precharge: Opening Precharge\r\n");
            CONT_OPENPRECHARGE();
            cont_state.timer = CONT_STATEMACH_WAIT_AFTER_OPENING_PRECHARGE_MS;
            cont_state.substate = CONT_STANDBY;
            cont_state.state = CONT_STATEMACH_CHARGE;
            cont_state.activePowerLine = CONT_POWER_LINE_0;
        } else {
            // Record error
            cont_state.ErrRequestCounter++;
        }
        break;


    /****************************CHARGE************************************/
    case CONT_STATEMACH_CHARGE:
        if (cont_state.laststate != cont_state.state) {
            printf("Current state: Charge\r\n");
        }
        if (cont_state.lastsubstate != cont_state.substate) {
            printf("Current substate: %d\r\n", cont_state.substate);
        }

        CONT_SAVELASTSTATES();
        // LMM: double-check with JL that this is the place to put the save --- before the two if statements that could break out 
        cont_SaveContStateToDatabase(&contactorstate, CONT_STATEMACH_CHARGE);
        cont_SaveContSubStateToDatabase(&contactorstate, cont_state.substate);

        if (cont_NewStandbyRequestExists()) {
            break;
        }
        if (cont_NewErrorRequestExists()) {
            break;
        }
           
        break;

    /****************************ENGINE_PRECHARGE**************************/
    case CONT_STATEMACH_ENGINE_PRECHARGE:
        if (cont_state.laststate != cont_state.state) {
            printf("Current state: Engine precharge\r\n");
        }
        if (cont_state.lastsubstate != cont_state.substate) {
            printf("Current substate: %d\r\n", cont_state.substate);
        }

        CONT_SAVELASTSTATES();
        if (cont_NewStandbyRequestExists()) {
            break;
        }

        cont_SaveContSubStateToDatabase(&contactorstate, cont_state.substate);

        /* precharge process, can be interrupted anytime by the requests above */
        if (cont_state.substate == CONT_ENTRY) {
            cont_SaveContStateToDatabase(&contactorstate, CONT_STATEMACH_ENGINE_PRECHARGE);

            if (cont_state.OscillationCounter > 0) {
                cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
                break;
            } else {
                cont_state.PrechargeTryCounter = 0;
                cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
                cont_state.substate = CONT_PRECHARGE_CLOSE_ENGINE;
                break;
            }
        } else if (cont_state.substate == CONT_PRECHARGE_CLOSE_ENGINE) {
            cont_state.PrechargeTryCounter++;
            cont_state.PrechargeTimeOut = CONT_PRECHARGE_TIMEOUT_MS;
            CONT_CLOSEENGINE();
            cont_state.timer = CONT_STATEMACH_WAIT_AFTER_CLOSING_MINUS_MS;
            cont_state.substate = CONT_PRECHARGE_CLOSE_PRECHARGE;
        } else if (cont_state.substate == CONT_PRECHARGE_CLOSE_PRECHARGE) {
            // cont_CloseMainPlusWithPrecharge();
            prnt("Engine precharge: Closing Precharge\r\n");
            CONT_CLOSEPRECHARGE();
            cont_state.timer = CONT_PRECHARGE_TIME_MS;
            cont_state.substate = CONT_CLOSE_MAIN_PLUS_STEP1;
        } else if (cont_state.substate == CONT_CLOSE_MAIN_PLUS_STEP1) {
            prnt("Engine precharge: Closing Main Plus\r\n");
            // CONT_CLOSEPLUS();
            CONT_CLOSEPLUS_STEP1();
            cont_state.timer = CONT_LATCHING_PULSE_WIDTH_MS;
            cont_state.substate = CONT_CLOSE_MAIN_PLUS_STEP2;
        } else if (cont_state.substate == CONT_CLOSE_MAIN_PLUS_STEP2) {
            prnt("Engine precharge: Main Plus Closed\r\n");
            CONT_CLOSEPLUS_STEP2();
            cont_contactor_states[CONT_CN_MAIN_PLUS].set = CONT_SWITCH_ON;
            cont_state.PrechargeTryCounter++;
            cont_state.PrechargeTimeOut = CONT_PRECHARGE_TIMEOUT_MS;
            cont_state.timer = CONT_STATEMACH_WAIT_AFTER_CLOSING_PLUS_MS;
            cont_state.substate = CONT_PRECHARGE_OPEN_PRECHARGE;
        } else if (cont_state.substate == CONT_PRECHARGE_OPEN_PRECHARGE) {
            prnt("Engine precharge: Opening Precharge\r\n");
            CONT_OPENPRECHARGE();
            cont_state.timer = CONT_STATEMACH_WAIT_AFTER_OPENING_PRECHARGE_MS;
            cont_state.substate = CONT_STANDBY;
            cont_state.state = CONT_STATEMACH_ENGINE;
            cont_state.activePowerLine = CONT_POWER_LINE_0;
        } else {
            // Record error
            cont_state.ErrRequestCounter++;
        }
        break;

    /***************************ENGINE*************************************/
    case CONT_STATEMACH_ENGINE:
        if (cont_state.laststate != cont_state.state) {
            printf("Current state: Engine\r\n");
        }
        if (cont_state.lastsubstate != cont_state.substate) {
            printf("Current substate: %d\r\n", cont_state.substate);
        }

        CONT_SAVELASTSTATES();

        // LMM: double-check with JL that this is the place to put the save --- before the two if statements that could break out 
        cont_SaveContStateToDatabase(&contactorstate, CONT_STATEMACH_ENGINE);
        cont_SaveContSubStateToDatabase(&contactorstate, cont_state.substate);

        if (cont_NewStandbyRequestExists()) {
            break;
        }
        if (cont_NewErrorRequestExists()) {
            break;
        }
        /* check fuse state */
        // CONT_CheckFuse(CONT_POWERLINE_NORMAL); // ?
        
        
        break;

    /***************************OPEN CONTACTORS*************************************/
    case CONT_STATEMACH_OPEN_CONT:

        cont_SaveContSubStateToDatabase(&contactorstate, cont_state.substate);

        if (cont_state.substate == CONT_ENTRY) {
            if (cont_state.OscillationCounter > 0) {
                cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
            } else {
                printf("Current state: Open Contactors\r\n");
                printf("Current substate: %d\r\n", cont_state.substate);

                CONT_OPENENGINE();
                CONT_OPENPRECHARGE();

                cont_state.PrechargeTryCounter = 0;
                cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
                cont_state.substate = CONT_OPEN_MAIN_PLUS_STEP1;
            }
            cont_SaveContStateToDatabase(&contactorstate, CONT_STATEMACH_OPEN_CONT);
        } else if (cont_state.substate == CONT_OPEN_MAIN_PLUS_STEP1) {
            CONT_OPENPLUS_STEP1();
            cont_state.timer = CONT_LATCHING_PULSE_WIDTH_MS;
            cont_state.substate = CONT_OPEN_MAIN_PLUS_STEP2;
        } else if (cont_state.substate == CONT_OPEN_MAIN_PLUS_STEP2) {
            CONT_OPENPLUS_STEP2();
            cont_contactor_states[CONT_CN_MAIN_PLUS].set = CONT_SWITCH_OFF;
            cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
            cont_state.substate = CONT_OPEN_MAIN_MINUS_STEP1;
        } else if (cont_state.substate == CONT_OPEN_MAIN_MINUS_STEP1) {
            CONT_OPENMINUS_STEP1();
            cont_state.timer = CONT_LATCHING_PULSE_WIDTH_MS;
            cont_state.substate = CONT_OPEN_MAIN_MINUS_STEP2;
        } else if (cont_state.substate == CONT_OPEN_MAIN_MINUS_STEP2) {
            CONT_OPENMINUS_STEP2();
            cont_contactor_states[CONT_CN_MAIN_MINUS].set = CONT_SWITCH_OFF;
            cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
            cont_state.state = CONT_STATEMACH_STANDBY;  // Go to Standby state
            cont_state.substate = CONT_ENTRY;
            cont_state.activePowerLine = CONT_POWER_LINE_0;
        } else {
            // Record error
            cont_state.ErrRequestCounter++;
        }
        break;

        /****************************ERROR*************************************/
    case CONT_STATEMACH_ERROR:
        if (cont_state.laststate != cont_state.state) {
            printf("Current state: Error\r\n");
        }
        if (cont_state.lastsubstate != cont_state.substate) {
            printf("Current substate: %d\r\n", cont_state.substate);
        }

        cont_SaveContStateToDatabase(&contactorstate, CONT_STATEMACH_ERROR);
        cont_SaveContSubStateToDatabase(&contactorstate, cont_state.substate);

        CONT_SAVELASTSTATES();
        if (cont_state.substate == CONT_ERROR) {
            /* Check if fuse is tripped */
            // CONT_CheckFuse(CONT_POWERLINE_NORMAL);
            /* when process done, look for requests */
            statereq = cont_TransferStateRequest();
            if (statereq == CONT_STATE_ERROR_REQUEST) {
                /* we stay already in requested state, nothing to do */
            } else if (statereq == CONT_STATE_STANDBY_REQUEST) {
                CONT_SAVELASTSTATES();
                cont_state.timer = CONT_STATEMACH_SHORTTIME_MS;
                cont_state.state = CONT_STATEMACH_STANDBY;
                cont_state.substate = CONT_ENTRY;
            } else if (statereq == CONT_STATE_NO_REQUEST) {
                /* no actual request pending */
            } else {
                cont_state.ErrRequestCounter++;   /* illegal request pending */
            }
            break;
        }
        break;

    default:
        break;
    }  /* end switch (cont_state.state) */

    cont_state.triggerentry--;
    cont_state.counter++;
}

static void cont_SaveContStateToDatabase(DATA_BLOCK_CONTACTORSTATE_s *data_block, CONT_STATEMACH_e contactor_state) {
    DB_ReadBlock(data_block, DATA_BLOCK_ID_CONTACTORSTATE);
    data_block->contactor_state = contactor_state;
    DB_WriteBlock(data_block, DATA_BLOCK_ID_CONTACTORSTATE);
}
static void cont_SaveContSubStateToDatabase(DATA_BLOCK_CONTACTORSTATE_s *data_block, CONT_STATEMACH_SUB_e contactor_substate){
    DB_ReadBlock(data_block, DATA_BLOCK_ID_CONTACTORSTATE);
    data_block->contactor_substate = contactor_substate;
    DB_WriteBlock(data_block, DATA_BLOCK_ID_CONTACTORSTATE);
}
//#endif /* BUILD_MODULE_ENABLE_CONTACTOR */
