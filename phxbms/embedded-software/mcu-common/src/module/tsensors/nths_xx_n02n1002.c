/**
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
 * @file    nths_xx_n02n1002.c
 * @author  foxBMS Team
 * @date    30.10.2018 (date of creation)
 * @ingroup TSENSORS
 * @prefix  NTHS_XX_N02N1002
 *
 * @brief   Resistive divider used for measuring temperature
 *
 */

/*================== Includes ===============================================*/
#include "nths_xx_n02n1002.h"

#include <float.h>
#include "foxmath.h"

/*================== Macros and Definitions =================================*/

/**
 * temperature-resistance LUT for Vishay NTHS_XX_N02N1002 NTC
 */
typedef struct {
    int16_t temperature_C;
    float resistance_Ohm;
} NTHS_XX_N02N1002_LUT_s;

/*================== Static Constant and Variable Definitions ===============*/

/**
 * LUT filled from higher resistance to lower resistance
 */
static const NTHS_XX_N02N1002_LUT_s nths_xx_n02n1002_LUT[] = {
        { -40, 214730.0}, 
        { -39, 202720.0}, 
        { -38, 191450.0}, 
        { -37, 180890.0}, 
        { -36, 170970.0}, 
        { -35, 161650.0}, 
        { -34, 152900.0}, 
        { -33, 144680.0}, 
        { -32, 136950.0}, 
        { -31, 129680.0}, 
        { -30, 122840.0}, 
        { -29, 116400.0}, 
        { -28, 110340.0}, 
        { -27, 104630.0}, 
        { -26, 99250.0}, 
        { -25, 94180.0}, 
        { -24, 89400.0}, 
        { -23, 84890.0}, 
        { -22, 80640.0}, 
        { -21, 76620.0}, 
        { -20, 72830.0}, 
        { -19, 69250.0}, 
        { -18, 65870.0}, 
        { -17, 62670.0}, 
        { -16, 59600.0}, 
        { -15, 56780.0}, 
        { -14, 54080.0}, 
        { -13, 51510.0}, 
        { -12, 49090.0}, 
        { -11, 46790.0}, 
        { -10, 44620.0}, 
        {  -9, 42550.0}, 
        {  -8, 40600.0}, 
        {  -7, 38750.0}, 
        {  -6, 36990.0}, 
        {  -5, 35320.0}, 
        {  -4, 33740.0}, 
        {  -3, 32230.0}, 
        {  -2, 30810.0}, 
        {  -1, 29450.0}, 
        {   0, 28160.0}, 
        {   1, 26920.0}, 
        {   2, 25760.0}, 
        {   3, 24650.0}, 
        {   4, 23600.0}, 
        {   5, 22600.0}, 
        {   6, 21640.0}, 
        {   7, 20740.0}, 
        {   8, 19870.0}, 
        {   9, 19050.0}, 
        {  10, 18270.0}, 
        {  11, 17510.0}, 
        {  12, 16800.0}, 
        {  13, 16120.0}, 
        {  14, 15470.0}, 
        {  15, 14850.0}, 
        {  16, 14260.0}, 
        {  17, 13700.0}, 
        {  18, 13160.0}, 
        {  19, 12640.0}, 
        {  20, 12160.0}, 
        {  21, 11680.0}, 
        {  22, 11230.0}, 
        {  23, 10800.0}, 
        {  24, 10390.0}, 
        {  25, 10000.0}, 
        {  26, 9624.0}, 
        {  27, 9265.0}, 
        {  28, 8921.0}, 
        {  29, 8591.0}, 
        {  30, 8276.0}, 
        {  31, 7973.0}, 
        {  32, 7684.0}, 
        {  33, 7406.0}, 
        {  34, 7140.0}, 
        {  35, 6885.0}, 
        {  36, 6641.0}, 
        {  37, 6406.0}, 
        {  38, 6181.0}, 
        {  39, 5965.0}, 
        {  40, 5758.0}, 
        {  41, 5559.0}, 
        {  42, 5368.0}, 
        {  43, 5185.0}, 
        {  44, 5008.0}, 
        {  45, 4839.0}, 
        {  46, 4676.0}, 
        {  47, 4520.0}, 
        {  48, 4370.0}, 
        {  49, 4225.0}, 
        {  50, 4086.0}, 
        {  51, 3953.0}, 
        {  52, 3824.0}, 
        {  53, 3700.0}, 
        {  54, 3581.0}, 
        {  55, 3467.0}, 
        {  56, 3356.0}, 
        {  57, 3250.0}, 
        {  58, 3147.0}, 
        {  59, 3049.0}, 
        {  60, 2954.0}, 
        {  61, 2862.0}, 
        {  62, 2774.0}, 
        {  63, 2689.0}, 
        {  64, 2607.0}, 
        {  65, 2528.0}, 
        {  66, 2451.0}, 
        {  67, 2378.0}, 
        {  68, 2306.0}, 
        {  69, 2238.0}, 
        {  70, 2172.0}, 
        {  71, 2108.0}, 
        {  72, 2046.0}, 
        {  73, 1986.0}, 
        {  74, 1929.0}, 
        {  75, 1873.0}, 
        {  76, 1820.0}, 
        {  77, 1768.0}, 
        {  78, 1717.0}, 
        {  79, 1669.0}, 
        {  80, 1622.0}, 
        {  81, 1577.0}, 
        {  82, 1533.0}, 
        {  83, 1490.0}, 
        {  84, 1449.0}, 
        {  85, 1410.0}, 
        {  86, 1371.0}, 
        {  87, 1334.0}, 
        {  88, 1298.0}, 
        {  89, 1263.0}, 
        {  90, 1229.0}, 
        {  91, 1197.0}, 
        {  92, 1165.0}, 
        {  93, 1134.0}, 
        {  94, 1105.0}, 
        {  95, 1076.0}, 
        {  96, 1048.0}, 
        {  97, 1021.0}, 
        {  98, 995.0}, 
        {  99, 969.0}, 
        { 100, 945.0}, 
        { 101, 921.0}, 
        { 102, 898.0}, 
        { 103, 875.0}, 
        { 104, 853.0}, 
        { 105, 832.0}, 
        { 106, 811.0}, 
        { 107, 792.0}, 
        { 108, 772.0}, 
        { 109, 753.0}, 
        { 110, 735.0}, 
        { 111, 717.0}, 
        { 112, 700.0}, 
        { 113, 683.0}, 
        { 114, 667.0}, 
        { 115, 651.0}, 
        { 116, 636.0}, 
        { 117, 621.0}, 
        { 118, 607.0}, 
        { 119, 593.0}, 
        { 120, 579.0}, 
        { 121, 566.0}, 
        { 122, 553.0}, 
        { 123, 540.0}, 
        { 124, 528.0}, 
        { 125, 516.0}  
};

