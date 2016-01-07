EESchema Schematic File Version 2
LIBS:power
LIBS:device
LIBS:transistors
LIBS:conn
LIBS:linear
LIBS:regul
LIBS:74xx
LIBS:cmos4000
LIBS:adc-dac
LIBS:memory
LIBS:xilinx
LIBS:microcontrollers
LIBS:dsp
LIBS:microchip
LIBS:analog_switches
LIBS:motorola
LIBS:texas
LIBS:intel
LIBS:audio
LIBS:interface
LIBS:digital-audio
LIBS:philips
LIBS:display
LIBS:cypress
LIBS:siliconi
LIBS:opto
LIBS:atmel
LIBS:contrib
LIBS:valves
LIBS:MoonStalker
LIBS:MoonStalker-cache
EELAYER 25 0
EELAYER END
$Descr A4 11693 8268
encoding utf-8
Sheet 1 1
Title "MoonStalker Drive Unit"
Date "2016-01-08"
Rev "0.1"
Comp "Koza d.o.o."
Comment1 "It is very nice"
Comment2 ""
Comment3 ""
Comment4 ""
$EndDescr
$Comp
L Arduino_Micro U5
U 1 1 568D949D
P 7150 4050
F 0 "U5" H 7150 3100 60  0000 C CNN
F 1 "Arduino_Micro" H 7150 5100 60  0000 C CNN
F 2 "" H 7200 3800 60  0000 C CNN
F 3 "" H 7200 3800 60  0000 C CNN
	1    7150 4050
	1    0    0    -1  
$EndComp
$Comp
L Pololu_DRV8825 U1
U 1 1 568D968D
P 2250 1900
F 0 "U1" H 2250 1400 60  0000 C CNN
F 1 "Pololu_DRV8825" H 2250 2400 60  0000 C CNN
F 2 "" H 2350 1600 60  0000 C CNN
F 3 "" H 2350 1600 60  0000 C CNN
	1    2250 1900
	1    0    0    -1  
$EndComp
$Comp
L Pololu_DRV8825 U2
U 1 1 568D96DA
P 2250 3600
F 0 "U2" H 2250 3100 60  0000 C CNN
F 1 "Pololu_DRV8825" H 2250 4100 60  0000 C CNN
F 2 "" H 2350 3300 60  0000 C CNN
F 3 "" H 2350 3300 60  0000 C CNN
	1    2250 3600
	1    0    0    -1  
$EndComp
$Comp
L HC-05_Bluetooth_Radio U4
U 1 1 568EE7E0
P 7150 1350
F 0 "U4" H 7600 1300 60  0000 C CNN
F 1 "HC-05_Bluetooth_Radio" H 7200 1700 60  0000 C CNN
F 2 "" H 7250 1200 60  0000 C CNN
F 3 "" H 7250 1200 60  0000 C CNN
	1    7150 1350
	1    0    0    -1  
$EndComp
$Comp
L TEL3-1211 U3
U 1 1 568EF545
P 2350 6700
F 0 "U3" H 2350 6100 60  0000 C CNN
F 1 "TEL3-1211" H 2350 7250 60  0000 C CNN
F 2 "" H 1950 6650 60  0000 C CNN
F 3 "" H 1950 6650 60  0000 C CNN
	1    2350 6700
	1    0    0    -1  
$EndComp
Wire Wire Line
	7200 1900 7200 2550
Wire Wire Line
	7200 2550 6100 2550
Wire Wire Line
	6100 2550 6100 3500
Wire Wire Line
	6100 3500 6450 3500
Wire Wire Line
	7300 1900 7300 2650
Wire Wire Line
	7300 2650 6200 2650
Wire Wire Line
	6200 2650 6200 3400
Wire Wire Line
	6200 3400 6450 3400
$Comp
L VCC #PWR01
U 1 1 568EF868
P 8300 3400
F 0 "#PWR01" H 8300 3250 50  0001 C CNN
F 1 "VCC" H 8300 3550 50  0000 C CNN
F 2 "" H 8300 3400 50  0000 C CNN
F 3 "" H 8300 3400 50  0000 C CNN
	1    8300 3400
	1    0    0    -1  
$EndComp
Wire Wire Line
	8300 3400 7850 3400
$Comp
L VCC #PWR02
U 1 1 568EF93E
P 6700 2150
F 0 "#PWR02" H 6700 2000 50  0001 C CNN
F 1 "VCC" H 6700 2300 50  0000 C CNN
F 2 "" H 6700 2150 50  0000 C CNN
F 3 "" H 6700 2150 50  0000 C CNN
	1    6700 2150
	1    0    0    -1  
$EndComp
$Comp
L GND #PWR03
U 1 1 568EF95C
P 7100 2200
F 0 "#PWR03" H 7100 1950 50  0001 C CNN
F 1 "GND" H 7100 2050 50  0000 C CNN
F 2 "" H 7100 2200 50  0000 C CNN
F 3 "" H 7100 2200 50  0000 C CNN
	1    7100 2200
	1    0    0    -1  
$EndComp
Wire Wire Line
	7100 1900 7100 2200
Wire Wire Line
	6700 2150 7000 2150
Wire Wire Line
	7000 2150 7000 1900
$EndSCHEMATC
