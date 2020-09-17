#!/usr/bin/env python
# -*- coding: utf-8 -*-

# @copyright &copy; 2010 - 2019, Fraunhofer-Gesellschaft zur Foerderung der
#   angewandten Forschung e.V. All rights reserved.
#
# BSD 3-Clause License
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
# 1.  Redistributions of source code must retain the above copyright notice,
#     this list of conditions and the following disclaimer.
# 2.  Redistributions in binary form must reproduce the above copyright notice,
#     this list of conditions and the following disclaimer in the documentation
#     and/or other materials provided with the distribution.
# 3.  Neither the name of the copyright holder nor the names of its
#     contributors may be used to endorse or promote products derived from this
#     software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#
# We kindly request you to use one or more of the following phrases to refer to
# foxBMS in your hardware, software, documentation or advertising materials:
#
# &Prime;This product uses parts of foxBMS&reg;&Prime;
#
# &Prime;This product includes parts of foxBMS&reg;&Prime;
#
# &Prime;This product is derived from foxBMS&reg;&Prime;

"""
@file       foxbms_interface.py
@date       11.12.2018 (date of creation)
@author     Written by foxBMS Team; Revised by JHL
@ingroup    tools
@prefix     none
@brief      Graphical user interface to communicate with foxBMS
"""

import threading
from multiprocessing import Process, Queue
import time
import sys
import re
import datetime
import os
import wx
import matplotlib
matplotlib.use('WXAgg') # noqa E402
from matplotlib.figure import Figure
from matplotlib.backends.backend_wxagg import FigureCanvasWxAgg as FigCanvas
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.ticker as plticker
import PCANBasic

logfile_size_limit = 100  # in megabytes
max_number_of_modules = 10      # JHL: changed from 100
max_number_of_cells = 18        # JHL: Changed from 20
max_number_of_temperatures = 16
plot_delay = 0.5                # JHL: What is the unit? Second?
baudrate_list = [100000, 125000, 250000, 500000, 800000, 1000000]
PCAN_baudratelist = {100000: PCANBasic.TPCANBaudrate(0x432F),
                    125000: PCANBasic.TPCANBaudrate(0x031C),
                    250000: PCANBasic.TPCANBaudrate(0x011C),
                    500000: PCANBasic.TPCANBaudrate(0x001C),
                    800000: PCANBasic.TPCANBaudrate(0x0016),
                    1000000: PCANBasic.TPCANBaudrate(0x0014)}

backgroundcolor = 'CADET BLUE'
backgroundcolor = (0x4B, 0x63, 0x6F)
backgroundcolor = (0x66, 0x88, 0x98)
backgroundcolor = (0x74, 0x9c, 0xb0)
backgroundcolor = 'WHITE'

# https://stackoverflow.com/questions/21036140/python-two-dimensional-array-changing-an-element
cell_voltages = [[0]*max_number_of_cells for _ in range(max_number_of_modules)]
cell_temps = [[0]*max_number_of_temperatures for _ in range(max_number_of_modules)]
vt_log_index = 1
vt_log_period = 1.0     # second

class can_message():
    def __init__(self):
        self.id = 0x00
        self.dlc = 8
        self.data = [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]


class parameter_list():
    def __init__(self):
        self.adapter_type = 'NONE'
        self.adapter_id = 'NONE'
        self.adapter = 'NONE'
        self.baudrate = 500000
        self.number_of_modules = 9      # Number of modules in the system
        self.number_of_cells = 12       # Number of cells in a module
        self.number_of_temperatures = 3
        self.logdata = True


def Initialize_with_HWID(hwid, baudrate):
    adapter = PCANBasic.PCANBasic()
    for bus in (PCANBasic.PCAN_USBBUS1, PCANBasic.PCAN_USBBUS2,
                PCANBasic.PCAN_USBBUS3, PCANBasic.PCAN_USBBUS4,
                PCANBasic.PCAN_USBBUS5, PCANBasic.PCAN_USBBUS6,
                PCANBasic.PCAN_USBBUS7, PCANBasic.PCAN_USBBUS8):
        init_result = adapter.Initialize(bus, baudrate)
        if init_result == 0:
            init_id = adapter.GetValue(bus, PCANBasic.PCAN_DEVICE_NUMBER)[1]
            if init_id == hwid:
                return adapter, bus
            else:
                adapter.Uninitialize(bus)
    return -1


def isnumericint(string):
    try:
        int(string)
        return True
    except BaseException:
        return False


def isnumericfloat(string):
    try:
        float(string)
        return True
    except BaseException:
        return False


# decodes two's complement for CAN data
# JHL: This is needed since int in Python can express arbitrarily large number.
def decode_two_complement(data, len):
    if len < 1 or len > 64:
        sys.exit('Problem with length in function decode_two_complement()')

    value = int(data)

    mask01 = 1 << (len - 1)
    mask02 = 0xFFFFFFFFFFFFFFFF >> (64 - len)

    if (value & mask01):
        value = ~value
        value = value & mask02
        value += 1
        signed_value = -value
    else:
        signed_value = value

    return signed_value


# extract data from CAN messages
def decode_CAN_data(message, bitstart, bitlength, factor,
                    offset, DLC, endianness, sign):
    if DLC < 1 or DLC > 8:
        sys.exit('Problem with DLC in function decode_CAN_data()')

    data = 0
    for i in range(DLC):
        if endianness == "LITTLE":
            data |= int(message[i]) << (i * 8)
        else:
            data |= int(message[i]) << ((DLC - i - 1) * 8)

    data >>= bitstart
    mask = 0xFFFFFFFFFFFFFFFF       # JHL: Guarantee to zero the leading bits
    mask <<= bitlength
    mask = ~mask
    data &= mask

    if sign == "SIGNED":
        data = decode_two_complement(data, bitlength)

    data = (float(data) / float(factor)) - float(offset)

    return data


# JHL: The app used to select the CAN adapter
class AdapterSelectionApp(wx.App):
    def __init__(self, parent=None):
        wx.App.__init__(self, False)
        self.frame = AdapterSelectionFrame(parent, -1, passedvalues=self)
        self.adapterFromSelectionFrame = ""  # This is the main data of this class

    def getFrameOutput(self):
        self.frame.Show()
        self.MainLoop()
        return self.adapterFromSelectionFrame


# JHL: The GUI used to select the CAN adapter
class AdapterSelectionFrame(wx.Frame):
    def __init__(self, parent, id, passedvalues):
        self.parameters = parameter_list()
        self.ref_x = 20
        self.ref_y = 45

        # BOARD
        self.adapter_list = []
        self.counter = 0
        self.adapter_counter = 0
        self.adapter_selected = 0

        wx.Frame.__init__(self, parent, id, 'foxBMS', size=(1180, 750))
        self.passedvalues = passedvalues
        self.Centre()
        self.SetBackgroundColour(backgroundcolor)

        # JHL: The static label of the frame.
        box = wx.StaticBox(self, wx.ID_ANY,
                    'foxBMS Graphical User Interface - CAN-Adapter Selection',
                    pos=(5, 5), size=(1130, 670))
        font = wx.Font(20, wx.DECORATIVE, wx.NORMAL, wx.BOLD)
        box.SetFont(font)
        box.SetForegroundColour((0, 0, 0))

        # JHL: Two imamges at the bottom of the frame
        png = wx.Image(os.path.join('_static', 'logo_foxbms.png'),
                        wx.BITMAP_TYPE_ANY).ConvertToBitmap()
        wx.StaticBitmap(self, -1, png, pos=(15, 575),
                        size=(png.GetWidth(), png.GetHeight()))

        png = wx.Image(os.path.join('_static', 'logo_fhg_iisb.png'),
                        wx.BITMAP_TYPE_ANY).ConvertToBitmap()
        wx.StaticBitmap(self, -1, png, pos=(855, 590),
                        size=(png.GetWidth(), png.GetHeight()))

        # JHL: The "Log data" check box
        self.logging_choice = wx.CheckBox(self, label='Log data',
                        pos=(self.ref_x + 860, self.ref_y + 350))
        self.logging_choice.SetValue(True)

        # JHL: The three buttons at the right up corner of the frame
        self.refreshButton = wx.Button(self, size=(240, 20),
                        pos=(self.ref_x + 860, self.ref_y + 5), label="REFRESH")
        self.Bind(wx.EVT_BUTTON, self.refreshButtonClick, self.refreshButton)

        self.selectButton = wx.Button(self, size=(240, 20),
                        pos=(self.ref_x + 860, self.ref_y + 35), label="SELECT")
        self.Bind(wx.EVT_BUTTON, self.selectButtonClick, self.selectButton)

        self.quitButton = wx.Button(self, size=(240, 20),
                pos=(self.ref_x + 860, self.ref_y + 65), label="Quit program")
        self.Bind(wx.EVT_BUTTON, self.quitButtonClick, self.quitButton)

        self.Bind(wx.EVT_CLOSE, self.quitButtonClick)

        # JHL: The TextCtrl fields for modifying the number of modules/cells/TCs
        global max_number_of_modules
        global max_number_of_cells
        global max_number_of_temperatures

        self.module_number_text = wx.StaticText(self, -1,
                'Number of modules (0<x ' + u'\u2264' +
                    str(max_number_of_modules) + ')',
                size=(240, 20), pos=(self.ref_x + 860, self.ref_y + 150),
                style=wx.TE_READONLY)
        self.module_number_field = wx.TextCtrl(self, -1,
                str(self.parameters.number_of_modules),
                size=(240, 20), pos=(self.ref_x + 860, self.ref_y + 170))
        self.module_number_field.Bind(wx.EVT_TEXT, self.OnNumberOfModulesModified)

        self.cell_number_text = wx.StaticText(self, -1,
                'Number of cells (0<x' + u'\u2264' +
                    str(max_number_of_cells) + ')',
                size=(240, 20), pos=(self.ref_x + 860, self.ref_y + 200),
                style=wx.TE_READONLY)
        self.cell_number_field = wx.TextCtrl(self, -1,
                str(self.parameters.number_of_cells),
                size=(240, 20), pos=(self.ref_x + 860, self.ref_y + 220))
        self.cell_number_field.Bind(wx.EVT_TEXT, self.OnNumberOfCellsModified)

        self.temperature_number_text = wx.StaticText(self, -1,
                'Number of temperatures (0<x' + u'\u2264' +
                    str(max_number_of_temperatures) + ')',
                size=(240, 20), pos=(self.ref_x + 860, self.ref_y + 250),
                style=wx.TE_READONLY)
        self.temperature_number_field = wx.TextCtrl(self, -1,
                str(self.parameters.number_of_temperatures),
                size=(240, 20), pos=(self.ref_x + 860, self.ref_y + 270))
        self.temperature_number_field.Bind(wx.EVT_TEXT,
                self.OnNumberOfTemperaturesModified)

        # JHL: Display the empty ListBox for selecting the CAN adapter
        self.listBoxAdapter = wx.ListBox(choices=[], parent=self,
                pos=(self.ref_x + 5, self.ref_y + 5),
                size=wx.Size(700, 200), style=0)
        self.listBoxAdapter.SetBackgroundColour(wx.Colour(255, 255, 255))
        self.listBoxAdapter.Bind(wx.EVT_LISTBOX, self.OnListBoxAdapter)

        # JHL: Display the empty ListBox for choosing the Bauderates
        self.listBoxBaudrate = wx.ListBox(choices=[], parent=self,
                pos=(self.ref_x + 5, self.ref_y + 250),
                size=wx.Size(700, 150), style=0)
        self.listBoxBaudrate.SetBackgroundColour(wx.Colour(255, 255, 255))

        global baudrate_list

        # JHL: Display the list of baudrates
        for i in range(len(baudrate_list)):
            self.listBoxBaudrate.Append(str(self.counter) +
                ' Baudrate ' + str(baudrate_list[i]) + ' bit/s')
            self.counter += 1

        self.listBoxBaudrate.SetSelection(
                baudrate_list.index(self.parameters.baudrate))

        # JHL: Display the list of CAN adapters
        self.updateAdapterList()

    def updateAdapterList(self):
        self.selectButton.Disable()
        self.adapter_selected = 0
        self.adapter_list = []
        self.listBoxAdapter.Clear()

        self.adapter_counter = 0
        # JHL: getAdapterList should be split to getAdapterList/dispAdapterList
        self.getAdapterList()
        self.passedvalues.adapterFromSelectionFrame = self.parameters

    def getAdapterList(self):
        self.detect_pcan()

    def detect_pcan(self):
        adapter_detected = 0

        adapter = PCANBasic.PCANBasic()
        for bus in (PCANBasic.PCAN_USBBUS1, PCANBasic.PCAN_USBBUS2,
                    PCANBasic.PCAN_USBBUS3, PCANBasic.PCAN_USBBUS4,
                    PCANBasic.PCAN_USBBUS5, PCANBasic.PCAN_USBBUS6,
                    PCANBasic.PCAN_USBBUS7, PCANBasic.PCAN_USBBUS8):
            init_result = adapter.Initialize(bus, PCANBasic.PCAN_BAUD_500K)
            if init_result == 0:
                init_id = adapter.GetValue(bus, PCANBasic.PCAN_DEVICE_NUMBER)[1]
                self.adapter_list.append(str(self.adapter_counter) + ' PCAN' + ' ' +
                    'Hardware ID' + ' ' + str(init_id) + ' (' + hex(init_id) + ')')
                # JHL: Display the detected CAN adapter
                # JHL: Note that this is not the best way to update the display list.
                # JHL: We need to update return the list and then display it later.
                self.listBoxAdapter.Append(str(self.adapter_counter) + ' PCAN' + ' ' +
                    'Hardware ID' + ' ' + str(init_id) + ' (' + hex(init_id) + ')')
                if adapter_detected == 0:
                    adapter_detected = 1
                    self.listBoxAdapter.SetSelection(0)
                    self.selectButton.Enable()
                self.adapter_counter += 1
                adapter.Uninitialize(bus)

    def checkConfiguredNumbers(self):
        module_number_OK = True
        cell_number_OK = True
        temperature_number_OK = True

        global max_number_of_modules
        global max_number_of_cells
        global max_number_of_temperatures

        if not isnumericint(self.module_number_field.GetValue()):
            module_number_OK = False
        else:
            number = int(self.module_number_field.GetValue())
            if number < 1 or number > max_number_of_modules:
                module_number_OK = False

        if not isnumericint(self.cell_number_field.GetValue()):
            cell_number_OK = False
        else:
            number = int(self.cell_number_field.GetValue())
            if number < 1 or number > max_number_of_cells:
                cell_number_OK = False

        if not isnumericint(self.temperature_number_field.GetValue()):
            temperature_number_OK = False
        else:
            number = int(self.temperature_number_field.GetValue())
            if number < 1 or number > max_number_of_temperatures:
                temperature_number_OK = False

        if module_number_OK is True \
                and cell_number_OK is True and temperature_number_OK is True:
            return True
        else:
            return False

    # called when "refresh" button clicked
    def refreshButtonClick(self, event):
        self.updateAdapterList()

    # called when "select" button clicked
    def selectButtonClick(self, event):
        adapter_select = re.compile('(^[0-9]+) (.*)')
        baudrate_select = re.compile('(^[0-9]+) (.*)')
        adapter_type_PCAN = re.compile('PCAN')
        adapter_type_PCAN_ID = re.compile('Hardware ID ([0-9]+) ')

        # JHL: Search for the selected Adapter
        try:
            string = str(self.listBoxAdapter.GetString(
                    self.listBoxAdapter.GetSelection()))
        except BaseException:
            return 0
        m = re.search(adapter_select, string)
        i = int(m.group(1))
        self.parameters.adapter = self.adapter_list[i]
        m = re.search(adapter_type_PCAN, string)
        if m:
            self.parameters.adapter_type = 'PCAN'
            m_id = re.search(adapter_type_PCAN_ID, string)
            self.parameters.id = int(m_id.group(1))

        # JHL: Search for the selected Baudrate
        string = str(self.listBoxBaudrate.GetString(
                self.listBoxBaudrate.GetSelection()))
        m = re.search(baudrate_select, string)
        i = int(m.group(1))
        global baudrate_list
        self.parameters.baudrate = baudrate_list[i]

        if self.logging_choice.GetValue() is True:
            self.parameters.logdata = True
        else:
            self.parameters.logdata = False

        if self.checkConfiguredNumbers() is True:
            self.parameters.number_of_modules = \
                    int(self.module_number_field.GetValue())
            self.parameters.number_of_cells = \
                    int(self.cell_number_field.GetValue())
            self.parameters.number_of_temperatures = \
                    int(self.temperature_number_field.GetValue())
            self.passedvalues.adapterFromSelectionFrame = self.parameters
            self.Destroy()

    def OnNumberOfModulesModified(self, event):
        if self.checkConfiguredNumbers() is False:
            self.selectButton.Disable()
        else:
            if self.adapter_selected == 1:
                self.selectButton.Enable()

    def OnNumberOfCellsModified(self, event):
        if self.checkConfiguredNumbers() is False:
            self.selectButton.Disable()
        else:
            if self.adapter_selected == 1:
                self.selectButton.Enable()

    def OnNumberOfTemperaturesModified(self, event):
        if self.checkConfiguredNumbers() is False:
            self.selectButton.Disable()
        else:
            if self.adapter_selected == 1:
                self.selectButton.Enable()

    # called when "quit" button clicked
    def quitButtonClick(self, event):
        self.passedvalues.adapterFromSelectionFrame = self.parameters
        self.Destroy()

    # called when element chosen in adapter list
    def OnListBoxAdapter(self, event):
        self.adapter_selected = 1
        if self.checkConfiguredNumbers() is True:
            self.selectButton.Enable()

    def returnParameters(self):
        self.MainLoop()
        return self.parameters


