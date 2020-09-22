Roadmap for CAN message changes

# foxBMS CAN messages related to cell voltages and module temperatures

foxBMS use a unique CAN message ID for each bundle of data. Part of the message table is shown below. 

| Message Name                  | Message ID    | DLC   | Description   |
| :---                          | :---          | :---  | :---          |
|CAN0_Cell_Voltage_M0_0         | 0x200         | 8     | Cell voltages 0 to 2 for Module 0 |
|CAN0_Cell_Voltage_M0_1         | 0x201         | 8     | Cell voltages 3 to 5 for Module 0 |
|CAN0_Cell_Voltage_M0_2         | 0x202         | 8     | Cell voltages 6 to 8 for Module 0 |
|CAN0_Cell_Voltage_M0_3         | 0x203         | 8     | Cell voltages 9 to 11 for Module 0 |
|CAN0_Cell_Voltage_M0_4         | 0x204         | 8     | Cell voltages 12 to 14 for Module 0 |
|CAN0_Cell_Voltage_M0_5         | 0x205         | 8     | Cell voltages 15 to 17 for Module 0 |
|CAN0_Cell_temperature_M0_0     | 0x210         | 8     | Cell temperatures 0 to 2 for Module 0 |
|CAN0_Cell_temperature_M0_1     | 0x211         | 8     | Cell temperatures 3 to 5 for Module 0 |
|CAN0_Cell_temperature_M0_2     | 0x212         | 8     | Cell temperatures 6 to 8 for Module 0 |
|CAN0_Cell_temperature_M0_3     | 0x213         | 8     | Cell temperatures 9 to 11 for Module 0 |
|CAN0_Cell_Voltage_M1_0         | 0x220         | 8     | Cell voltages 0 to 2 for Module 1 |
|CAN0_Cell_Voltage_M1_1         | 0x221         | 8     | Cell voltages 3 to 5 for Module 1 |
|CAN0_Cell_Voltage_M1_2         | 0x222         | 8     | Cell voltages 6 to 8 for Module 1 |
|CAN0_Cell_Voltage_M1_3         | 0x223         | 8     | Cell voltages 9 to 11 for Module 1 |
|CAN0_Cell_Voltage_M1_4         | 0x224         | 8     | Cell voltages 12 to 14 for Module 1 |
|CAN0_Cell_Voltage_M1_5         | 0x225         | 8     | Cell voltages 15 to 17 for Module 1 |

For the complete list, see the Chapter titled "Communicating with foxBMS" in the foxBMS manual.

The current dictionary has the capacity to transmit 18 cell voltages for up to EIGHT modules. Beyond that, we have to add more CAN message IDs. This can lead to conflicts of message IDs when other devices are on the same CAN bus. Below, we will modify the data structure of the message to reduce the number of message IDs used for the transmission. To this end, we need to take a look at the data structure first.

# Data structure of the cell voltage/temperature CAN message

There are 8 bytes in the data field of the CAN messages for cell voltage/temperature. They are assigned as follows:
  * Byte 0: indicators for validities of the cell voltage/temperature data
  * Bytes 1 and 2: cell voltage/temperature 0
  * Bytes 3 and 4: cell voltage/temperature 1
  * Bytes 5 and 6: cell voltage/temperature 2
  * Byte 7: Not used 

Seeing the above structure, we can use two approaches to improve the expressiveness of the CAN messages. 

# New Approach 1: using Byte 7 to describe the module number

Using this approach, we just need to add the encoding of the module number using the last byte in the data field. The module number starts from 0. 

The change to the existing code is minimum. For example, for phxbms_interface.py, we just need to change  

# New Approach 2: using a single message to transmit all cell voltage/temperature data

yte 7 to describe the module number