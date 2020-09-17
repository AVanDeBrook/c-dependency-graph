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
 * @file    contactor.h
 * @author  foxBMS Team
 * @date    23.09.2015 (date of creation)
 * @ingroup DRIVERS
 * @prefix  CONT
 *
 * @brief   Headers for the driver for the contactors.
 *
 */

#ifndef CONTACTOR_H_
#define CONTACTOR_H_

/*================== Includes =============================================*/
#include "contactor_cfg.h"


/*================== Macros and Definitions ===============================*/
#define CONT_OPENALLCONTACTORS()   CONT_SwitchAllContactorsOff();

#define CONT_CLOSEPLUS()        CONT_SetContactorState_pulse(CONT_CN_MAIN_PLUS);
#define CONT_OPENPLUS()         CONT_SetContactorState_pulse(CONT_CN_MAIN_PLUS2);
#define CONT_CLOSEPLUS_STEP1() CONT_SetContactorState(CONT_CN_MAIN_PLUS, CONT_SWITCH_ON);
#define CONT_CLOSEPLUS_STEP2() CONT_SetContactorState(CONT_CN_MAIN_PLUS, CONT_SWITCH_OFF);
#define CONT_OPENPLUS_STEP1()  CONT_SetContactorState(CONT_CN_MAIN_PLUS2, CONT_SWITCH_ON);
#define CONT_OPENPLUS_STEP2()  CONT_SetContactorState(CONT_CN_MAIN_PLUS2, CONT_SWITCH_OFF);

#define CONT_CLOSEPRECHARGE()   CONT_SetContactorState(CONT_CN_PRECHARGE, CONT_SWITCH_ON);
#define CONT_OPENPRECHARGE()    CONT_SetContactorState(CONT_CN_PRECHARGE, CONT_SWITCH_OFF);

#define CONT_CLOSEMINUS()       CONT_SetContactorState_pulse(CONT_CN_MAIN_MINUS);
#define CONT_OPENMINUS()        CONT_SetContactorState_pulse(CONT_CN_MAIN_MINUS2);

#define CONT_CLOSEMINUS_STEP1() CONT_SetContactorState(CONT_CN_MAIN_MINUS, CONT_SWITCH_ON);
#define CONT_CLOSEMINUS_STEP2() CONT_SetContactorState(CONT_CN_MAIN_MINUS, CONT_SWITCH_OFF);
#define CONT_OPENMINUS_STEP1()  CONT_SetContactorState(CONT_CN_MAIN_MINUS2, CONT_SWITCH_ON);
#define CONT_OPENMINUS_STEP2()  CONT_SetContactorState(CONT_CN_MAIN_MINUS2, CONT_SWITCH_OFF);

#define CONT_CLOSEENGINE()      CONT_SetContactorState(CONT_CN_ENGINE, CONT_SWITCH_ON);
#define CONT_OPENENGINE()       CONT_SetContactorState(CONT_CN_ENGINE, CONT_SWITCH_OFF);

//----------------------------------
#define CONT_OPENCHARGEMINUS()  CONT_SetContactorState(CONT_CHARGE_MAIN_MINUS, CONT_SWITCH_OFF);
#define CONT_CLOSECHARGEMINUS() CONT_SetContactorState(CONT_CHARGE_MAIN_MINUS, CONT_SWITCH_ON);

#define CONT_OPENCHARGEPLUS()   CONT_SetContactorState(CONT_CHARGE_MAIN_PLUS, CONT_SWITCH_OFF);
#define CONT_CLOSECHARGEPLUS()  CONT_SetContactorState(CONT_CHARGE_MAIN_PLUS, CONT_SWITCH_ON);

#define CONT_OPENCHARGEPRECHARGE()  CONT_SetContactorState(CONT_CHARGE_PRECHARGE_PLUS, CONT_SWITCH_OFF);
#define CONT_CLOSECHARGEPRECHARGE() CONT_SetContactorState(CONT_CHARGE_PRECHARGE_PLUS, CONT_SWITCH_ON);


#define DIAG_CH_CONTACTOR_ENGINE_FEEDBACK  DIAG_CH_CONTACTOR_CHARGE_MAIN_PLUS_FEEDBACK


/*================== Constant and Variable Definitions ====================*/
/**
 * States of the CONT state machine
 */