class MainFrame(wx.Frame):
    def __init__(self, parent, id, parameters, queue_can_action,
                 queue_can_receive, queue_can_send):

        # BOARD # JHL: what does this BOARD mean?
        self.ref_x = 20
        self.ref_y = 45
        self.refgraph_x = 20
        self.refgraph_y = 45
        self.refmodule_x = 20
        self.refmodule_y = 45

        self.min_voltage = ' '
        self.max_voltage = ' '
        self.min_temperature = ' '
        self.max_temperature = ' '

        self.general_error = 0
        self.current_state = 0
        self.over_temperature_charge = 0
        self.under_temperature_charge = 0
        self.over_temperature_discharge = 0
        self.under_temperature_discharge = 0
        self.over_current_charge = 0
        self.over_current_discharge = 0

        self.over_voltage = 0
        self.under_voltage = 0
        self.deep_discharge = 0
        self.over_temperature_ic = 0
        self.contactor_error = 0
        self.selftest_error = 0
        self.cantiming_error = 0
        self.current_sensor_error = 0
        self.balancing_active = 0

        self.main_plus = 0
        self.main_precharge = 0
        self.main_minus = 0
        self.main_chargeplus = 0
        self.main_chargeprecharge = 0
        self.main_chargeminus = 0

        self.interlock = 0

        self.insulation_error = 0
        self.fuse_state = 0
        self.lowcoincellvolt_error = 0
        self.openwire_error = 0
        self.daisychain_spi_error = 0
        self.daisychain_pec_error = 0
        self.daisychain_mux_error = 0
        self.daisychain_cfg_error = 0

        self.contactor_state = 0
        self.contactor_substate = 0

        self.ivt_current = 0
        self.ivt_pack_volt = 0

        self.error_list = {
            'Over temperature (charge) error': 0,
            'Under temperature (charge) error': 0,
            'Over temperature (discharge) error': 0,
            'Under temperature (discharge) error': 0,
            'Over current (charge) error': 0,
            'Over current (discharge) error': 0,
            'Over voltage error': 0,
            'Under voltage error': 0,
            'Deep discharge flag': 0,
            'Contactor error': 0,
            'Selftest error': 0,
            'CAN timing error': 0,
            'Current sensor error': 0,
            'Balancing active': 0,
            'CAN timing error': 0,
            'Insulation error': 0,
            'Low coin cell voltage error': 0,
            'Open wire error': 0,
            'Daisychain SPI error': 0,
            'Daisychain PEC error': 0,
            'Daisychain Mux error': 0,
            'Daisychain CFG error': 0,
        }

        self.logfilename = "logfile_can_foxbms"
        self.full_logfilename = "logfile_can_foxbms"
        system_date = str(time.strftime("%Y-%m-%d"))
        system_time = str(time.strftime("%H-%M-%S"))
        if parameters.logdata is True:
            self.logfile = open('start.log', "w")
            self.logfile.write(
                'Started on ' + str(system_date) + "_" + str(system_time))
            self.logfile.close()

        self.curve_0_x = []
        self.curve_0_y = []
        self.curve_1_x = []
        self.curve_1_y = []
        self.curve_2_x = []
        self.curve_2_y = []

        self.xmin = 0
        self.xmax = 100
        self.ymin = 1500
        self.ymax = 3000

        self.graph_autoscale = 1
        self.graph_xscale = 0
        self.graph_yscale = 0

        self.graph_started = 0
        self.plotting = 0

        self.time_initial = datetime.datetime.now()

        self.old_display_time = [datetime.datetime.now()] * 4

### JHL: Open the entire frame/panel.
        wx.Frame.__init__(self, parent, id, 'foxBMS', size=(1180, 700))
        self.Centre()    # the window opens at the center of the screen

        self.panel = wx.Panel(self)
        self.nb = wx.Notebook(self.panel)

        self.panel_list = []
        self.panel_list.append(wx.Panel(self.nb))

