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

// limit switch pins (normally open to GND, internal pull-up enabled)
// Direction mapping:
//   HORIZ_WEST  (A1) – blocks horizontal CW movement (telescope moving west / azimuth decreasing)
//   HORIZ_EAST  (A2) – blocks horizontal CCW movement (telescope moving east / azimuth increasing)
//   VERT_NORTH  (A3) – blocks vertical CW movement (telescope moving north / altitude increasing)
//   VERT_SOUTH  (A4) – blocks vertical CCW movement (telescope moving south / altitude decreasing)
const uint8_t LIMIT_HORIZ_WEST  = A1;
const uint8_t LIMIT_HORIZ_EAST  = A2;
const uint8_t LIMIT_VERT_NORTH  = A3;
const uint8_t LIMIT_VERT_SOUTH  = A4;

// global variables
StepperController stepper_controller = StepperController(200);

void setup()
{
    /* Open USB serial port */
  Serial.begin(115200);
  Serial.println("<INFO System start>");

  initialize_pins();

  // Initialize hardware timers for stepper pulse generation
  stepper_controller.initialize_timer1();
  stepper_controller.initialize_timer3();
}

void loop()
{
  static char command_buffer[65] = "";
  static int num_recv_char = 0;
  char incoming_char = 0;

  if (Serial.available() > 0)
  {
    if (num_recv_char == 64)
    {
      Serial.println(F("<FATAL_ERROR RCV_BUFFER_OVERFLOW>"));
    }
    incoming_char = Serial.read();
    // ignore newline characters
    if ((incoming_char != 10) && (incoming_char != 13))
    {
      command_buffer[num_recv_char] = incoming_char;
      command_buffer[num_recv_char + 1] = 0;
      num_recv_char++;
      if (incoming_char == '>')
      {
        Serial.print("<INFO Handling command: ");
        Serial.print(command_buffer);
        Serial.println(">");
        handle_incoming_command(command_buffer);

        // clear command buffer, NULL terminate
        *command_buffer = 0;
        num_recv_char = 0;
      }
    }
  }

    // Poll limit switches every loop iteration
  check_limit_switches();

  // Check if free-run ramp-down has completed
  stepper_controller.check_free_run_complete();
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
      Serial.println("<NOT_RDY>");
      return;
    }
    horiz_steps_str = strtok(NULL, " ");
    vert_steps_str = strtok(NULL, " ");
    rpm_speed_str = strtok(NULL, " ");

    horiz_steps = atoi(horiz_steps_str);
    vert_steps = atoi(vert_steps_str);
    rpm_speed = atoi(rpm_speed_str);
    Serial.print("<MV_ACK ");
    Serial.print(horiz_steps);
    Serial.print(" ");
    Serial.print(vert_steps);
    Serial.print(" ");
    Serial.print(rpm_speed);
    Serial.println(">");

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

    // Check if movement is blocked by an active limit switch before starting the move.
    // Direction-to-limit mapping:
    //   Horizontal CW  (west = CCW for StepperController, since we defined CW as increasing azimuth)
    //     → blocked if LIMIT_HORIZ_WEST  is pressed (pin LOW)
    //   Horizontal CCW (east = CW for StepperController)
    //     → blocked if LIMIT_HORIZ_EAST  is pressed (pin LOW)
    //   Vertical CW    (north = altitude increasing)
    //     → blocked if LIMIT_VERT_NORTH  is pressed (pin LOW)
    //   Vertical CCW   (south = altitude decreasing)
    //     → blocked if LIMIT_VERT_SOUTH  is pressed (pin LOW)
    // If any axis would move toward a pressed limit, the entire command is rejected
    // before any movement starts.
    bool limit_blocked = false;
    if (horiz_steps > 0)
    {
      if (horiz_direction == StepperDirection::CW && digitalRead(LIMIT_HORIZ_WEST) == LOW)
        limit_blocked = true;
      else if (horiz_direction == StepperDirection::CCW && digitalRead(LIMIT_HORIZ_EAST) == LOW)
        limit_blocked = true;
    }
    if (vert_steps > 0)
    {
      if (vert_direction == StepperDirection::CW && digitalRead(LIMIT_VERT_NORTH) == LOW)
        limit_blocked = true;
      else if (vert_direction == StepperDirection::CCW && digitalRead(LIMIT_VERT_SOUTH) == LOW)
        limit_blocked = true;
    }
    if (limit_blocked)
    {
      Serial.println("<LIMIT_BLOCKED>");
      return;
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
      Serial.println("<NOT_RDY>");
      return;
    }

    direction_str = strtok(NULL, " ");
    rpm_speed_str = strtok(NULL, " ");

        rpm_speed = atoi(rpm_speed_str);
        // Check if movement is blocked by an active limit switch before starting free run.
    // Direction-to-limit mapping for MVS commands:
    //   N  (north  = vertical CW)    → blocked if LIMIT_VERT_NORTH  is pressed
    //   S  (south  = vertical CCW)   → blocked if LIMIT_VERT_SOUTH  is pressed
    //   W  (west   = horizontal CW)  → blocked if LIMIT_HORIZ_WEST  is pressed
    //   E  (east   = horizontal CCW) → blocked if LIMIT_HORIZ_EAST  is pressed
    //   NW (north-west)              → blocked if EITHER corresponding limit is pressed
    //   NE (north-east)              → blocked if EITHER corresponding limit is pressed
    //   SW (south-west)              → blocked if EITHER corresponding limit is pressed
    //   SE (south-east)              → blocked if EITHER corresponding limit is pressed
    // If the direction is blocked, the command is rejected with <LIMIT_BLOCKED>
    // and no movement starts.
    bool limit_blocked = false;

    if (!strcmp(direction_str, "N"))
    {
      if (digitalRead(LIMIT_VERT_NORTH) == LOW) limit_blocked = true;
      if (!limit_blocked)
        stepper_controller.free_run_start(0, StepperDirection::IGNORE, rpm_speed, StepperDirection::CW);
    }
    else if (!strcmp(direction_str, "S"))
    {
      if (digitalRead(LIMIT_VERT_SOUTH) == LOW) limit_blocked = true;
      if (!limit_blocked)
        stepper_controller.free_run_start(0, StepperDirection::IGNORE, rpm_speed, StepperDirection::CCW);
    }
    else if (!strcmp(direction_str, "W"))
    {
      if (digitalRead(LIMIT_HORIZ_WEST) == LOW) limit_blocked = true;
      if (!limit_blocked)
        stepper_controller.free_run_start(rpm_speed, StepperDirection::CW, 0, StepperDirection::IGNORE);
    }
    else if (!strcmp(direction_str, "E"))
    {
      if (digitalRead(LIMIT_HORIZ_EAST) == LOW) limit_blocked = true;
      if (!limit_blocked)
        stepper_controller.free_run_start(rpm_speed, StepperDirection::CCW, 0, StepperDirection::IGNORE);
    }
    else if (!strcmp(direction_str, "NW"))
    {
      if (digitalRead(LIMIT_VERT_NORTH) == LOW || digitalRead(LIMIT_HORIZ_WEST) == LOW) limit_blocked = true;
      if (!limit_blocked)
        stepper_controller.free_run_start(rpm_speed, StepperDirection::CW, rpm_speed, StepperDirection::CW);
    }
    else if (!strcmp(direction_str, "NE"))
    {
      if (digitalRead(LIMIT_VERT_NORTH) == LOW || digitalRead(LIMIT_HORIZ_EAST) == LOW) limit_blocked = true;
      if (!limit_blocked)
        stepper_controller.free_run_start(rpm_speed, StepperDirection::CCW, rpm_speed, StepperDirection::CW);
    }
    else if (!strcmp(direction_str, "SW"))
    {
      if (digitalRead(LIMIT_VERT_SOUTH) == LOW || digitalRead(LIMIT_HORIZ_WEST) == LOW) limit_blocked = true;
      if (!limit_blocked)
        stepper_controller.free_run_start(rpm_speed, StepperDirection::CW, rpm_speed, StepperDirection::CCW);
    }
    else if (!strcmp(direction_str, "SE"))
    {
      if (digitalRead(LIMIT_VERT_SOUTH) == LOW || digitalRead(LIMIT_HORIZ_EAST) == LOW) limit_blocked = true;
      if (!limit_blocked)
        stepper_controller.free_run_start(rpm_speed, StepperDirection::CCW, rpm_speed, StepperDirection::CCW);
    }
    else
    {
      Serial.println("<ERROR UNKNOWN_DIRECTION>");
      return;
    }

    if (limit_blocked)
    {
      Serial.println("<LIMIT_BLOCKED>");
      return;
    }

    Serial.print("<MVS_ACK ");
    Serial.print(direction_str);
    Serial.print(" ");
    Serial.print(rpm_speed);
    Serial.println(">");
  }
  else if (!strcmp(cmd, "MVE"))
  {
    if (stepper_controller.get_running_mode() != RunningMode::FREE_RUN_MODE)
    {
      Serial.println("<NOT_RDY>");
      return;
    }
    else
    {
      stepper_controller.free_run_stop();
      Serial.println("<MVE_ACK>");
    }
  }
  else if (!strcmp(cmd, "BTRY?"))
  {
    int volt_mv = get_battery_voltage();
    Serial.print("<BTRY ");
    Serial.print(volt_mv);
    Serial.println(">");
  }
  else if (!strcmp(cmd, "MVST?"))
  {
    RunningMode mode = stepper_controller.get_running_mode();

    if (mode == RunningMode::IDLE_MODE)
    {
      Serial.println("<RDY>");
    }
    else
    {
      Serial.println("<NOT_RDY>");
    }
  }
  else if (!strcmp(cmd, "SYS_CHK"))
  {
    system_check();
    Serial.println("<SYS_CHK_DONE>");
  }
  else if (!strcmp(cmd, "DEBUG"))
  {
    Serial.print("<MV_REMAIN X: ");
    Serial.print(StepperController::horiz_steps_remain);
    Serial.print(" Y: ");
    Serial.print(StepperController::vert_steps_remain);
    Serial.println(">");
    Serial.print("<MV_CURRENT X: ");
    Serial.print(StepperController::horiz_steps_current);
    Serial.print(" Y: ");
    Serial.print(StepperController::vert_steps_current);
    Serial.println(">");
  }
    else if (!strcmp(cmd, "STOP"))
    {
      noInterrupts();
      // Stop the movement - clear all step counts and disable timer interrupts
      StepperController::horiz_steps_remain = 0;
      StepperController::vert_steps_remain = 0;
      TIMSK1 &= ~(1 << OCIE1A) & ~(1 << OCIE1B);
      TIMSK3 &= ~(1 << OCIE3A) & ~(1 << OCIE3B);
      // Pull step pins low
      PORTD &= ~(1 << 1);
      PORTC &= ~(1 << 6);
      interrupts();
      // Clear any ongoing free-run ramp-down state
      StepperController::free_run_ramping_down = false;
      stepper_controller.free_run_stop(); // Ensures IDLE_MODE
      Serial.println("<STOP_ACK>");
    }
    else if (!strcmp(cmd, "LIM?"))
  {
    // Query and report the current state of all four limit switches over Bluetooth.
    // Format:  <LIM HORIZ_WEST horiz_east vert_north VERT_SOUTH>
    // Each switch name is printed in UPPERCASE if the limit is active (pressed / pin LOW),
    // or in lowercase if the limit is inactive (not pressed / pin HIGH).
    // This allows the user to quickly see which physical limits are currently triggered.
    //
    // In contrast to the emergency stop in check_limit_switches(), this command is
    // informational only – it does not stop the motors.
    bool horiz_west_active  = (digitalRead(LIMIT_HORIZ_WEST)  == LOW);
    bool horiz_east_active  = (digitalRead(LIMIT_HORIZ_EAST)  == LOW);
    bool vert_north_active  = (digitalRead(LIMIT_VERT_NORTH)  == LOW);
    bool vert_south_active  = (digitalRead(LIMIT_VERT_SOUTH)  == LOW);

    Serial.print("<LIM");
    if (horiz_west_active) Serial.print(" HORIZ_WEST");  else Serial.print(" horiz_west");
    if (horiz_east_active) Serial.print(" HORIZ_EAST");  else Serial.print(" horiz_east");
    if (vert_north_active) Serial.print(" VERT_NORTH");  else Serial.print(" vert_north");
    if (vert_south_active) Serial.print(" VERT_SOUTH");  else Serial.print(" vert_south");
    Serial.println(">");
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
    Serial.println("<ALARM_LOW_BTRY>");
  }

  // check drv fault pins
  horiz_fault = digitalRead(horiz_fault_pin);
  vert_fault = digitalRead(vert_fault_pin);

  if (horiz_fault == LOW)
  {
    Serial.println("<ALARM_DRV_HORIZ_FAULT>");
  }

  if (vert_fault == LOW)
  {
    Serial.println("<ALARM_DRV_VERT_FAULT>");
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

    // limit switches (normally open to GND, internal pull-up)
  // When a switch is pressed, the pin reads LOW, indicating the physical limit is reached.
  // The internal pull-up keeps the pin HIGH when the switch is not pressed.
  pinMode(LIMIT_HORIZ_WEST,  INPUT_PULLUP);
  pinMode(LIMIT_HORIZ_EAST,  INPUT_PULLUP);
  pinMode(LIMIT_VERT_NORTH,  INPUT_PULLUP);
  pinMode(LIMIT_VERT_SOUTH,  INPUT_PULLUP);

  // set sleep pins
  digitalWrite(horiz_sleep_pin, HIGH);
  digitalWrite(vert_sleep_pin, HIGH);

    // set reset pins
  digitalWrite(horiz_reset_pin, HIGH);
  digitalWrite(vert_reset_pin, HIGH);
}

