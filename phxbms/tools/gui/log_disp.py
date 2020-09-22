# -*- coding: utf-8 -*-
"""
Created on Thu Sep  3 00:02:58 2020

@author: Susie
"""

import os,re
import numpy as np
import matplotlib.pyplot as plt

# default initial parameters
# filePath = '2020-09-03_11-07-16_logfile_can_foxbms.log.vt'
filePath = '2020-09-03_11-17-32_logfile_can_foxbms.log.vt'
ModuleNumber = 9
CellNumber = 12
TemperatureNumber = 3

if os.path.exists(filePath):
    file_object = open(filePath)
    raw_data = file_object.read()
    reg = r'\d+.\d+'
    locs_s = [g.span()[0] for g in re.finditer(reg, raw_data, re.I)]
    locs_e = [g.span()[1] for g in re.finditer(reg, raw_data, re.I)]
    temp = [float(raw_data[locs_s[i]:locs_e[i]]) for i in range(len(locs_s))]
    
    line_data_num = CellNumber + TemperatureNumber +1 
    sample_num = len(temp) // line_data_num 
    t_num = sample_num // ModuleNumber

    temp = np.array(temp).reshape(t_num, ModuleNumber, line_data_num)
    
    voltages = temp[:, :, 1:CellNumber+1] # dims: (t,module,cell)
    temperatures = temp[:, :, CellNumber+1:] # dims: (t,module,temperature)
    
    voltages = voltages.transpose() # dims: (cell,module,t)
    temperatures = temperatures.transpose() # dims: (temperature,module,t)
    

    xlabel = [t for t in range(t_num)]
    for i in range(ModuleNumber):
        # plot voltages
        plt.figure(figsize=(10,10))
        for j in range(CellNumber):
            plt.plot(xlabel, voltages[j,i]/1000, label = 'CELL_'+str(j))
            plt.xlabel('time (s)')
            plt.ylabel('voltage (V)')
        plt.ylim([1.0, 4.5])
        plt.legend(loc = 7)
        plt.title('Voltages of Module '+ str(i))
        plt.savefig('log_disp_figs/voltage_Module'+ str(i) + '.pdf')
        
        # plot temperatures
        # plt.figure(figsize=(10,10))
        # for j in range(TemperatureNumber):
        #     plt.plot(xlabel, temperatures[j,i], label='Temperature_'+str(j))
        #     plt.xlabel('time (s)')
        #     plt.ylabel('temperature (\degC)')
        # plt.ylim([24, 30])
        # plt.legend(loc = 7)
        # plt.title('Temperatures of Module '+ str(i))
        # plt.savefig('temperature_Module'+ str(i) + '.pdf')
print('done') 