### JHL: The foxBMS (Main) tab
        # Add tabs, first tab(index 0)
        self.nb.AddPage(self.panel_list[0], 'foxBMS', 0)
        # Set colors of panels
        self.panel_list[0].SetBackgroundColour(backgroundcolor)

        box = wx.StaticBox(self.panel_list[0], wx.ID_ANY,
                'foxBMS Graphical User Interface',
                pos=(5, 5), size=(1130, 630))
        font = wx.Font(20, wx.DECORATIVE, wx.NORMAL, wx.BOLD)
        box.SetFont(font)
        box.SetForegroundColour((0, 0, 0))

        # JHL: The "Interface Control" field
        box = wx.StaticBox(self.panel_list[0], wx.ID_ANY, 'Interface Control',
                pos=(self.ref_x, self.ref_y), size=(488, 50))
        font = wx.Font(10, wx.DECORATIVE, wx.NORMAL, wx.BOLD)
        box.SetFont(font)

        # JHL: The control buttons in the "Interface Control" field
        self.startButton = wx.Button(self.panel_list[0], size=(150, 20),
                pos=(self.ref_x + 15, self.ref_y + 20), label="START")
        self.Bind(wx.EVT_BUTTON, self.startButtonClick, self.startButton)

        self.stopButton = wx.Button(self.panel_list[0], size=(150, 20),
                pos=(self.ref_x + 170, self.ref_y + 20), label="STOP")
        self.Bind(wx.EVT_BUTTON, self.stopButtonClick, self.stopButton)
        self.stopButton.Disable()

        self.quitButton = wx.Button(self.panel_list[0], size=(150, 20),
                pos=(self.ref_x + 325, self.ref_y + 20), label="Quit program")
        self.Bind(wx.EVT_BUTTON, self.quitButtonClick, self.quitButton)

        self.Bind(wx.EVT_CLOSE, self.quitButtonClick)

        # JHL: The "BMS Values" field
        box = wx.StaticBox(self.panel_list[0], wx.ID_ANY, 'BMS Values',
            pos=(self.ref_x, self.ref_y + 70), size=(488, 220))
        font = wx.Font(10, wx.DECORATIVE, wx.NORMAL, wx.BOLD)
        box.SetFont(font)

        # JHL: min/max values for voltage/temperature
        self.min_voltage_text = wx.StaticText(self.panel_list[0], -1,
                'minimum voltage', size=(150, 20),
                pos=(self.ref_x + 15, self.ref_y + 90), style=wx.TE_READONLY)
        self.min_voltage_label = wx.TextCtrl(self.panel_list[0], -1, '',
                size=(150, 20), pos=(self.ref_x + 15, self.ref_y + 110),
                style=wx.TE_READONLY)

        self.max_voltage_text = wx.StaticText(self.panel_list[0], -1,
                'maximum voltage', size=(150, 20),
                pos=(self.ref_x + 15, self.ref_y + 140), style=wx.TE_READONLY)
        self.max_voltage_label = wx.TextCtrl(self.panel_list[0], -1, '',
                size=(150, 20), pos=(self.ref_x + 15, self.ref_y + 160),
                style=wx.TE_READONLY)

        self.min_temperature_text = wx.StaticText(self.panel_list[0], -1,
                'minimum temperature', size=(150, 20),
                pos=(self.ref_x + 15, self.ref_y + 190), style=wx.TE_READONLY)
        self.min_temperature_label = wx.TextCtrl(self.panel_list[0], -1, '',
                size=(150, 20), pos=(self.ref_x + 15, self.ref_y + 210),
                style=wx.TE_READONLY)

        self.max_temperature_text = wx.StaticText(self.panel_list[0], -1,
                'maximum temperature', size=(150, 20),
                pos=(self.ref_x + 15, self.ref_y + 240), style=wx.TE_READONLY)
        self.max_temperature_label = wx.TextCtrl(self.panel_list[0], -1, '',
                size=(150, 20), pos=(self.ref_x + 15, self.ref_y + 260),
                style=wx.TE_READONLY)

        # JHL: values for current/voltage/temperature sensor
        # JHL: Need to reformat the code later
        self.current_text = wx.StaticText(self.panel_list[0], -1, 'current', size=(
            150, 20), pos=(self.ref_x + 170, self.ref_y + 90), style=wx.TE_READONLY)
        self.current_label = wx.TextCtrl(self.panel_list[0], -1, '', size=(
            150, 20), pos=(self.ref_x + 170, self.ref_y + 110), style=wx.TE_READONLY)

        self.current_V1_text = wx.StaticText(self.panel_list[0], -1, 'current sensor voltage 1', size=(
            150, 20), pos=(self.ref_x + 170, self.ref_y + 140), style=wx.TE_READONLY)
        self.current_V1_label = wx.TextCtrl(self.panel_list[0], -1, '', size=(
            150, 20), pos=(self.ref_x + 170, self.ref_y + 160), style=wx.TE_READONLY)

        self.current_V2_text = wx.StaticText(self.panel_list[0], -1, 'current sensor voltage 2', size=(
            150, 20), pos=(self.ref_x + 170, self.ref_y + 190), style=wx.TE_READONLY)
        self.current_V2_label = wx.TextCtrl(self.panel_list[0], -1, '', size=(
            150, 20), pos=(self.ref_x + 170, self.ref_y + 210), style=wx.TE_READONLY)

        self.current_V3_text = wx.StaticText(self.panel_list[0], -1, 'current sensor voltage 3', size=(
            150, 20), pos=(self.ref_x + 170, self.ref_y + 240), style=wx.TE_READONLY)
        self.current_V3_label = wx.TextCtrl(self.panel_list[0], -1, '', size=(
            150, 20), pos=(self.ref_x + 170, self.ref_y + 260), style=wx.TE_READONLY)

        # JHL: SOC values
        # JHL: Need to reformat the code later
        self.mean_soc_text = wx.StaticText(self.panel_list[0], -1, 'average SOC', size=(
            150, 20), pos=(self.ref_x + 325, self.ref_y + 90), style=wx.TE_READONLY)
        self.mean_soc_label = wx.TextCtrl(self.panel_list[0], -1, '', size=(
            150, 20), pos=(self.ref_x + 325, self.ref_y + 110), style=wx.TE_READONLY)

        self.min_soc_text = wx.StaticText(self.panel_list[0], -1, 'minimum SOC', size=(
            150, 20), pos=(self.ref_x + 325, self.ref_y + 140), style=wx.TE_READONLY)
        self.min_soc_label = wx.TextCtrl(self.panel_list[0], -1, '', size=(
            150, 20), pos=(self.ref_x + 325, self.ref_y + 160), style=wx.TE_READONLY)

        self.max_soc_text = wx.StaticText(self.panel_list[0], -1, 'maximum SOC', size=(
            150, 20), pos=(self.ref_x + 325, self.ref_y + 190), style=wx.TE_READONLY)
        self.max_soc_label = wx.TextCtrl(self.panel_list[0], -1, '', size=(
            150, 20), pos=(self.ref_x + 325, self.ref_y + 210), style=wx.TE_READONLY)


        # JHL: The "Contactors and Interlock" field
        box = wx.StaticBox(self.panel_list[0], wx.ID_ANY,
                'Contactors and Interlock',
                pos=(self.ref_x + 545, self.ref_y + 70), size=(178, 220))
        font = wx.Font(10, wx.DECORATIVE, wx.NORMAL, wx.BOLD)
        box.SetFont(font)

        # JHL: The contactor states
        # JHL: Need to reformat the code later
        self.mainplus_text = wx.StaticText(self.panel_list[0], -1, 'Main Plus', size=(
            150, 20), pos=(self.ref_x + 560, self.ref_y + 90), style=wx.TE_READONLY)
        self.mainplus_label = wx.TextCtrl(self.panel_list[0], -1, '', size=(
            150, 20), pos=(self.ref_x + 560, self.ref_y + 110), style=wx.TE_READONLY)

        self.mainprecharge_text = wx.StaticText(self.panel_list[0], -1, 'Precharge', size=(
            150, 20), pos=(self.ref_x + 560, self.ref_y + 140), style=wx.TE_READONLY)
        self.mainprecharge_label = wx.TextCtrl(self.panel_list[0], -1, '', size=(
            150, 20), pos=(self.ref_x + 560, self.ref_y + 160), style=wx.TE_READONLY)

        self.mainminus_text = wx.StaticText(self.panel_list[0], -1, 'Main Minus', size=(
            150, 20), pos=(self.ref_x + 560, self.ref_y + 190), style=wx.TE_READONLY)
        self.mainminus_label = wx.TextCtrl(self.panel_list[0], -1, '', size=(
            150, 20), pos=(self.ref_x + 560, self.ref_y + 210), style=wx.TE_READONLY)

        self.mainchargeplus_text = wx.StaticText(self.panel_list[0], -1, 'Engine', size=(
            150, 20), pos=(self.ref_x + 560, self.ref_y + 240), style=wx.TE_READONLY)
        self.mainchargeplus_label = wx.TextCtrl(self.panel_list[0], -1, '', size=(
            150, 20), pos=(self.ref_x + 560, self.ref_y + 260), style=wx.TE_READONLY)

        # JHL: The "BMS State: Error Flags" field
        box = wx.StaticBox(self.panel_list[0], wx.ID_ANY,
                'BMS State: Error Flags',
                pos=(self.ref_x + 770, self.ref_y), size=(330, 270))
        font = wx.Font(10, wx.DECORATIVE, wx.NORMAL, wx.BOLD)
        box.SetFont(font)

        # JHL: the "System state" display
        self.state_text = wx.StaticText(self.panel_list[0], -1, 'System state', size=(
            80, 15), pos=(self.ref_x + 805, self.ref_y + 25), style=wx.TE_READONLY)
        self.state_label = wx.TextCtrl(self.panel_list[0], -1, '', size=(
            200, 20), pos=(self.ref_x + 885, self.ref_y + 25), style=wx.TE_READONLY)
        # JHL: the "general error flag" display
        self.general_error_text = wx.StaticText(self.panel_list[0], -1, 'General error flag', size=(
            100, 20), pos=(self.ref_x + 785, self.ref_y + 50), style=wx.TE_READONLY)
        self.general_error_label = wx.TextCtrl(self.panel_list[0], -1, '', size=(
            200, 20), pos=(self.ref_x + 885, self.ref_y + 50), style=wx.TE_READONLY)
        # JHL: the "error list" box
        self.listErrors = wx.ListBox(self.panel_list[0], -1, choices=[], pos=(
            self.ref_x + 885, self.ref_y + 80), size=wx.Size(200, 180), style=0)
        self.listErrors.SetBackgroundColour(wx.Colour(255, 255, 255))

        # Contactor Module State
        box = wx.StaticBox(self.panel_list[0], wx.ID_ANY,
                'Contactor State',
                pos=(self.ref_x + 770, self.ref_y + 280), size=(330, 80))
        font = wx.Font(10, wx.DECORATIVE, wx.NORMAL, wx.BOLD)
        box.SetFont(font)
        # Contactor State Display
        self.contactor_state_text = wx.StaticText(self.panel_list[0], -1, 'Contactor State', size=(
            85, 15), pos=(self.ref_x + 795, self.ref_y + 305), style=wx.TE_READONLY)
        self.contactor_state_label = wx.TextCtrl(self.panel_list[0], -1, '', size=(
            200, 20), pos=(self.ref_x + 885, self.ref_y + 305), style=wx.TE_READONLY)
        # Contactor Substate Display
        self.contactor_substate_text = wx.StaticText(self.panel_list[0], -1, 'Contactor Substate', size=(
            110, 15), pos=(self.ref_x + 775, self.ref_y + 330), style=wx.TE_READONLY)
        self.contactor_substate_label = wx.TextCtrl(self.panel_list[0], -1, '', size=(
            200, 20), pos=(self.ref_x + 885, self.ref_y + 330), style=wx.TE_READONLY)

        # JHL: The "State Request and SOC" field
        box = wx.StaticBox(self.panel_list[0], wx.ID_ANY,
                'State Request and SOC',
                pos=(self.ref_x, self.ref_y + 320), size=(488, 175))
        font = wx.Font(10, wx.DECORATIVE, wx.NORMAL, wx.BOLD)
        box.SetFont(font)

        # JHL: the "No request" button
        self.norequestButton = wx.Button(self.panel_list[0], size=(150, 20),
                pos=(self.ref_x + 15, self.ref_y + 340), label="No request")
        self.Bind(wx.EVT_BUTTON, self.norequestButtonClick, self.norequestButton)
        # JHL: the "Standby" button
        self.standbyrequestButton = wx.Button(self.panel_list[0], size=(150, 20),
                pos=(self.ref_x + 170, self.ref_y + 340), label="Standby")
        self.Bind(wx.EVT_BUTTON, self.standbyrequestButtonClick,
                self.standbyrequestButton)
        # JHL: the "Normal" button
        self.normalrequestButton = wx.Button(self.panel_list[0], size=(150, 20),
                pos=(self.ref_x + 325, self.ref_y + 340), label="Normal")
        self.Bind(wx.EVT_BUTTON, self.normalrequestButtonClick,
                self.normalrequestButton)
        # Error Request button
        self.errorRequestButton = wx.Button(self.panel_list[0], size=(150, 20),
                pos=(self.ref_x + 15, self.ref_y + 365), label="Error Request")
        self.Bind(wx.EVT_BUTTON, self.errorRequestButtonClick,
                self.errorRequestButton)
        # Charge request button
        self.chargeRequestButton = wx.Button(self.panel_list[0], size=(150, 20),
                pos=(self.ref_x + 170, self.ref_y + 365), label="Charge")
        self.Bind(wx.EVT_BUTTON, self.chargeRequestButtonClick,
                self.chargeRequestButton)
        # Engine Request Button
        self.engineRequestButton = wx.Button(self.panel_list[0], size=(150, 20),
                pos=(self.ref_x + 325, self.ref_y + 365), label="Engine Request")
        self.Bind(wx.EVT_BUTTON, self.engineRequestButtonClick,
                self.engineRequestButton)
        # JHL: the "Stop sending request" button
        self.stopsendingrequestButton = wx.Button(self.panel_list[0],
                size=(150, 20), pos=(self.ref_x + 15, self.ref_y + 430),
                label="Stop sending request")
        self.Bind(wx.EVT_BUTTON, self.stopsendingrequestButtonClick,
                self.stopsendingrequestButton)
        # JHL: the "Request period" field
        self.request_period_text = wx.StaticText(self.panel_list[0], -1,
                'period 0<x' + u'\u2264' + '1000 ms', size=(150, 20),
                pos=(self.ref_x + 232, self.ref_y + 420), style=wx.TE_READONLY)
        self.request_period_field = wx.TextCtrl(self.panel_list[0], -1, '100',
                size=(150, 20), pos=(self.ref_x + 232, self.ref_y + 440))
        self.request_period_field.Bind(wx.EVT_TEXT, self.OnRequestPeriodModified)
        # JHL: the "Set SOC" button
        self.setSOCButton = wx.Button(self.panel_list[0], size=(150, 20),
                pos=(self.ref_x + 15, self.ref_y + 465), label="Set SOC")
        self.Bind(wx.EVT_BUTTON, self.setSOCButtonClick, self.setSOCButton)
        # JHL: the "Set SOC" field
        self.setSOC_field = wx.TextCtrl(self.panel_list[0], -1, '50.0',
                size=(150, 20), pos=(self.ref_x + 170, self.ref_y + 465))

        # APV: Misc. actions
        box = wx.StaticBox(
            self.panel_list[0],
            wx.ID_ANY,
            "Miscellaneous",
            pos=(self.ref_x, self.ref_y + 500),
            size=(488, 80))
        font = wx.Font(10, wx.DECORATIVE, wx.NORMAL, wx.BOLD)
        box.SetFont(font)

        # APV: Clear deep discharge flag
        self.resetDeepDischargeButton = wx.Button(self.panel_list[0], size=(
            150, 20), pos=(self.ref_x + 15, self.ref_y + 520), label="Clear Deep Discharge Flag")
        self.Bind(wx.EVT_BUTTON, self.resetDeepDischargeButtonClick,
                  self.resetDeepDischargeButton)

        # APV: Send software reset signal
        self.softwareResetButton = wx.Button(self.panel_list[0], size=(
            150, 20), pos=(self.ref_x + 170, self.ref_y + 520), label="Send Software Reset Signal")
        self.Bind(wx.EVT_BUTTON, self.softwareResetButtonClick,
                  self.softwareResetButton)