static const uint16_t sizeLUT = sizeof(nths_xx_n02n1002_LUT)/sizeof(NTHS_XX_N02N1002_LUT_s);


/*================== Extern Constant and Variable Definitions ===============*/
/* Defines for calculating the ADC voltage on the ends of the operating range.
 * The ADC voltage is calculated with the following formula:
 *
 * Vadc = ((Vsupply * Rntc) / (R + Rntc))
 *
 * Depending on the position of the NTC in the voltage resistor (R1/R2),
 * different Rntc values are used for the calculation.
 */
#if NTHS_XX_N02N1002_POSITION_IN_RESISTOR_DIVIDER_IS_R1 == TRUE
#define ADC_VOLTAGE_VMAX_V    (float)((NTHS_XX_N02N1002_RESISTOR_DIVIDER_SUPPLY_VOLTAGE_V * nths_xx_n02n1002_LUT[sizeLUT-1].resistance_Ohm) / (nths_xx_n02n1002_LUT[sizeLUT-1].resistance_Ohm+NTHS_XX_N02N1002_RESISTOR_DIVIDER_RESISTANCE_R1_R2_Ohm))
#define ADC_VOLTAGE_VMIN_V    (float)((NTHS_XX_N02N1002_RESISTOR_DIVIDER_SUPPLY_VOLTAGE_V * nths_xx_n02n1002_LUT[0].resistance_Ohm) / (nths_xx_n02n1002_LUT[0].resistance_Ohm+NTHS_XX_N02N1002_RESISTOR_DIVIDER_RESISTANCE_R1_R2_Ohm))
#else /*NTHS_XX_N02N1002_POSITION_IN_RESISTOR_DIVIDER_IS_R1 == FALSE */
#define ADC_VOLTAGE_VMIN_V    (float)((NTHS_XX_N02N1002_RESISTOR_DIVIDER_SUPPLY_VOLTAGE_V * nths_xx_n02n1002_LUT[sizeLUT-1].resistance_Ohm) / (nths_xx_n02n1002_LUT[sizeLUT-1].resistance_Ohm+NTHS_XX_N02N1002_RESISTOR_DIVIDER_RESISTANCE_R1_R2_Ohm))
#define ADC_VOLTAGE_VMAX_V    (float)((NTHS_XX_N02N1002_RESISTOR_DIVIDER_SUPPLY_VOLTAGE_V * nths_xx_n02n1002_LUT[0].resistance_Ohm) / (nths_xx_n02n1002_LUT[0].resistance_Ohm+NTHS_XX_N02N1002_RESISTOR_DIVIDER_RESISTANCE_R1_R2_Ohm))
#endif
/*================== Static Function Prototypes =============================*/

