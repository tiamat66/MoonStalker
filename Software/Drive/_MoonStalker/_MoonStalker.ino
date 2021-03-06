/* MoonStalker

   This SW controls the MoonStalker drive unit.
*/

/*
   Variable definitions
*/
#include "StepperController.h"

// constants

const int BATTERY_LOW_LIMIT_MV = 10000;

// battery input pin
const int battery_voltage_pin = A0;

// bluetooth serial input and output
const int bluetooth_tx_pin = 1;
const int bluetooth_rx_pin = 0;

// output pins
const int horiz_step_pin = 2;
const int horiz_direction_pin = 3;
const int horiz_reset_pin = 7;
const int horiz_sleep_pin = 8;

const int vert_step_pin = 5;
const int vert_direction_pin = 6;
const int vert_reset_pin = 9;
const int vert_sleep_pin = 10;

// input pins
const int horiz_fault_pin = 11;
const int vert_fault_pin = 12;

// global variables
StepperController stepper_controller = StepperController(200);

void setup()
{
  /* Open bluetooth serial port */
  Serial1.begin(115200);
  while (!Serial1)
  {
    ; // Wait for serial port to connect
  }
  Serial1.println("<INFO System start>");
  
  initialize_pins();
}

void loop()
{
  static char command_buffer[65] = "";
  static int num_recv_char = 0;
  char incoming_char = 0;

  if (Serial1.available() > 0)
  {
    if (num_recv_char == 64)
    {
      Serial1.println(F("<FATAL_ERROR RCV_BUFFER_OVERFLOW>"));
    }
    incoming_char = Serial1.read();
    // ignore newline characters
    if ((incoming_char != 10) && (incoming_char != 13))
    {
      command_buffer[num_recv_char] = incoming_char;
      command_buffer[num_recv_char + 1] = 0;
      num_recv_char++;
      if (incoming_char == '>')
      {
        Serial1.print("<INFO Handling command: ");
        Serial1.print(command_buffer);
        Serial1.println(">");
        handle_incoming_command(command_buffer);

        // clear command buffer, NULL terminate
        *command_buffer = 0;
        num_recv_char = 0;
      }
    }
  }
}

int get_battery_voltage()
{
  uint16_t value = 0;
  uint32_t voltage_mv = 0;

  value = analogRead(battery_voltage_pin);
  // Convert read 10 bit value to 0-5000 mV and
  // multiply with the voltage divider 5088R and 14k93
  // voltage_mv = value * (5000 * ((5088 + 14930)/5088)/ 1023;
  voltage_mv = value * 19229 / 1000;

  return voltage_mv;
}