### JHL: The Graph tab
        self.panel_list.append(wx.Panel(self.nb))
        self.nb.AddPage(self.panel_list[1], 'Graph', 1)
        self.panel_list[1].SetBackgroundColour(backgroundcolor)

        # JHL: main title of the Graph tab
        box = wx.StaticBox(self.panel_list[1], wx.ID_ANY,
                'foxBMS Graphical User Interface - Curve Display',
                pos=(5, 5), size=(1130, 670))
        font = wx.Font(20, wx.DECORATIVE, wx.NORMAL, wx.BOLD)
        box.SetFont(font)
        box.SetForegroundColour((0, 0, 0))

        # JHL: Images of the Graph tab
        png = wx.Image(os.path.join('_static', 'logo_foxbms.png'),
                wx.BITMAP_TYPE_ANY).ConvertToBitmap()
        wx.StaticBitmap(self.panel_list[1], -1, png,
            pos=(940, 475), size=(png.GetWidth(), png.GetHeight()))

        png = wx.Image(os.path.join('_static', 'logo_fhg_iisb.png'),
                wx.BITMAP_TYPE_ANY).ConvertToBitmap()
        wx.StaticBitmap(self.panel_list[1], -1, png,
            pos=(855, 590), size=(png.GetWidth(), png.GetHeight()))

        # JHL: Curve display title of the Graph tab
        box = wx.StaticBox(self.panel_list[1], wx.ID_ANY, 'Curve Display',
                pos=(self.refgraph_x, self.refgraph_y), size=(620, 620))
        font = wx.Font(10, wx.DECORATIVE, wx.NORMAL, wx.BOLD)
        box.SetFont(font)

        # JHL: Produce the plot
        self.init_plot()
        self.panel_list[1].canvas = FigCanvas(self.panel_list[1], -1, self.fig)
        self.panel_list[1].canvas.SetPosition(
                (self.refgraph_x + 15, self.refgraph_y + 20))
        self.panel_list[1].canvas.SetSize((580, 580))

        # JHL: Control buttons on the right side
        box = wx.StaticBox(self.panel_list[1], wx.ID_ANY, 'Graph Controls',
                pos=(self.refgraph_x + 830, self.refgraph_y), size=(270, 335))
        font = wx.Font(10, wx.DECORATIVE, wx.NORMAL, wx.BOLD)
        box.SetFont(font)

        # JHL: The Auto scale button
        self.graphAutoButton = wx.Button(self.panel_list[1], size=(150, 20),
                pos=(self.refgraph_x + 845, self.refgraph_y + 20),
                label="Auto scale")
        self.Bind(wx.EVT_BUTTON, self.graphAutoButtonClick,
                self.graphAutoButton)

        # JHL: The Manual X scale button
        self.graphScaleXButton = wx.Button(self.panel_list[1], size=(150, 20),
                pos=(self.refgraph_x + 845, self.refgraph_y + 55),
                label="Manual X scale")
        self.Bind(wx.EVT_BUTTON, self.graphScaleXButtonClick,
                self.graphScaleXButton)

        # JHL: x_min and x_max
        self.minx_text = wx.StaticText(self.panel_list[1], -1, 'xmin',
                size=(240, 20),
                pos=(self.refgraph_x + 845, self.refgraph_y + 85),
                style=wx.TE_READONLY)
        self.minx_field = wx.TextCtrl(self.panel_list[1], -1, '',
                size=(240, 20),
                pos=(self.refgraph_x + 845, self.refgraph_y + 105))
        self.minx_field.Bind(wx.EVT_TEXT, self.OnMinXModified)

        self.maxx_text = wx.StaticText(self.panel_list[1], -1, 'xmax',
                size=(240, 20),
                pos=(self.refgraph_x + 845, self.refgraph_y + 135),
                style=wx.TE_READONLY)
        self.maxx_field = wx.TextCtrl(self.panel_list[1], -1, '',
                size=(240, 20),
                pos=(self.refgraph_x + 845, self.refgraph_y + 155))
        self.maxx_field.Bind(wx.EVT_TEXT, self.OnMaxXModified)

        # JHL: The Manual Y scale button
        self.graphScaleYButton = wx.Button(self.panel_list[1], size=(150, 20),
                pos=(self.refgraph_x + 845, self.refgraph_y + 205),
                label="Manual Y scale")
        self.Bind(wx.EVT_BUTTON, self.graphScaleYButtonClick,
                self.graphScaleYButton)

        # JHL: y_min and y_max
        self.miny_text = wx.StaticText(self.panel_list[1], -1, 'ymin',
                size=(240, 20),
                pos=(self.refgraph_x + 845, self.refgraph_y + 235),
                style=wx.TE_READONLY)
        self.miny_field = wx.TextCtrl(self.panel_list[1], -1, '', size=(240, 20),
                pos=(self.refgraph_x + 845, self.refgraph_y + 255))
        self.miny_field.Bind(wx.EVT_TEXT, self.OnMinYModified)

        self.maxy_text = wx.StaticText(self.panel_list[1], -1, 'ymax',
                size=(240, 20),
                pos=(self.refgraph_x + 845, self.refgraph_y + 285),
                style=wx.TE_READONLY)
        self.maxy_field = wx.TextCtrl(self.panel_list[1], -1, '', size=(240, 20),
                pos=(self.refgraph_x + 845, self.refgraph_y + 305))
        self.maxy_field.Bind(wx.EVT_TEXT, self.OnMaxYModified)


### JHL: The Module tabs
        # Every tab has the same color,same size,same position and is called
        # Module. # 0 is the main board
        for i in range(2, 1 + parameters.number_of_modules + 1):
            self.panel_list.append(wx.Panel(self.nb))
            self.nb.AddPage(self.panel_list[i], 'Module' + str(i - 2), i)
            self.panel_list[i].SetBackgroundColour(backgroundcolor)

            box = wx.StaticBox(self.panel_list[i], wx.ID_ANY,
                    'foxBMS Graphical User Interface - Module ' +
                    str(i - 2) + ' Information',
                    pos=(5, 5), size=(1130, 670))
            font = wx.Font(20, wx.DECORATIVE, wx.NORMAL, wx.BOLD)
            box.SetFont(font)
            box.SetForegroundColour((0, 0, 0))

            png = wx.Image(os.path.join('_static', 'logo_foxbms.png'),
                    wx.BITMAP_TYPE_ANY).ConvertToBitmap()
            wx.StaticBitmap(self.panel_list[i], -1, png,
                pos=(940, 475), size=(png.GetWidth(), png.GetHeight()))

            png = wx.Image(os.path.join('_static', 'logo_fhg_iisb.png'),
                    wx.BITMAP_TYPE_ANY).ConvertToBitmap()
            wx.StaticBitmap(self.panel_list[i], -1, png,
                pos=(855, 590), size=(png.GetWidth(), png.GetHeight()))

            self.panel_list[i].vbox = wx.BoxSizer(wx.VERTICAL)
            self.panel_list[i].hbox = wx.BoxSizer(wx.HORIZONTAL)

            self.panel_list[i].voltage_text_list = []
            self.panel_list[i].voltage_label_list = []
            self.panel_list[i].temperature_text_list = []
            self.panel_list[i].temperature_label_list = []

            box = wx.StaticBox(self.panel_list[i], wx.ID_ANY, 'Voltages',
                    pos=(self.refgraph_x, self.refgraph_y),
                    size=(245, 17 + 30 * parameters.number_of_cells))
            font = wx.Font(10, wx.DECORATIVE, wx.NORMAL, wx.BOLD)
            box.SetFont(font)

            for j in range(0, parameters.number_of_cells):
                self.panel_list[i].voltage_text_list.append(
                        wx.StaticText(self.panel_list[i], -1, 'cell' +
                                str(j) + ' voltage',
                        size=(80, 20),
                        pos=(self.refmodule_x + 15, self.refmodule_y + 20 + j * 30),
                        style=wx.TE_READONLY))
                self.panel_list[i].voltage_label_list.append(
                        wx.TextCtrl(self.panel_list[i], -1, '',
                        size=(120, 20),
                        pos=(self.refmodule_x + 105, self.refmodule_y + 20 + j * 30),
                        style=wx.TE_READONLY))

            box = wx.StaticBox(self.panel_list[i], wx.ID_ANY, 'Temperatures',
                    pos=(self.refgraph_x + 280, self.refgraph_y),
                    size=(260, 17 + 30 * parameters.number_of_temperatures))
            font = wx.Font(10, wx.DECORATIVE, wx.NORMAL, wx.BOLD)
            box.SetFont(font)

            for j in range(0, parameters.number_of_temperatures):
                self.panel_list[i].temperature_text_list.append(
                        wx.StaticText(self.panel_list[i], -1, 'cell' +
                                str(j) + ' temperature',
                        size=(100, 20),
                        pos=(self.refmodule_x + 295, self.refmodule_y + 20 + j * 30),
                        style=wx.TE_READONLY))
                self.panel_list[i].temperature_label_list.append(
                        wx.TextCtrl(self.panel_list[i], -1, '',
                        size=(120, 20),
                        pos=(self.refmodule_x + 405, self.refmodule_y + 20 + j * 30),
                        style=wx.TE_READONLY))


# JHL: The tabs are set up. Now set up the sizer and others.
        sizer = wx.BoxSizer()
        sizer.Add(self.nb, 1, wx.EXPAND)
        self.panel.SetSizer(sizer)

        # JHL: select first tab by default
        self.nb.SetSelection(0)

        # JHL: various queues in the system
        # queue to communicate with the display thread
        self.q_display = Queue(maxsize=0)
        # queue to plot
        self.q_plot = Queue(maxsize=0)
        # queue to communicate parameters to the graph
        self.q_graph_setup = Queue(maxsize=0)
        # queue to communicate with the periodic send thread
        self.q_periodicsend_action = Queue(maxsize=0)
        # queue to give sendperiod
        self.q_periodictrigger = Queue(maxsize=0)