/*================== Static Function Implementations ========================*/

/*================== Extern Function Implementations ========================*/

extern float NTHS_XX_N02N1002_GetTempFromLUT(uint16_t vadc_mV) {
    float temperature = 0.0;
    float resistance_Ohm = 0.0;
    float adcVoltage_V = vadc_mV/1000.0;    /* Convert mV to V */

    /* Check for valid ADC measurements to prevent undefined behavior */
    if (adcVoltage_V > ADC_VOLTAGE_VMAX_V) {
        /* Invalid measured ADC voltage -> sensor out of operating range or disconnected/shorted */
        temperature = -1000.0;
    } else if (adcVoltage_V < ADC_VOLTAGE_VMIN_V) {
        /* Invalid measured ADC voltage -> sensor out of operating range or shorted/disconnected */
        temperature = 1000.0;
    } else {
        /* Calculate NTC resistance based on measured ADC voltage */
#if NTHS_XX_N02N1002_POSITION_IN_RESISTOR_DIVIDER_IS_R1 == TRUE
        /* R1 = R2*((Vsupply/Vadc)-1) */
        resistance_Ohm = 
            NTHS_XX_N02N1002_RESISTOR_DIVIDER_RESISTANCE_R1_R2_Ohm *
            ((NTHS_XX_N02N1002_RESISTOR_DIVIDER_SUPPLY_VOLTAGE_V/adcVoltage_V) - 1);
#else /* NTHS_XX_N02N1002_POSITION_IN_RESISTOR_DIVIDER_IS_R1 == FALSE */
        /* R2 = R1*(V2/(Vsupply-Vadc)) */
        resistance_Ohm = 
            NTHS_XX_N02N1002_RESISTOR_DIVIDER_RESISTANCE_R1_R2_Ohm *
            (adcVoltage_V/(NTHS_XX_N02N1002_RESISTOR_DIVIDER_SUPPLY_VOLTAGE_V - adcVoltage_V));
#endif /* NTHS_XX_N02N1002_POSITION_IN_RESISTOR_DIVIDER_IS_R1 */

        /* Variables for interpolating LUT value */
        uint16_t between_high = 0;
        uint16_t between_low = 0;
        for (uint16_t i = 1; i < sizeLUT; i++) {
            if (resistance_Ohm < nths_xx_n02n1002_LUT[i].resistance_Ohm) {
                between_low = i+1;
                between_high = i;
            }
        }

        /* Interpolate between LUT vales, but do not extrapolate LUT! */
        if (!((between_high == 0 && between_low == 0) ||  /* measured resistance > maximum LUT resistance */
                 (between_low > sizeLUT))) {              /* measured resistance < minimum LUT resistance */
                     temperature = MATH_linearInterpolation(
                            nths_xx_n02n1002_LUT[between_low].resistance_Ohm,
                            nths_xx_n02n1002_LUT[between_low].temperature_C,
                            nths_xx_n02n1002_LUT[between_high].resistance_Ohm,
                            nths_xx_n02n1002_LUT[between_high].temperature_C,
                            resistance_Ohm);
        }
    }

    /* Return temperature based on measured NTC resistance */
    return temperature;
}


extern float NTHS_XX_N02N1002_GetTempFromPolynom(uint16_t vadc_mV) {
    // Note impletmented; 
    float temperature = -1000.0;
    return temperature;
}