typedef enum {
    /* Init-Sequence */
    CONT_STATEMACH_UNINITIALIZED             = 0,    /*!<    */
    CONT_STATEMACH_INITIALIZATION            = 1,    /*!<    */
    CONT_STATEMACH_INITIALIZED               = 2,    /*!<    */
    CONT_STATEMACH_IDLE                      = 3,    /*!<    */
    CONT_STATEMACH_STANDBY                   = 4,    /*!<    */
    CONT_STATEMACH_NORMAL_PRECHARGE          = 5,    /*!<    */
    CONT_STATEMACH_NORMAL                    = 6,    /*!<    */
    CONT_STATEMACH_CHARGE_PRECHARGE          = 7,    /*!<    */
    CONT_STATEMACH_CHARGE                    = 8,    /*!<    */
    CONT_STATEMACH_ENGINE_PRECHARGE          = 9,    /*!<    */
    CONT_STATEMACH_ENGINE                    = 10,    /*!<    */
    CONT_STATEMACH_OPEN_CONT                 = 11,    /*!< Open all contactors */
    CONT_STATEMACH_UNDEFINED                 = 20,   /*!< undefined state                                */
    CONT_STATEMACH_RESERVED1                 = 0x80, /*!< reserved state                                 */
    CONT_STATEMACH_ERROR                     = 0xF0, /*!< Error-State:  */
} CONT_STATEMACH_e;

/**
 * Substates of the CONT state machine
 */
typedef enum {
    CONT_ENTRY                                    = 0,    /*!< Substate entry state       */
    CONT_OPEN_FIRST_CONTACTOR                     = 1,    /*!< Open-sequence: first contactor */
    CONT_OPEN_SECOND_CONTACTOR_MINUS              = 2,    /*!< Open-sequence: second contactor */
    CONT_OPEN_SECOND_CONTACTOR_PLUS               = 3,    /*!< Open-sequence: second contactor */
    CONT_STANDBY                                  = 4,    /*!< Substate standby */
    CONT_PRECHARGE_CLOSE_MINUS                    = 5,    /*!< Begin of precharge sequence: close main minus */
    CONT_PRECHARGE_CLOSE_PRECHARGE                = 6,    /*!< Next step of precharge sequence: close precharge */
    CONT_PRECHARGE_CLOSE_PLUS                     = 7,    /*!< Next step of precharge sequence: close main plus */
    CONT_PRECHARGE_CHECK_VOLTAGES                 = 8,    /*!< Next step of precharge sequence: check if voltages OK */
    CONT_PRECHARGE_OPEN_PRECHARGE                 = 9,    /*!< Next step of precharge sequence: open precharge */
    CONT_ERROR                                    = 10,   /*!< Error state */
    CONT_PRECHARGE_CLOSE_MAIN_MINUS = 12,    /*!< Begin of precharge sequence: close main minus */
    CONT_PRECHARGE_CLOSE_MAIN_PLUS  = 14,    /*!< Next step of precharge sequence: close main plus */
    CONT_PRECHARGE_CLOSE_ENGINE     = 15,    /*!< Next step of precharge sequence: check if voltages OK */
    CONT_CLOSE_MAIN_PLUS_STEP1      = 16,
    CONT_CLOSE_MAIN_PLUS_STEP2      = 17,
    CONT_CLOSE_MAIN_MINUS_STEP1     = 18,
    CONT_CLOSE_MAIN_MINUS_STEP2     = 19,
    CONT_OPEN_MAIN_PLUS_STEP1      = 20,
    CONT_OPEN_MAIN_PLUS_STEP2      = 21,
    CONT_OPEN_MAIN_MINUS_STEP1     = 22,
    CONT_OPEN_MAIN_MINUS_STEP2     = 23,
} CONT_STATEMACH_SUB_e;

/**
 * State requests for the CONT statemachine
 */
typedef enum {
    CONT_STATE_INIT_REQUEST      = CONT_STATEMACH_INITIALIZATION,     /*!<    */
    CONT_STATE_STANDBY_REQUEST   = CONT_STATEMACH_STANDBY,            /*!<    */
    CONT_STATE_NORMAL_REQUEST    = CONT_STATEMACH_NORMAL,             /*!<    */
    CONT_STATE_CHARGE_REQUEST    = CONT_STATEMACH_CHARGE,             /*!<    */
    CONT_STATE_ENGINE_REQUEST    = CONT_STATEMACH_ENGINE,             /*!<    */
    CONT_STATE_ERROR_REQUEST     = CONT_STATEMACH_ERROR,              /*!<    */
    CONT_STATE_NO_REQUEST        = CONT_STATEMACH_RESERVED1,          /*!<    */
} CONT_STATE_REQUEST_e;

/**
 * Possible return values when state requests are made to the CONT statemachine
 */