### JHL: threads of the system
        # Run the main GUI thread
        self.t_display = threading.Thread(target=self.main_gui_thread,
                args=(parameters.number_of_modules, parameters.number_of_cells,
                parameters.number_of_temperatures,))
        self.t_display.setDaemon(True)
        self.t_display.start()
        # Run the periodic send thread
        self.t_send = threading.Thread(target=self.periodic_send_thread)
        self.t_send.setDaemon(True)
        self.t_send.start()

    # JHL: define the main GUI thread
    def main_gui_thread(self, number_of_modules, number_of_cells,
            number_of_temperatures):
        action = 'WAIT'
        can_data = 0

        while True:
            if not self.q_display.empty():
                action = (self.q_display.get())

            if action == 'EXIT':
                break

            if action == 'WAIT':
                pass

            if action == 'RUN':
                self.graph_started = 1

                if not queue_can_receive.empty():
                    can_data = (queue_can_receive.get())

                    time_now = datetime.datetime.now()
                    actual_time = (time_now - self.time_initial).total_seconds()
                    if parameters.logdata is True:
                        self.logfile.write(str(actual_time))
                        self.logfile.write(" ")
                        self.logfile.write(str(can_data.id))
                        self.logfile.write(" ")
                        self.logfile.write(str(can_data.dlc))
                        for i in can_data.data:
                            self.logfile.write(" ")
                            self.logfile.write(str(i))
                        self.logfile.write("\n")

                        global vt_log_index
                        global vt_log_period
                        if actual_time >= vt_log_index * vt_log_period:
                            vt_log_index += 1
                            # self.vt_logfile.write("Time (sec): " + str(actual_time).zfill(5) + "\n")
                            # for i in range(number_of_modules):
                            #     self.vt_logfile.write("M" + str(i))
                            #     self.vt_logfile.write(" CV (V): [")
                            #     for j in range(number_of_cells - 1):
                            #         self.vt_logfile.write(str(cell_voltages[i][j]/1000) + ", ")
                            #     self.vt_logfile.write(str(cell_voltages[i][j+1]/1000) + "] ")
                            #     self.vt_logfile.write(" T (C): {")
                            #     for j in range(number_of_temperatures - 1):
                            #         self.vt_logfile.write(str(cell_temps[i][j]) + ", ")
                            #     self.vt_logfile.write(str(cell_temps[i][j+1]) + "} ")
                            #     self.vt_logfile.write("\n")
                            self.vt_logfile.write("Time (s): " + str(actual_time))
                            self.vt_logfile.write(", Current (A): " + str(self.ivt_current))
                            # self.vt_logfile.write(", Pack Voltage (V): " + str(self.ivt_pack_volt) + "\n")
                            self.vt_logfile.write("\n")
                            for i in range(number_of_modules):
                                self.vt_logfile.write("Module#" + str(i))
                                self.vt_logfile.write(" Voltages: [")
                                for j in range(number_of_cells - 1):
                                    self.vt_logfile.write(str(cell_voltages[i][j]) + ", ")
                                self.vt_logfile.write(str(cell_voltages[i][j+1]) + "] ")
                                self.vt_logfile.write(" Temperatures: [")
                                for j in range(number_of_temperatures - 1):
                                    self.vt_logfile.write(str(cell_temps[i][j]) + ", ")
                                self.vt_logfile.write(str(cell_temps[i][j+1]) + "] ")
                                self.vt_logfile.write("\n")
                            self.vt_logfile.write("\n")

                    decoded_can_data = self.decode_can_data(can_data,
                            number_of_modules, number_of_cells,
                            number_of_temperatures)
                    self.update_display(decoded_can_data)

                    # collect data for graph
                    if decoded_can_data != 0:
                        if decoded_can_data[0] == 'voltageminmax':
                            self.q_plot.put(decoded_can_data)

    # JHL: define the periodic send thread
    def periodic_send_thread(self):
        action = 'WAIT'
        period = 0.100
        request = 'NOREQUEST'

        can_message_02 = can_message()

        while True:
            if not self.q_periodicsend_action.empty():
                action = self.q_periodicsend_action.get()

            if action == 'EXIT':
                break

            if action == 'WAIT':
                time.sleep(float(period))

            if action == 'RUN':
                if not self.q_periodictrigger.empty():
                    data = self.q_periodictrigger.get()

                if float(period) > 0:
                    request = data[0]
                    period = data[1]

                    can_message_02.id = 0x120
                    can_message_02.dlc = 8
                    can_message_02.data = [
                            0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]

                    if request == 'NOREQUEST':
                        can_message_02.data = [
                                0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]
                    if request == 'STANDBY':
                        can_message_02.data = [
                                0x00, 0x08, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]
                    if request == 'NORMAL':
                        can_message_02.data = [
                                0x00, 0x03, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]
                    if request == 'CHARGE':
                        can_message_02.data = [
                                0x00, 0x04, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]
                    if request == 'ENGINE':
                        can_message_02.data = [
                                0x00, 0x05, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]

                    queue_can_send.put(can_message_02)
                    time.sleep(float(period))


### JHL: Handlers for the controls on the Main tab.
    # called when "no request" button clicked
    def norequestButtonClick(self, event):
        self.request_period_field.Disable()
        self.q_periodictrigger.put(['NOREQUEST'] +
                [float(self.request_period_field.GetValue()) / 1000.0])
        self.q_periodicsend_action.put('RUN')

    # called when "standby" button clicked
    def standbyrequestButtonClick(self, event):
        self.request_period_field.Disable()
        self.q_periodictrigger.put(['STANDBY'] +
                [float(self.request_period_field.GetValue()) / 1000.0])
        self.q_periodicsend_action.put('RUN')

    # called when "normal" button clicked
    def normalrequestButtonClick(self, event):
        self.request_period_field.Disable()
        self.q_periodictrigger.put(['NORMAL'] +
                [float(self.request_period_field.GetValue()) / 1000.0])
        self.q_periodicsend_action.put('RUN')

    def errorRequestButtonClick(self, event):
        error_message = can_message()

        error_message.id = 0x100
        error_message.dlc = 8
        error_message.data = [
            0xBB, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]

        queue_can_send.put(error_message)

    def chargeRequestButtonClick(self, event):
        self.request_period_field.Disable()
        self.q_periodictrigger.put(['CHARGE'] +
                [float(self.request_period_field.GetValue()) / 1000.0])
        self.q_periodicsend_action.put('RUN')

    def engineRequestButtonClick(self, event):
        self.request_period_field.Disable()
        self.q_periodictrigger.put(['ENGINE'] +
                [float(self.request_period_field.GetValue()) / 1000.0])
        self.q_periodicsend_action.put('RUN')


    # called when "stop sending" button clicked
    def stopsendingrequestButtonClick(self, event):
        self.q_periodicsend_action.put('WAIT')
        self.request_period_field.Enable()

    def OnRequestPeriodModified(self, event):
        value = self.request_period_field.GetValue()
        if not isnumericint(value):
            self.request_period_field.SetValue('100')
        elif int(value) < 0 or int(value) > 1000:
            self.request_period_field.SetValue('100')

    # called when "start" button clicked
    def startButtonClick(self, event):
        self.quitButton.Disable()
        self.stopButton.Enable()
        self.startButton.Disable()
        if parameters.logdata is True:
            logfilename = self.get_logfile_name()
            self.logfile = open(logfilename, "w")
            self.vt_logfile = open(logfilename + ".vt", "w")
            
        time.sleep(0.1)
        self.time_initial = datetime.datetime.now()
        self.q_display.put('RUN')
        queue_can_action.put('RUN')     # Queue for CAN action defined in main

        self.plotting = 1
        graph_time = 0
        self.reset_plot_data()

        while self.plotting == 1:       # JHL: loops here until self.plotting = 0
            if not self.q_plot.empty():
                decoded_can_data = (self.q_plot.get())

                time_now = datetime.datetime.now()
                actual_time = (time_now - self.time_initial).total_seconds()
                self.curve_0_x.append(actual_time)
                self.curve_0_y.append(decoded_can_data[1])
                self.curve_1_x.append(actual_time)
                self.curve_1_y.append(decoded_can_data[2])
                self.curve_2_x.append(actual_time)
                self.curve_2_y.append(decoded_can_data[3])

                if time.time() - graph_time > 0.2:
                    self.draw_plot()
                    graph_time = time.time()
                    plt.show()

            self.Update()
            wx.Yield()

    # called when "stop" button clicked
    def stopButtonClick(self, event):
        self.plotting = 0
        self.quitButton.Enable()
        self.stopButton.Disable()
        self.startButton.Enable()
        self.q_display.put('WAIT')
        queue_can_action.put('WAIT')
        time.sleep(0.5)
        if parameters.logdata is True:
            self.logfile.close()
            self.vt_logfile.close()

    def quitButtonClick(self, event):
        self.plotting = 0
        self.q_display.put('EXIT')
        self.q_periodicsend_action.put('EXIT')
        queue_can_action.put('EXIT')
        time.sleep(0.1)
        if parameters.logdata is True:
            if not self.logfile.closed:
                self.logfile.close()
                self.vt_logfile.close()
        self.t_display.join()
        self.t_send.join()
        self.Destroy()

    def setSOCButtonClick(self, event):
        can_message_03 = can_message()
        can_message_03.id = 0x100
        can_message_03.dlc = 8
        lowbyte = 0
        highbyte = 0
        SOC = self.setSOC_field.GetValue()
        if isnumericfloat(SOC):
            SOC = float(SOC)
            if int(SOC) >= 0 and int(SOC) <= 100:
                SOC = int(SOC * 100)
                lowbyte = (SOC & 0xFF)
                highbyte = (SOC & 0xFF00) >> 8
                can_message_03.data = [0x0B, highbyte, lowbyte,
                        0x00, 0x00, 0x00, 0x00, 0x00]
                queue_can_send.put(can_message_03)

    def resetDeepDischargeButtonClick(self, event):
        clear_flag_message = can_message()

        clear_flag_message.id = 0x100
        clear_flag_message.dlc = 8
        clear_flag_message.data = [
            0xAA, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]

        queue_can_send.put(clear_flag_message)

    def softwareResetButtonClick(self, event):
        software_reset_message = can_message()

        software_reset_message.id = 0x95
        software_reset_message.dlc = 8

        queue_can_send.put(software_reset_message)

### JHL: Handlers for the controls on the Graph tab.
    def graphAutoButtonClick(self, event):
        self.q_graph_setup.put(['Auto'] + ['None'])

    def graphScaleXButtonClick(self, event):
        xmin = self.minx_field.GetValue()
        xmax = self.maxx_field.GetValue()
        self.q_graph_setup.put(['ScaleX'] + [xmin] + [xmax])

    def graphScaleYButtonClick(self, event):
        ymin = self.miny_field.GetValue()
        ymax = self.maxy_field.GetValue()
        self.q_graph_setup.put(['ScaleY'] + [ymin] + [ymax])

    def OnMinXModified(self, event):
        pass

    def OnMaxXModified(self, event):
        pass

    def OnMinYModified(self, event):
        pass

    def OnMaxYModified(self, event):
        pass