// Poll limit switches and stop motors if any limit is triggered.
// This function is called once per loop() iteration to provide a safety watchdog
// that catches mechanical over-travel even if the movement was started before the
// limit was pressed, or if movement erroneously starts toward a limit.
//
// When a limit switch is pressed (pin reads LOW), the function checks whether the
// motors are currently running. If so, it performs an emergency stop:
//   1. Disables timer interrupts (TIMSK1/TIMSK3)
//   2. Clears all remaining step counts
//   3. Pulls both step pins LOW to release the driver signal
//   4. Sets the controller back to IDLE_MODE
//   5. Reports which limit(s) triggered the stop over Bluetooth, e.g. <LIMIT HORIZ_WEST>
//
// The stop code is identical to the <STOP> command handler.
// This polling approach was chosen over pin-change interrupts to avoid any risk of
// interrupt timing interference with the stepper motor timer ISRs.
void check_limit_switches()
{
  // Read all limit switches (LOW = triggered, normally open to GND)
  bool horiz_west_active  = (digitalRead(LIMIT_HORIZ_WEST)  == LOW);
  bool horiz_east_active  = (digitalRead(LIMIT_HORIZ_EAST)  == LOW);
  bool vert_north_active  = (digitalRead(LIMIT_VERT_NORTH)  == LOW);
  bool vert_south_active  = (digitalRead(LIMIT_VERT_SOUTH)  == LOW);

  // If any limit is triggered, stop all motor movement immediately
  if (horiz_west_active || horiz_east_active || vert_north_active || vert_south_active)
  {
    // Only stop if currently moving (free run or move mode)
    RunningMode mode = stepper_controller.get_running_mode();
    if (mode != RunningMode::IDLE_MODE)
    {
                  // Force stop: disable timer interrupts and clear step counts
      noInterrupts();
      StepperController::horiz_steps_remain = 0;
      StepperController::vert_steps_remain = 0;
      TIMSK1 &= ~(1 << OCIE1A) & ~(1 << OCIE1B);
      TIMSK3 &= ~(1 << OCIE3A) & ~(1 << OCIE3B);
      // Pull step pins low
      PORTD &= ~(1 << 1);
      PORTC &= ~(1 << 6);
      // Clear any free-run ramp-down state so free_run_stop() doesn't
      // try to start a new ramp-down on already-stopped motors
      StepperController::free_run_ramping_down = false;
      StepperController::free_run_ramp_steps_horiz = 0;
      StepperController::free_run_ramp_steps_vert = 0;
      interrupts();

      // Transition to IDLE_MODE (handles both FREE_RUN and MOVE modes)
      stepper_controller.free_run_stop();

      // Report which limit was hit
      Serial.print("<LIMIT");
      if (horiz_west_active)  Serial.print(" HORIZ_WEST");
      if (horiz_east_active)  Serial.print(" HORIZ_EAST");
      if (vert_north_active)  Serial.print(" VERT_NORTH");
      if (vert_south_active)  Serial.print(" VERT_SOUTH");
      Serial.println(">");
    }
  }
}