typedef enum {
    CONT_OK                                 = 0,    /*!< CONT --> ok                            */
    CONT_BUSY_OK                            = 1,    /*!< CONT under load --> ok                 */
    CONT_REQUEST_PENDING                    = 2,    /*!< requested to be executed               */
    CONT_REQUEST_IMPOSSIBLE                 = 3,    /*!< requested not possible                 */
    CONT_ILLEGAL_REQUEST                    = 4,    /*!< Request can not be executed            */
    CONT_INIT_ERROR                         = 5,    /*!< Error state: Source: Initialization    */
    CONT_OK_FROM_ERROR                      = 6,    /*!< Return from error --> ok               */
    CONT_ALREADY_INITIALIZED                = 30,   /*!< Initialization of LTC already finished */
    CONT_ILLEGAL_TASK_TYPE                  = 99,   /*!< Illegal                                */
} CONT_RETURN_TYPE_e;

/**
 * @brief Names for connected powerlines.
 */
typedef enum CONT_POWER_LINE_e {
    CONT_POWER_LINE_NONE,    /*!< no power line is connected, contactors are open            */
    CONT_POWER_LINE_0,       /*!< power line 0, e.g. used for the power train                */
    CONT_POWER_LINE_1,       /*!< power line 1, e.g. used for charging                       */
} CONT_POWER_LINE_e;

/**
 * This structure contains all the variables relevant for the CONT state machine.
 * The user can get the current state of the CONT state machine with this variable
 */
typedef struct {
    uint16_t timer;                          /*!< time in ms before the state machine processes the next state, e.g. in counts of 1ms    */
    CONT_STATE_REQUEST_e statereq;           /*!< current state request made to the state machine                                        */
    CONT_STATEMACH_e state;                  /*!< state of Driver State Machine                                                          */
    CONT_STATEMACH_SUB_e substate;           /*!< current substate of the state machine                                                  */
    CONT_STATEMACH_e laststate;              /*!< previous state of the state machine                                                    */
    CONT_STATEMACH_SUB_e lastsubstate;       /*!< previous substate of the state machine                                                 */
    uint32_t ErrRequestCounter;              /*!< counts the number of illegal requests to the LTC state machine                         */
    STD_RETURN_TYPE_e initFinished;          /*!< #E_OK if the initialization has passed, #E_NOT_OK otherwise                            */
    uint16_t OscillationCounter;             /*!< timeout to prevent oscillation of contactors                                           */
    uint8_t PrechargeTryCounter;             /*!< timeout to prevent oscillation of contactors                                           */
    uint16_t PrechargeTimeOut;               /*!< time to wait when precharge has been closed for voltages to settle                     */
    uint8_t triggerentry;                    /*!< counter for re-entrance protection (function running flag)                             */
    uint8_t counter;                         /*!< general purpose counter                                                                */
    CONT_POWER_LINE_e activePowerLine;       /*!< tracks the currently connected power line                                              */
} CONT_STATE_s;


/*================== Function Prototypes ==================================*/
/**
 * @brief   Checks the configuration of the contactor-module
 *
 * @return  retVal (type: STD_RETURN_TYPE_e)
 */
extern STD_RETURN_TYPE_e CONT_Init(void);

/**
 * @brief   Checks if the current limitations are violated
 *
 * @return  E_OK if the current limitations are NOT violated, else E_NOT_OK (type: STD_RETURN_TYPE_e)
 */
extern STD_RETURN_TYPE_e CONT_CheckPrecharge(CONT_WHICH_POWERLINE_e caller);

/**
 * Function to check fuse state. Fuse state can only be checked if at least
 * plus contactors are closed. Furthermore fuse needs to be placed in plus
 * path and monitored by Isabellenhuette HV measurement
 *
 * @return  Returns E_OK if fuse is intact, and E_NOT_OK if fuse is tripped.
 *
 */
extern STD_RETURN_TYPE_e CONT_CheckFuse(CONT_WHICH_POWERLINE_e caller);

/**
 * @brief   Reads the feedback pins of all contactors and updates the contactors_cfg[] array with
 *          their current states.
 *
 * @return  Returns E_OK if all feedbacks could be acquired (type: STD_RETURN_TYPE_e)
 */
extern STD_RETURN_TYPE_e CONT_AcquireContactorFeedbacks(void);

/**
 * @brief   Alias of CONT_AcquireContactorFeedbacks().
 *
 * @return  Returns E_OK if all feedbacks could be acquired (type: STD_RETURN_TYPE_e)
 */
extern STD_RETURN_TYPE_e CONT_CONT_GetAllContactorFeedbacks(void);

/**
 * @brief   Reads the feedback pin of one contactor and returns its current value
 *          (CONT_SWITCH_OFF/CONT_SWITCH_ON).
 *
 * @details If the contactor has a feedback pin the measured feedback is returned. If the contactor
 *          has no feedback pin, it is assumed that after a certain time the contactor has reached
 *          the requested state.
 *
 * @param   contactor (type: CONT_NAMES_e)
 *
 * @return  measuredContactorState (type: CONT_ELECTRICAL_STATE_TYPE_e)
 */