### JHL: plot related methods
    def reset_plot_data(self):
        self.curve_0_x = []
        self.curve_0_y = []
        self.curve_1_x = []
        self.curve_1_y = []
        self.curve_2_x = []
        self.curve_2_y = []

    def init_plot(self):
        self.dpi = 65
        self.fig = Figure((8.0, 8.0), dpi=self.dpi)

        self.ax1 = self.fig.add_subplot(111)

        self.ax1.set_ylabel("V (mV)")
        self.ax1.set_xlabel("Time (s)")

        self.ax1.grid(axis='x', color='black', linestyle='dashed',
                linewidth=0.5, which='major')
        self.ax1.grid(axis='y', color='black', linestyle='dashed',
                linewidth=0.5, which='major')

        self.curve0 = self.ax1.plot(self.curve_0_y,
                color='red', label='voltage mean', linewidth=1)[0]
        self.curve1 = self.ax1.plot(self.curve_1_y,
                color='blue', label='voltage min', linewidth=1)[0]
        self.curve2 = self.ax1.plot(self.curve_2_y,
                color='green', label='voltage max', linewidth=1)[0]

        self.fig.subplots_adjust(top=0.98)
        self.fig.subplots_adjust(bottom=0.15)

        self.ax1.legend(loc='upper center', bbox_to_anchor=(0.5, -0.080),
                fancybox=True, shadow=True, ncol=5, prop={'size': 10})

    def draw_plot(self):
        if not self.q_graph_setup.empty():
            setup = (self.q_graph_setup.get())

            if setup[0] == 'Auto':
                self.graph_autoscale = 1
                self.graph_xscale = 0
                self.graph_yscale = 0
            if setup[0] == 'ScaleX':
                if isnumericfloat(setup[1]) and isnumericfloat(setup[2]):
                    xmin = float(setup[1])
                    xmax = float(setup[2])
                    if xmin < xmax:
                        self.xmin = float(setup[1])
                        self.xmax = float(setup[2])
                        self.graph_autoscale = 0
                        self.graph_xscale = 1
            if setup[0] == 'ScaleY':
                if isnumericfloat(setup[1]) and isnumericfloat(setup[2]):
                    ymin = float(setup[1])
                    ymax = float(setup[2])
                    if ymin < ymax:
                        self.ymin = float(setup[1])
                        self.ymax = float(setup[2])
                        self.graph_autoscale = 0
                        self.graph_yscale = 1

        if self.graph_autoscale == 1:
            if      (len(self.curve_0_x) > 0 and len(self.curve_0_y) > 0) or \
                    (len(self.curve_1_x) > 0 and len(self.curve_1_y) > 0) or \
                    (len(self.curve_2_x) > 0 and len(self.curve_2_y) > 0):

                self.xmin = min(self.curve_0_x +
                                self.curve_1_x + self.curve_2_x)
                self.xmax = max(self.curve_0_x +
                                self.curve_1_x + self.curve_2_x)
                self.ymin = min(self.curve_0_y +
                                self.curve_1_y + self.curve_2_y)
                self.ymax = max(self.curve_0_y +
                                self.curve_1_y + self.curve_2_y)
            else:
                self.xmin = 0
                self.xmax = 100
                self.ymin = 1500
                self.ymax = 3000

            self.ax1.set_xbound(lower=self.xmin, upper=self.xmax + 1)
            self.ax1.set_ybound(lower=self.ymin - 10, upper=self.ymax + 10)

        else:
            if self.graph_xscale == 1:
                if self.graph_yscale == 0:
                    if len(self.curve_0_y) > 0 or len(self.curve_1_y) > 0 or \
                            len(self.curve_2_y) > 0:
                        self.ymin = min(self.curve_0_y +
                                        self.curve_1_y + self.curve_2_y)
                        self.ymax = max(self.curve_0_y +
                                        self.curve_1_y + self.curve_2_y)
                    else:
                        self.ymin = 1500
                        self.ymax = 3000
                    self.ax1.set_ybound(lower=self.ymin - 10, upper=self.ymax + 10)
                self.ax1.set_xbound(lower=self.xmin, upper=self.xmax)
            if self.graph_yscale == 1:
                if self.graph_xscale == 0:
                    if len(self.curve_0_x) > 0 or len(self.curve_1_x) > 0 or \
                            len(self.curve_2_x) > 0:
                        self.xmin = min(self.curve_0_x +
                                        self.curve_1_x + self.curve_2_x)
                        self.xmax = max(self.curve_0_x +
                                        self.curve_1_x + self.curve_2_x)
                    else:
                        self.xmin = 0
                        self.xmax = 100
                    self.ax1.set_xbound(lower=self.xmin, upper=self.xmax + 1)
                self.ax1.set_ybound(lower=self.ymin, upper=self.ymax)

        range_on_x = abs(self.xmax - self.xmin)
        locatorbase_on_x = int(range_on_x / 10)
        range_on_y = abs(self.ymax - self.ymin)
        locatorbase_on_y = int(range_on_y / 10)

        if locatorbase_on_x <= 0:
            locatorbase_on_x = 1
        if locatorbase_on_y <= 0:
            locatorbase_on_y = 1

        # this locator puts ticks at regular intervals
        locx = plticker.MultipleLocator(base=locatorbase_on_x)
        # this locator puts ticks at regular intervals
        locy = plticker.MultipleLocator(base=locatorbase_on_y)
        self.ax1.xaxis.set_major_locator(locx)
        self.ax1.yaxis.set_major_locator(locy)

        self.curve0.set_xdata(np.array(self.curve_0_x))
        self.curve0.set_ydata(np.array(self.curve_0_y))

        self.curve1.set_xdata(np.array(self.curve_1_x))
        self.curve1.set_ydata(np.array(self.curve_1_y))

        self.curve2.set_xdata(np.array(self.curve_2_x))
        self.curve2.set_ydata(np.array(self.curve_2_y))

        self.panel_list[1].canvas.draw()