/* handle_incoming_command

   handle command tag that arrived
   over seial

   commands:
   <MV a b>
   <BTRY?>
   <ST?>
   <SYS_CHK>
   <DEBUG>
   <STOP>

*/
void handle_incoming_command(char *command_buff)
{
  char command[65];
  char *cmd;

  // extract command without starting '<'
  // and ending '>'
  strcpy(command, command_buff + 1);
  command[strlen(command) - 1] = 0;

  cmd = strtok(command, " ");
  if (!strcmp(cmd, "MV"))
  {
    char *horiz_steps_str;
    char *vert_steps_str;
    char *rpm_speed_str;
    int horiz_steps, vert_steps, rpm_speed;
    StepperDirection horiz_direction;
    StepperDirection vert_direction;

    
    if (stepper_controller.get_running_mode() != RunningMode::IDLE_MODE)
    {
      Serial1.println("<NOT_RDY>");
      return;
    }
    horiz_steps_str = strtok(NULL, " ");
    vert_steps_str = strtok(NULL, " ");
    rpm_speed_str = strtok(NULL, " ");

    horiz_steps = atoi(horiz_steps_str);
    vert_steps = atoi(vert_steps_str);
    rpm_speed = atoi(rpm_speed_str);
    Serial1.print("<MV_ACK ");
    Serial1.print(horiz_steps);
    Serial1.print(" ");
    Serial1.print(vert_steps);
    Serial1.print(" ");
    Serial1.print(rpm_speed);
    Serial1.println(">");

    if (horiz_steps < 0)
    {
      horiz_steps = -horiz_steps;
      horiz_direction = StepperDirection::CCW;
    }
    else
    {
      horiz_direction = StepperDirection::CW;
    }
    if (vert_steps < 0)
    {
      vert_steps = -vert_steps;
      vert_direction = StepperDirection::CCW;
    }
    else
    {
      vert_direction = StepperDirection::CW;
    }
    stepper_controller.move_steppers(rpm_speed,
                                     horiz_direction,
                                     horiz_steps,
                                     rpm_speed,
                                     vert_direction,
                                     vert_steps);                                    
  }
  else if (!strcmp(cmd, "MVS"))
  {
    char *direction_str;
    char *rpm_speed_str;
    int   rpm_speed;

    // We can't start free run move if we are
    // in move mode. Previous free run mode can
    // be modified with new parameters.
    if (stepper_controller.get_running_mode() == RunningMode::MOVE_MODE)
    {
      Serial1.println("<NOT_RDY>");
      return;
    }
    
    direction_str = strtok(NULL, " ");
    rpm_speed_str = strtok(NULL, " ");

    rpm_speed = atoi(rpm_speed_str);
    if (!strcmp(direction_str, "N"))
    {
      stepper_controller.free_run_start(0, StepperDirection::IGNORE, rpm_speed, StepperDirection::CW);
    }
    else if (!strcmp(direction_str, "S"))
    {
      stepper_controller.free_run_start(0, StepperDirection::IGNORE, rpm_speed, StepperDirection::CCW);
    }
    else if (!strcmp(direction_str, "W"))
    {
      stepper_controller.free_run_start(rpm_speed, StepperDirection::CW, 0, StepperDirection::IGNORE);
    }
    else if (!strcmp(direction_str, "E"))
    {
      stepper_controller.free_run_start(rpm_speed, StepperDirection::CCW, 0, StepperDirection::IGNORE);
    }
    else if (!strcmp(direction_str, "NW"))
    {
      stepper_controller.free_run_start(rpm_speed, StepperDirection::CW, rpm_speed, StepperDirection::CW);
    }
    else if (!strcmp(direction_str, "NE"))
    {
      stepper_controller.free_run_start(rpm_speed, StepperDirection::CCW, rpm_speed, StepperDirection::CW);
    }
    else if (!strcmp(direction_str, "SW"))
    {
      stepper_controller.free_run_start(rpm_speed, StepperDirection::CW, rpm_speed, StepperDirection::CCW);
    }
    else if (!strcmp(direction_str, "SE"))
    {
      stepper_controller.free_run_start(rpm_speed, StepperDirection::CCW, rpm_speed, StepperDirection::CCW);
    }
    else
    {
      Serial1.println("<ERROR UNKNOWN_DIRECTION>");
      return;
    }   
    Serial1.print("<MVS_ACK ");
    Serial1.print(direction_str);
    Serial1.print(" ");
    Serial1.print(rpm_speed);
    Serial1.println(">");
  }
  else if (!strcmp(cmd, "MVE"))
  {
    if (stepper_controller.get_running_mode() != RunningMode::FREE_RUN_MODE)
    {
      Serial1.println("<NOT_RDY>");
      return;
    }
    else
    {
      stepper_controller.free_run_stop();
      Serial1.println("<MVE_ACK");
    }
  }
  else if (!strcmp(cmd, "BTRY?"))
  {
    int volt_mv = get_battery_voltage();
    Serial1.print("<BTRY ");
    Serial1.print(volt_mv);
    Serial1.println(">");
  }
  else if (!strcmp(cmd, "MVST?"))
  {
    char ret_msg[32];

    if (1) // TODO - Do the coorect thing here
    {
      strcpy(ret_msg, "<NOT_RDY>");
    }
    else
    {
      strcpy(ret_msg, "<RDY>");
    }
    Serial1.println(ret_msg);
  }
  else if (!strcmp(cmd, "SYS_CHK"))
  {
    system_check();
    Serial1.println("Would execute system check");
  }
  else if (!strcmp(cmd, "DEBUG"))
  {
    uint16_t x;
    uint16_t y;
    
    Serial1.print("<MV_REMAIN X: ");
    Serial1.print(x);
    Serial1.print(" Y: ");
    Serial1.print(y);
    Serial1.println(">");
  }
  else if (!strcmp(cmd, "STOP"))
  {
    noInterrupts();
    // TODO - stop the movement
    interrupts();
    Serial1.println("<STOP_ACK>");
  }
}

// Check for any error conditions
void system_check()
{
  int battery_volt_mv;
  int horiz_fault;
  int vert_fault;

  battery_volt_mv = get_battery_voltage();

  if (battery_volt_mv < BATTERY_LOW_LIMIT_MV)
  {
    Serial1.println("<ALARM_LOW_BTRY>");
  }

  // check drv fault pins
  horiz_fault = digitalRead(horiz_fault_pin);
  vert_fault = digitalRead(vert_fault_pin);

  if (horiz_fault == LOW)
  {
    Serial1.println("<ALARM_DRV_HORIZ_FAULT>");
  }

  if (vert_fault == LOW)
  {
    Serial1.println("<ALARM_DRV_VERT_FAULT>");
  }
}

void initialize_pins()
{
  // output pins horizontal
  pinMode(horiz_step_pin, OUTPUT);
  pinMode(horiz_direction_pin, OUTPUT);
  pinMode(horiz_reset_pin, OUTPUT);
  pinMode(horiz_sleep_pin, OUTPUT);

  // output pins vertical
  pinMode(vert_step_pin, OUTPUT);
  pinMode(vert_direction_pin, OUTPUT);
  pinMode(vert_reset_pin, OUTPUT);
  pinMode(vert_sleep_pin, OUTPUT);

  // input fault pins
  pinMode(horiz_fault_pin, INPUT);
  pinMode(vert_fault_pin, INPUT);

  pinMode(battery_voltage_pin, INPUT);

  // set sleep pins
  digitalWrite(horiz_sleep_pin, HIGH);
  digitalWrite(vert_sleep_pin, HIGH);

  // set reset pins
  digitalWrite(horiz_reset_pin, HIGH);
  digitalWrite(vert_reset_pin, HIGH);
}