extern CONT_ELECTRICAL_STATE_TYPE_e CONT_GetOneContactorFeedback(CONT_NAMES_e contactor);

/**
 * @brief   Gets the latest value (TRUE, FALSE) the contactors were set to.
 *
 * @param   contactor (type: CONT_NAMES_e)
 *
 * @return  returns CONT_SWITCH_OFF or CONT_SWITCH_ON
 */
extern CONT_ELECTRICAL_STATE_TYPE_e CONT_GetContactorSetValue(CONT_NAMES_e contactor);

/**
 * @brief   Sets the contactor state to its requested state, if the contactor is at that time not
 *          in the requested state.
 *
 * @details If the new state was already requested, but not reached (meaning the measured feedback
 *          does not return the requested state), there are two states: it can be still ok (E_OK),
 *          because the contactor has some time left to get physically in the requested state
 *          (passed time since the request is lower than the limit) or it can be not ok (E_NOT_OK),
 *          because there is timing violation, i.e. the contactor has surpassed the maximum time
 *          for getting in the requested state. It returns E_OK if the requested state was
 *          successfully set or if the contactor was at the requested state before.
 *
 * @param   contactor (type: CONT_NAMES_e)
 * @param   requestedContactorState (type: CONT_ELECTRICAL_STATE_TYPE_e)
 *
 * @return  retVal (type: STD_RETURN_TYPE_e)
 */
extern STD_RETURN_TYPE_e CONT_SetContactorState(CONT_NAMES_e contactor, CONT_ELECTRICAL_STATE_TYPE_e requestedContactorState);

/**
 * @brief   Sets the latching type contactor state to its requested state, if the contactor is at 
 *          that time not in the requested state.
 *
 * @details If the new state was already requested, but not reached (meaning the measured feedback
 *          does not return the requested state), there are two states: it can be still ok (E_OK),
 *          because the contactor has some time left to get physically in the requested state
 *          (passed time since the request is lower than the limit) or it can be not ok (E_NOT_OK),
 *          because there is timing violation, i.e. the contactor has surpassed the maximum time
 *          for getting in the requested state. It returns E_OK if the requested state was
 *          successfully set or if the contactor was at the requested state before.
 *
 * @param   contactor (type: CONT_NAMES_e)
 *
 * @return  retVal (type: STD_RETURN_TYPE_e)
 */
extern STD_RETURN_TYPE_e CONT_SetContactorState_pulse(CONT_NAMES_e contactor);

/**
 * @brief   Iterates over the contactor array and switches all contactors off
 *
 * @return  E_OK if all contactors were opened, E_NOT_OK if not all contactors could be opened
 *          (type: STD_RETURN_TYPE_e)
 */
extern STD_RETURN_TYPE_e CONT_SwitchAllContactorsOff(void);


/**
 * @brief   Gets the current state.
 *
 * @details This function is used in the functioning of the CONT state machine.
 *
 * @return  current state, taken from #CONT_STATEMACH_e
 */
extern  CONT_STATEMACH_e CONT_GetState(void);

/**
 * @brief   Gets the initialization state.
 *
 * This function is used for getting the CONT initialization state.
 *
 * @return  #E_OK if initialized, otherwise #E_NOT_OK
 */
STD_RETURN_TYPE_e CONT_GetInitializationState(void);

/**
 * @brief Returns the active power line.
 *
 * This function returns the value of #cont_state.activePowerLine
 *
 * @return value of #cont_state.activePowerLine
 */
extern CONT_POWER_LINE_e CONT_GetActivePowerLine(void);


/**
 * @brief   Sets the current state request of the state variable cont_state.
 *
 * @details This function is used to make a state request to the state machine,e.g, start voltage
 *          measurement, read result of voltage measurement, re-initialization.
 *          It calls cont_CheckStateRequest() to check if the request is valid. The state request
 *          is rejected if is not valid. The result of the check is returned immediately, so that
 *          the requester can act in case it made a non-valid state request.
 *
 * @param   state request to set
 *
 * @return  #CONT_OK if a state request was made, #CONT_STATE_NO_REQUEST if no state request was made
 */
extern CONT_RETURN_TYPE_e CONT_SetStateRequest(CONT_STATE_REQUEST_e statereq);


/**
 * @brief   Trigger function for the CONT driver state machine.
 *
 * @details This function contains the sequence of events in the CONT state machine. It must be
 *          called time-triggered, every 1ms. It exits without effect, if the function call is
 *          a reentrance.
 */
extern void CONT_Trigger(void);


#endif /* CONTACTOR_H_ */