### JHL: other helper methods
    def get_logfile_name(self):
        fileincrementlimit = 0
        logfilecounter = 0

        system_date = str(time.strftime("%Y-%m-%d"))
        system_time = str(time.strftime("%H-%M-%S"))

        filename = system_date + "_" + system_time + "_" + self.logfilename + ".log"

        while(os.path.isfile(filename)):
            filename = system_date + "_" + system_time + "_" + \
                    self.logfilename + "_" + str(logfilecounter) + ".log"
            logfilecounter += 1
            fileincrementlimit += 1
            if fileincrementlimit > 100:
                print("Unable to open a file to write log, exiting")
                exit()

        self.full_logfilename = filename

        return filename


    def update_display(self, decoded_can_data):
        if decoded_can_data == 0:
            return

        global plot_delay
        _deg = ' ' + u'\u2103'
        _mV = ' mV'

        # The Main tab
        if decoded_can_data[0] == 'voltageminmax':
            self.min_voltage_label.SetValue(str(decoded_can_data[2]) + _mV)
            self.max_voltage_label.SetValue(str(decoded_can_data[3]) + _mV)

        if decoded_can_data[0] == 'temperatureminmax':
            self.min_temperature_label.SetValue(str(decoded_can_data[2]) + _deg)
            self.max_temperature_label.SetValue(str(decoded_can_data[3]) + _deg)

        if decoded_can_data[0] == 'current':
            current_time = datetime.datetime.now()
            if (current_time - self.old_display_time[0]).total_seconds() > plot_delay:
                self.ivt_current = decoded_can_data[1]
                self.current_label.SetValue(str(self.ivt_current) + ' A')
                self.old_display_time[0] = datetime.datetime.now()

        if decoded_can_data[0] == 'current_V1':
            current_time = datetime.datetime.now()
            if (current_time - self.old_display_time[1]).total_seconds() > plot_delay:
                self.ivt_pack_volt = decoded_can_data[1]
                self.current_V1_label.SetValue(str(self.ivt_pack_volt) + ' V')
                self.old_display_time[1] = datetime.datetime.now()

        if decoded_can_data[0] == 'current_V2':
            current_time = datetime.datetime.now()
            if (current_time - self.old_display_time[2]).total_seconds() > plot_delay:
                current_V2 = decoded_can_data[1]
                self.current_V2_label.SetValue(str(current_V2) + ' V')
                self.old_display_time[2] = datetime.datetime.now()

        if decoded_can_data[0] == 'current_V3':
            current_time = datetime.datetime.now()
            if (current_time - self.old_display_time[3]).total_seconds() > plot_delay:
                current_V3 = decoded_can_data[1]
                self.current_V3_label.SetValue(str(current_V3) + ' V')
                self.old_display_time[3] = datetime.datetime.now()

        if decoded_can_data[0] == 'socminmax':
            self.mean_soc_label.SetValue(str(decoded_can_data[1]) + ' %')
            self.min_soc_label.SetValue(str(decoded_can_data[2]) + ' %')
            self.max_soc_label.SetValue(str(decoded_can_data[3]) + ' %')

        if decoded_can_data[0] == 'contactor_state':
            self.contactor_state = decoded_can_data[1]
            self.contactor_substate = decoded_can_data[2]
             # Display Contactor State
            if self.contactor_state == 0x00:
                self.contactor_state_label.SetValue('Uninitialized')
            if self.contactor_state == 0x03:
                self.contactor_state_label.SetValue('Idle')
            if self.contactor_state == 0x04:
                self.contactor_state_label.SetValue('Standby')
            if self.contactor_state == 0x05:
                self.contactor_state_label.SetValue('Normal Precharge')
            if self.contactor_state == 0x06:
                self.contactor_state_label.SetValue('Normal')
            if self.contactor_state == 0x07:
                self.contactor_state_label.SetValue('Charge Precharge')
            if self.contactor_state == 0x08:
                self.contactor_state_label.SetValue('Charge')
            if self.contactor_state == 0x09:
                self.contactor_state_label.SetValue('Engine Precharge')
            if self.contactor_state == 0x0A:
                self.contactor_state_label.SetValue('Engine')
            if self.contactor_state == 0x0B:
                self.contactor_state_label.SetValue('Open All Contactors')
            if self.contactor_state == 0xF0:
                self.contactor_state_label.SetValue('Error')
            #LMM
             # Display Contactor Substate
            if self.contactor_substate == 0x00:
                self.contactor_substate_label.SetValue('Entry State')
            if self.contactor_substate == 0x01:
                self.contactor_substate_label.SetValue('Open First Contactor')
            if self.contactor_substate == 0x02:
                self.contactor_substate_label.SetValue('Open Second Contactor')
            if self.contactor_substate == 0x03:
                self.contactor_substate_label.SetValue('Open Third Contactor')
            if self.contactor_substate == 0x04:
                self.contactor_substate_label.SetValue('Standby')
            if self.contactor_substate == 0x05:
                self.contactor_substate_label.SetValue('Begin Precharge: Close Main Minus')
            if self.contactor_substate == 0x06:
                self.contactor_substate_label.SetValue('Close Precharge')
            if self.contactor_substate == 0x07:
                self.contactor_substate_label.SetValue('Close Main Plus')
            if self.contactor_substate == 0x08:
                self.contactor_substate_label.SetValue('Check Voltage')
            if self.contactor_substate == 0x09:
                self.contactor_substate_label.SetValue('Open Precharge')
            if self.contactor_substate == 0x0A:
                self.contactor_substate_label.SetValue('Error State')
            if self.contactor_substate == 0x0C:
                self.contactor_substate_label.SetValue('Precharge: Close Main Minus')
            if self.contactor_substate == 0x0E:
                self.contactor_substate_label.SetValue('Precharge: Close Main Plus')
            if self.contactor_substate == 0x0F:
                self.contactor_substate_label.SetValue('Precharge: Close Engine')
            if self.contactor_substate == 0x10:
                self.contactor_substate_label.SetValue('Close Main Plus: Step 1')
            if self.contactor_substate == 0x11:
                self.contactor_substate_label.SetValue('Close Main Plus: Step 2')
            if self.contactor_substate == 0x12:
                self.contactor_substate_label.SetValue('Close Main Minus: Step 1')
            if self.contactor_substate == 0x13:
                self.contactor_substate_label.SetValue('Close Main Minus: Step 2')
            if self.contactor_substate == 0x14:
                self.contactor_substate_label.SetValue('Open Main Plus: Step 1')
            if self.contactor_substate == 0x15:
                self.contactor_substate_label.SetValue('Open Main Plus: Step 2')
            if self.contactor_substate == 0x16:
                self.contactor_substate_label.SetValue('Open Main Minus: Step 1')
            if self.contactor_substate == 0x17:
                self.contactor_substate_label.SetValue('Open Main Minus: Step 2')

        # JHL: Flags
        if decoded_can_data[0] == 'error_flags0':
            self.general_error = decoded_can_data[1]
            self.current_state = decoded_can_data[2]
            self.over_temperature_charge = decoded_can_data[3]
            self.under_temperature_charge = decoded_can_data[4]
            self.over_temperature_discharge = decoded_can_data[5]
            self.under_temperature_discharge = decoded_can_data[6]
            self.over_current_charge = decoded_can_data[7]
            self.over_current_discharge = decoded_can_data[8]

            # JHL: display the System state
            if self.current_state == 0x00:
                self.state_label.SetValue('Uninitialized')
            if self.current_state == 0x03:
                self.state_label.SetValue('Idle')
            if self.current_state == 0x04:
                self.state_label.SetValue('Standby')
            if self.current_state == 0x05:
                self.state_label.SetValue('Precharge (Normal)')
            if self.current_state == 0x06:
                self.state_label.SetValue('Normal')
            if self.current_state == 0x07:
                self.state_label.SetValue('Precharge (Charge)')
            if self.current_state == 0x08:
                self.state_label.SetValue('Charge')
            if self.current_state == 0x09:
                self.state_label.SetValue('Precharge (Engine)')
            if self.current_state == 0x0A:
                self.state_label.SetValue('Engine')
            if self.current_state == 0xF0:
                self.state_label.SetValue('Error')

            # JHL: display the General error flag
            if self.general_error == 0:
                self.general_error_label.SetValue('No errors')
            if self.general_error != 0:
                self.general_error_label.SetValue('General error')


        if decoded_can_data[0] == 'error_flags1':
            self.over_voltage = decoded_can_data[1]
            self.under_voltage = decoded_can_data[2]
            self.deep_discharge = decoded_can_data[3]
            self.over_temperature_ic = decoded_can_data[4]
            self.contactor_error = decoded_can_data[5]
            self.selftest_error = decoded_can_data[6]
            self.cantiming_error = decoded_can_data[7]
            self.current_sensor_error = decoded_can_data[8]
            self.balancing_active = decoded_can_data[9]

        if decoded_can_data[0] == 'error_flags2':
            # contactor flags
            self.main_plus = decoded_can_data[1]
            self.main_precharge = decoded_can_data[2]
            self.main_minus = decoded_can_data[3]
            self.main_chargeplus = decoded_can_data[4]
            self.main_chargeprecharge = decoded_can_data[5]
            self.main_chargeminus = decoded_can_data[6]
            self.interlock = decoded_can_data[7]
            # other error flags
            self.insulation_error = decoded_can_data[8]
            self.fuse_state = decoded_can_data[9]
            self.lowcoincellvolt_error = decoded_can_data[10]
            self.openwire_error = decoded_can_data[11]
            self.daisychain_spi_error = decoded_can_data[12]
            self.daisychain_pec_error = decoded_can_data[13]
            self.daisychain_mux_error = decoded_can_data[14]
            self.daisychain_cfg_error = decoded_can_data[15]

            # JHL: update contactor/interlock info
            if self.main_plus == 0:
                self.mainplus_label.SetValue('OPEN')
            if self.main_plus == 1:
                self.mainplus_label.SetValue('CLOSED')
            if self.main_precharge == 0:
                self.mainprecharge_label.SetValue('OPEN')
            if self.main_precharge == 1:
                self.mainprecharge_label.SetValue('CLOSED')
            if self.main_minus == 0:
                self.mainminus_label.SetValue('OPEN')
            if self.main_minus == 1:
                self.mainminus_label.SetValue('CLOSED')
            if self.main_chargeplus == 0:
                self.mainchargeplus_label.SetValue('OPEN')
            if self.main_chargeplus == 1:
                self.mainchargeplus_label.SetValue('CLOSED')

        if decoded_can_data[0] == 'error_flags0' or \
                    decoded_can_data[0] == 'error_flags1' or \
                    decoded_can_data[0] == 'error_flags2':
            clear_list_errors = 0
            if (self.over_temperature_charge !=
                    self.error_list['Over temperature (charge) error']):
                self.error_list['Over temperature (charge) error'] = self.over_temperature_charge
                clear_list_errors = 1
            if (self.under_temperature_charge !=
                    self.error_list['Under temperature (charge) error']):
                self.error_list['Under temperature (charge) error'] = self.under_temperature_charge
                clear_list_errors = 1
            if (self.over_temperature_discharge !=
                    self.error_list['Over temperature (discharge) error']):
                self.error_list['Over temperature (discharge) error'] = self.over_temperature_discharge
                clear_list_errors = 1
            if (self.under_temperature_charge !=
                    self.error_list['Under temperature (discharge) error']):
                self.error_list['Under temperature (discharge) error'] = self.under_temperature_charge
                clear_list_errors = 1
            if (self.over_current_charge !=
                    self.error_list['Over current (charge) error']):
                self.error_list['Over current (charge) error'] = self.over_current_charge
                clear_list_errors = 1
            if (self.over_current_discharge !=
                    self.error_list['Over current (discharge) error']):
                self.error_list['Over current (discharge) error'] = self.over_current_discharge
                clear_list_errors = 1
            if (self.over_voltage !=
                    self.error_list['Over voltage error']):
                self.error_list['Over voltage error'] = self.over_voltage
                clear_list_errors = 1
            if (self.under_voltage !=
                    self.error_list['Under voltage error']):
                self.error_list['Under voltage error'] = self.under_voltage
                clear_list_errors = 1
            if (self.deep_discharge !=
                    self.error_list['Deep discharge flag']):
                self.error_list['Deep discharge flag'] = self.deep_discharge
                clear_list_errors = 1
            if (self.contactor_error !=
                    self.error_list['Contactor error']):
                self.error_list['Contactor error'] = self.contactor_error
                clear_list_errors = 1
            if (self.selftest_error != self.error_list['Selftest error']):
                self.error_list['Selftest error'] = self.selftest_error
                clear_list_errors = 1
            if (self.cantiming_error !=
                    self.error_list['CAN timing error']):
                self.error_list['CAN timing error'] = self.cantiming_error
                clear_list_errors = 1
            if (self.current_sensor_error !=
                    self.error_list['Current sensor error']):
                self.error_list['Current sensor error'] = self.current_sensor_error
                clear_list_errors = 1
            if (self.balancing_active !=
                    self.error_list['Balancing active']):
                self.error_list['Balancing active'] = self.balancing_active
                clear_list_errors = 1
            if (self.insulation_error !=
                    self.error_list['Insulation error']):
                self.error_list['Insulation error'] = self.insulation_error
                clear_list_errors = 1
            if (self.lowcoincellvolt_error !=
                    self.error_list['Low coin cell voltage error']):
                self.error_list['Low coin cell voltage error'] = self.lowcoincellvolt_error
                clear_list_errors = 1
            if (self.openwire_error != self.error_list['Open wire error']):
                self.error_list['Open wire error'] = self.openwire_error
                clear_list_errors = 1

            if (self.daisychain_spi_error != self.error_list['Daisychain SPI error']):
                self.error_list['Daisychain SPI error'] = self.daisychain_spi_error
                clear_list_errors = 1

            if (self.daisychain_pec_error != self.error_list['Daisychain PEC error']):
                self.error_list['Daisychain PEC error'] = self.daisychain_pec_error
                clear_list_errors = 1

            if (self.daisychain_mux_error != self.error_list['Daisychain Mux error']):
                self.error_list['Daisychain Mux error'] = self.daisychain_mux_error
                clear_list_errors = 1

            if (self.daisychain_cfg_error != self.error_list['Daisychain CFG error']):
                self.error_list['Daisychain CFG error'] = self.daisychain_cfg_error
                clear_list_errors = 1

            if clear_list_errors == 1:
                self.listErrors.Clear()

                if self.over_temperature_charge != 0:
                    self.listErrors.Append('Over temperature (charge) error')
                if self.under_temperature_charge != 0:
                    self.listErrors.Append('Under temperature (charge) error')
                if self.over_temperature_discharge != 0:
                    self.listErrors.Append('Over temperature (discharge) error')
                if self.under_temperature_charge != 0:
                    self.listErrors.Append('Under temperature (discharge) error')
                if self.over_current_charge != 0:
                    self.listErrors.Append('Over current (charge) error')
                if self.over_current_discharge != 0:
                    self.listErrors.Append('Over current (discharge) error')
                if self.over_voltage != 0:
                    self.listErrors.Append('Over voltage error')
                if self.under_voltage != 0:
                    self.listErrors.Append('Under voltage error')
                if self.deep_discharge != 0:
                    self.listErrors.Append('Deep discharge flag')
                if self.contactor_error != 0:
                    self.listErrors.Append('Contactor error')
                if self.selftest_error != 0:
                    self.listErrors.Append('Selftest error')
                if self.cantiming_error != 0:
                    self.listErrors.Append('CAN timing error')
                if self.current_sensor_error != 0:
                    self.listErrors.Append('Current sensor error')
                if self.balancing_active != 0:
                    self.listErrors.Append('Balancing active')
                if self.insulation_error != 0:
                    self.listErrors.Append('Insulation error')
                if self.lowcoincellvolt_error != 0:
                    self.listErrors.Append('Low coin cell voltage error')
                if self.openwire_error != 0:
                    self.listErrors.Append('Open wire error')
                if self.daisychain_spi_error != 0:
                    self.listErrors.Append('Daisychain: SPI error')
                if self.daisychain_pec_error != 0:
                    self.listErrors.Append('Daisychain: LTC PEC error')
                if self.daisychain_mux_error != 0:
                    self.listErrors.Append('Daisychain: Multiplexor error')
                if self.daisychain_cfg_error != 0:
                    self.listErrors.Append('Daisychain: LTC Configuration error')



        # JHL: the Module tabs
        if decoded_can_data[0] == 'voltage':
            module_number = int(decoded_can_data[1])
            voltage_bank = decoded_can_data[2]      # JHL: need to know the data
            voltage = [0] * 3
            for i in range(len(voltage)):
                voltage[i] = decoded_can_data[3 + i]
                if ((module_number) < len(self.panel_list) - 1) and \
                        ((3 * voltage_bank + i) < len(self.panel_list[1 + \
                                module_number + 1].voltage_label_list)) and \
                        (voltage[i] != 'NONE'):
                    self.panel_list[1 + module_number + 1].voltage_label_list[\
                            3 * voltage_bank + i].SetValue(str(voltage[i]) + _mV)
                    cell_voltages[module_number][3 * voltage_bank + i] = voltage[i]

        # JHL: need to change the structure of the if's to the same way as above
        if decoded_can_data[0] == 'temperature':
            module_number = int(decoded_can_data[1])
            temperature_bank = decoded_can_data[2]
            temperature = [0] * 3
            for i in range(len(temperature)):
                temperature[i] = decoded_can_data[3 + i]
                if (module_number) < len(self.panel_list) - 1:
                    if (3 * temperature_bank + i) < len(
                            self.panel_list[1 + module_number + 1].temperature_label_list):
                        if temperature[i] != 'NONE':
                            self.panel_list[1 + module_number + 1].temperature_label_list[3 *
                                    temperature_bank + i].SetValue(str(temperature[i]) + _deg)
                            cell_temps[module_number][3 * temperature_bank + i] = temperature[i]

        # JHL: where are temperature_one and _two displayed?
        if decoded_can_data[0] == 'temperature_one':
            module_number = decoded_can_data[1]
            temperature = [0] * 8
            for i in range(len(temperature)):
                temperature[i] = decoded_can_data[2 + i]
                if (module_number) < len(self.panel_list) - 1:
                    if i < len(
                            self.panel_list[1 + module_number + 1].temperature_label_list):
                        self.panel_list[1 + module_number + 1].temperature_label_list[i].SetValue(
                            str(temperature[i]) + ' ' + u'\u2103')

        if decoded_can_data[0] == 'temperature_two':
            module_number = decoded_can_data[1]
            temperature = [0] * 7
            for i in range(len(temperature)):
                temperature[i] = decoded_can_data[2 + i]
                if (module_number) < len(self.panel_list) - 1:
                    if (i + 8) < len(self.panel_list[1 +
                                                     module_number + 1].temperature_label_list):
                        self.panel_list[1 + module_number + 1].temperature_label_list[i + 8].SetValue(
                            str(temperature[i]) + ' ' + u'\u2103')

    def decode_can_data(self, can_data, number_of_modules,
                        number_of_cells, number_of_temperatures):
        global CAN_MATRIX       # JHL: not used?

        if parameters.logdata is True:
            if os.path.getsize(self.full_logfilename) / 1000000 > logfile_size_limit:
                self.logfile.close()
                self.logfile = open(self.get_logfile_name(), "w")

        # CAN matrix foxBMS
        # Read and decode voltage values
        # LMM : modified code to read the eighth byte in the message as the module number
        if (can_data.id>= 0x200) and (can_data.id < 0x206):
           module_number = can_data.data[7]
           voltage_bank = can_data.id - 0x200 

           if 0 <= module_number and module_number < number_of_modules:
                voltage = [0] * 3
                if (3 * voltage_bank + 0) < number_of_cells:
                    voltage[0] = decode_CAN_data(can_data.data, 8, 16, 1, 0,
                            can_data.dlc, "LITTLE", "UNSIGNED")
                else:
                    voltage[0] = 'NONE'
                if (3 * voltage_bank + 1) < number_of_cells:
                    voltage[1] = decode_CAN_data(can_data.data, 24, 16, 1, 0,
                            can_data.dlc, "LITTLE", "UNSIGNED")
                else:
                    voltage[1] = 'NONE'
                if (3 * voltage_bank + 2) < number_of_cells:
                    voltage[2] = decode_CAN_data(can_data.data, 40, 16, 1, 0,
                            can_data.dlc, "LITTLE", "UNSIGNED")
                else:
                    voltage[2] = 'NONE'

                decoded_can_data = ['voltage'] + [module_number] + [voltage_bank]
                for i in range(len(voltage)):
                    decoded_can_data.append(voltage[i])

                return decoded_can_data

        # Read and decode temperature values
        if can_data.id == 0x210:
            module_number = can_data.data[7]
            temperature_bank = 0

            if 0 <= module_number and module_number < number_of_modules:
                temperature = [0] * 3
                if (3 * temperature_bank + 0) < number_of_temperatures:
                    temperature[0] = decode_CAN_data(can_data.data,
                            8, 16, 100, 128, can_data.dlc, "LITTLE", "SIGNED")
                else:
                    temperature[0] = 'NONE'
                if (3 * temperature_bank + 1) < number_of_temperatures:
                    temperature[1] = decode_CAN_data(can_data.data,
                            24, 16, 100, 128, can_data.dlc, "LITTLE", "SIGNED")
                else:
                    temperature[1] = 'NONE'
                if (3 * temperature_bank + 2) < number_of_temperatures:
                    temperature[2] = decode_CAN_data(can_data.data,
                            40, 16, 100, 128, can_data.dlc, "LITTLE", "SIGNED")
                else:
                    temperature[2] = 'NONE'

                decoded_can_data = ['temperature'] + [module_number] + [temperature_bank]
                for i in range(len(temperature)):
                    decoded_can_data.append(temperature[i])

                return decoded_can_data

        # min-max SOC
        if (can_data.id == 0x140):
            mean_soc = decode_CAN_data(can_data.data, 0, 16, 100, 0,
                    can_data.dlc, "LITTLE", "UNSIGNED")
            min_soc = decode_CAN_data(can_data.data, 16, 16, 100, 0,
                    can_data.dlc, "LITTLE", "UNSIGNED")
            max_soc = decode_CAN_data(can_data.data, 32, 16, 100, 0,
                    can_data.dlc, "LITTLE", "UNSIGNED")
            decoded_can_data = ['socminmax'] + [mean_soc] + [min_soc] + [max_soc]
            return decoded_can_data

        # min-max voltage
        if (can_data.id == 0x170):
            mean_voltage = decode_CAN_data(
                can_data.data, 0, 16, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED")
            min_voltage = decode_CAN_data(
                can_data.data, 16, 16, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED")
            max_voltage = decode_CAN_data(
                can_data.data, 32, 16, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED")
            min_voltage_module = int(decode_CAN_data(
                can_data.data, 48, 8, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            max_voltage_module = int(decode_CAN_data(
                can_data.data, 56, 8, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            decoded_can_data = ['voltageminmax'] + [mean_voltage] + [min_voltage] + \
                [max_voltage] + [min_voltage_module] + [max_voltage_module]
            return decoded_can_data


        # min-max temperature
        if (can_data.id == 0x180):
            mean_temperature = decode_CAN_data(
                can_data.data, 0, 16, 100, 128, can_data.dlc, "LITTLE", "SIGNED")
            min_temperature = decode_CAN_data(
                can_data.data, 16, 16, 100, 128, can_data.dlc, "LITTLE", "SIGNED")
            max_temperature = decode_CAN_data(
                can_data.data, 32, 16, 100, 128, can_data.dlc, "LITTLE", "SIGNED")
            min_temperature_module = int(decode_CAN_data(
                can_data.data, 48, 8, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            max_temperature_module = int(decode_CAN_data(
                can_data.data, 56, 8, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            decoded_can_data = ['temperatureminmax'] + [mean_temperature] + \
                    [min_temperature] + [max_temperature] + \
                    [min_temperature_module] + [max_temperature_module]
            return decoded_can_data

        # error flags
        if (can_data.id == 0x110):
            general_error = int(decode_CAN_data(
                can_data.data, 0, 3, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            current_state = int(decode_CAN_data(
                can_data.data, 8, 8, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            over_temperature_charge = int(decode_CAN_data(
                can_data.data, 16, 3, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            under_temperature_charge = int(decode_CAN_data(
                can_data.data, 24, 3, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            over_temperature_discharge = int(decode_CAN_data(
                can_data.data, 32, 3, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            under_temperature_discharge = int(decode_CAN_data(
                can_data.data, 40, 3, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            over_current_charge = int(decode_CAN_data(
                can_data.data, 48, 3, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            over_current_discharge = int(decode_CAN_data(
                can_data.data, 56, 3, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))

            decoded_can_data = ['error_flags0'] + [general_error] + \
                    [current_state] + [over_temperature_charge] + \
                    [under_temperature_charge] + [over_temperature_discharge] + \
                    [under_temperature_discharge] + [over_current_charge] + \
                    [over_current_discharge]
            return decoded_can_data

        if (can_data.id == 0x111):
            over_voltage = int(decode_CAN_data(
                can_data.data, 0, 3, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            under_voltage = int(decode_CAN_data(
                can_data.data, 8, 3, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            deep_discharge = int(decode_CAN_data(
                can_data.data, 11, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            over_temperature_ic = int(decode_CAN_data(
                can_data.data, 16, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            contactor_error = int(decode_CAN_data(
                can_data.data, 24, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            selftest_error = int(decode_CAN_data(
                can_data.data, 32, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            cantiming_error = int(decode_CAN_data(
                can_data.data, 40, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            current_sensor_error = int(decode_CAN_data(
                can_data.data, 48, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            balancing_active = int(decode_CAN_data(
                can_data.data, 56, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))

            decoded_can_data = ['error_flags1'] + [over_voltage] + \
                    [under_voltage] + [deep_discharge] + [over_temperature_ic] + \
                    [contactor_error] + [selftest_error] + \
                    [cantiming_error] + [current_sensor_error] + \
                    [balancing_active]
            return decoded_can_data

        if (can_data.id == 0x112):
            # contactors
            main_plus = int(decode_CAN_data(
                can_data.data, 0, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            main_precharge = int(decode_CAN_data(
                can_data.data, 1, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            main_minus = int(decode_CAN_data(
                can_data.data, 2, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            main_chargeplus = int(decode_CAN_data(
                can_data.data, 3, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            main_chargeprecharge = int(decode_CAN_data(
                can_data.data, 4, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            main_chargeminus = int(decode_CAN_data(
                can_data.data, 5, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            interlock = int(decode_CAN_data(
                can_data.data, 9, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))

            decoded_can_data = ['error_flags2'] + [main_plus] + \
                    [main_precharge] + [main_minus] + [main_chargeplus] + \
                    [main_chargeprecharge] + [main_chargeminus] + [interlock]

            # other error flags
            insulation_error = int(decode_CAN_data(
                can_data.data, 16, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            fuse_state = int(decode_CAN_data(
                can_data.data, 24, 4, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            lowcoincellvolt_error = int(decode_CAN_data(
                can_data.data, 32, 2, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            openwire_error = int(decode_CAN_data(
                can_data.data, 40, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            daisychain_spi_error = int(decode_CAN_data(
                can_data.data, 48, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            daisychain_pec_error = int(decode_CAN_data(
                can_data.data, 49, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            daisychain_mux_error = int(decode_CAN_data(
                can_data.data, 50, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))
            daisychain_cfg_error = int(decode_CAN_data(
                can_data.data, 51, 1, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))

            decoded_can_data += [insulation_error] + [fuse_state] + \
                    [lowcoincellvolt_error] + [openwire_error] + \
                    [daisychain_spi_error] + [daisychain_pec_error] + \
                    [daisychain_mux_error] + [daisychain_cfg_error]
            return decoded_can_data
        # End CAN matrix foxBMS

        # Current Contactor State
        if can_data.id == 0x113:
            # Contactor state is in the first byte, length 8 bits
            contactor_state = int(decode_CAN_data(
                can_data.data, 0, 8, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))

            contactor_substate = int(decode_CAN_data(
                can_data.data, 8, 8, 1, 0, can_data.dlc, "LITTLE", "UNSIGNED"))

            decoded_can_data = ['contactor_state'] + [contactor_state] + [contactor_substate]
            return decoded_can_data


        # CAN matrix current sensor
        # current
        if can_data.id == 0x35C or can_data.id == 0x521:
            current = decode_CAN_data(
                can_data.data, 0, 32, 1000, 0, can_data.dlc, "BIG", "SIGNED")
            decoded_can_data = ['current'] + [current]
            return decoded_can_data

        # current sensor voltage1
        if can_data.id == 0x35D or can_data.id == 0x522:
            current_V1 = decode_CAN_data(
                can_data.data, 0, 32, 1000, 0, can_data.dlc, "BIG", "SIGNED")
            decoded_can_data = ['current_V1'] + [current_V1]
            return decoded_can_data

        # current sensor voltage2
        if can_data.id == 0x35E or can_data.id == 0x523:
            current_V2 = decode_CAN_data(
                can_data.data, 0, 32, 1000, 0, can_data.dlc, "BIG", "SIGNED")
            decoded_can_data = ['current_V2'] + [current_V2]
            return decoded_can_data

        # current sensor voltage3
        if can_data.id == 0x35F or can_data.id == 0x524:
            current_V3 = decode_CAN_data(
                can_data.data, 0, 32, 1000, 0, can_data.dlc, "BIG", "SIGNED")
            decoded_can_data = ['current_V3'] + [current_V3]
            return decoded_can_data
        # CAN matrix current sensor

        return 0


def receive_send_can_adapter(parameters, queue_can_action, queue_out, queue_send):
    action = 'WAIT'

    if parameters.adapter_type == 'PCAN':
        adapter, bus = Initialize_with_HWID(parameters.id,
                PCAN_baudratelist[parameters.baudrate])

    can_message_01 = can_message()

    while True:
        time.sleep(0.001)
        if not queue_can_action.empty():
            action = queue_can_action.get()

        if action == 'EXIT':
            if parameters.adapter_type == 'PCAN':
                adapter.Uninitialize(bus)
            break
        if action == 'WAIT':
            if parameters.adapter_type == 'PCAN':
                if not queue_send.empty():
                    can_message_01 = queue_send.get()
                    message = PCANBasic.TPCANMsg()
                    message.ID = can_message_01.id
                    message.LEN = can_message_01.dlc
                    for i in range(len(can_message_01.data)):
                        message.DATA[i] = can_message_01.data[i]
                    adapter.Write(bus, message)
                adapter.Read(bus)
        if action == 'RUN':
            if parameters.adapter_type == 'PCAN':
                if not queue_send.empty():
                    can_message_01 = queue_send.get()
                    message = PCANBasic.TPCANMsg()
                    message.ID = can_message_01.id
                    message.LEN = can_message_01.dlc
                    for i in range(len(can_message_01.data)):
                        message.DATA[i] = can_message_01.data[i]
                    adapter.Write(bus, message)
                message = adapter.Read(bus)
                can_message_01 = can_message()
                if message[0] == PCANBasic.PCAN_ERROR_OK:
                    can_message_01.id = message[1].ID
                    can_message_01.dlc = message[1].LEN
                    for i in range(len(message[1].DATA)):
                        can_message_01.data[i] = message[1].DATA[i]
                    queue_out.put(can_message_01)

    return 0



if __name__ == '__main__':

    app = AdapterSelectionApp()
    parameters = app.getFrameOutput()   # JHL: parameters for CAN adapter.
    if parameters.adapter == 'NONE':
        sys.exit()

    queue_can_action = Queue(maxsize=0)
    queue_can_receive = Queue(maxsize=0)
    queue_can_send = Queue(maxsize=0)

    receive_thread = Process(target=receive_send_can_adapter,
            args=(parameters, queue_can_action, queue_can_receive, queue_can_send,))
    receive_thread.daemon = True
    receive_thread.start()

    main_app = wx.App(False)  # creates the application
    frame = MainFrame(parent=None, id=-1, parameters=parameters,
            queue_can_action=queue_can_action,
            queue_can_receive=queue_can_receive, queue_can_send=queue_can_send)
    frame.Show()  # show the window
    main_app.MainLoop()

    receive_thread.join()
