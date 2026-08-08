//  MoonStalker
//  SW for MoonStalker Drive unit control

//  Variable definitions

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
  // Set after a receive buffer overflow: the rest of the oversized command
  // is thrown away one byte per loop() iteration until its terminator
  // arrives. Draining in the main loop (instead of spinning on
  // Serial.available()) keeps the limit switch watchdog running even if the
  // sender stalls mid-command.
  static bool discarding = false;

  if (Serial.available() > 0)
  {
    char incoming_char = Serial.read();

    if (discarding)
    {
      if (incoming_char == '>' || incoming_char == 10 || incoming_char == 13)
        discarding = false;
    }
    else if (num_recv_char >= 64)
    {
      Serial.println(F("<FATAL_ERROR RCV_BUFFER_OVERFLOW>"));
      // Reset the buffer and discard the remainder of the oversized command.
      // The truncated command is never executed.
      *command_buffer = 0;
      num_recv_char = 0;
      discarding = (incoming_char != '>' &&
                    incoming_char != 10 &&
                    incoming_char != 13);
    }
    // ignore newline characters
    else if ((incoming_char != 10) && (incoming_char != 13))
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
  // Voltage divider: R1 = 5088 Ohm, R2 = 14930 Ohm
  // Vbat_mV = ADC * (5000/1023) * (R1+R2)/R1
  //         = value * 5000 * (5088+14930) / (5088*1023)
  //
  // The multiplications are interleaved with the divisions so no
  // intermediate result exceeds 32 bits. Evaluating value*5000*20018
  // first would overflow uint32_t for any ADC reading above 42.
  // Worst-case intermediates below (value = 1023):
  //   1023 * 20018 = 20,478,414
  //   20,478,414 / 5088 * 5000 = 20,120,000
  // Both fit comfortably; the residual rounding error (~5 mV at 19.7 V)
  // is well under the ADC's own 4.9 mV per LSB quantisation.
  voltage_mv = (uint32_t)value * (5088UL + 14930UL) / 5088UL * 5000UL / 1023UL;

  return voltage_mv;
}


/* handle_incoming_command

   handle command tag that arrived
   over serial connection

   commands:
   <MV a b>
   <BTRY?>
   <MVST?>
   <ALARMS?>
   <DEBUG>
   <STOP>
   <MVS direction speed>
   <LIM?>
   <STEP_COUNTER?>
   <STEP_COUNTER_RESET>
   <COORDS?>

*/
void handle_incoming_command(char *command_buff)
{
  char command[65];
  char *cmd;

  // extract command without starting '<'
  // and ending '>'
  strcpy(command, command_buff + 1);
  size_t command_len = strlen(command);
  // Nothing between the brackets (e.g. "<>"): without this guard the
  // terminator strip below would write to command[-1].
  if (command_len == 0)
    return;
  command[command_len - 1] = 0;

  cmd = strtok(command, " ");
  // Empty command body, no token to dispatch on.
  if (cmd == NULL)
    return;

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
      Serial.println("<MV_NACK NOT_RDY>");
      return;
    }
    horiz_steps_str = strtok(NULL, " ");
    vert_steps_str = strtok(NULL, " ");
    rpm_speed_str = strtok(NULL, " ");

    // strtok returns NULL for a missing parameter; passing that to atoi()
    // dereferences a null pointer.
    if (horiz_steps_str == NULL || vert_steps_str == NULL || rpm_speed_str == NULL)
    {
      Serial.println("<MV_NACK BAD_PARAMS>");
      return;
    }

    // Parse as long, then range check, so out-of-range input is rejected
    // rather than silently wrapping. The lower bound is -32767 and not
    // -32768 because the sign is stripped by negation below, and negating
    // INT16_MIN overflows.
    long horiz_steps_long = atol(horiz_steps_str);
    long vert_steps_long  = atol(vert_steps_str);
    long rpm_speed_long   = atol(rpm_speed_str);

    // A non-positive speed has no valid timer value; the axes would run at
    // whatever speed the previous move left configured.
    if (rpm_speed_long <= 0 || rpm_speed_long > 32767L ||
        horiz_steps_long < -32767L || horiz_steps_long > 32767L ||
        vert_steps_long  < -32767L || vert_steps_long  > 32767L)
    {
      Serial.println("<MV_NACK BAD_PARAMS>");
      return;
    }

    // Nothing to do. Passing this through would leave the controller stuck
    // in MOVE_MODE, because neither axis would ever step and so nothing
    // would return it to IDLE_MODE.
    if (horiz_steps_long == 0 && vert_steps_long == 0)
    {
      Serial.println("<MV_NACK NO_MOVEMENT>");
      return;
    }

    horiz_steps = (int)horiz_steps_long;
    vert_steps  = (int)vert_steps_long;
    rpm_speed   = (int)rpm_speed_long;

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
    //   Horizontal CW  (west)
    //     → blocked if LIMIT_HORIZ_WEST  is pressed (pin LOW)
    //   Horizontal CCW (east)
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
      Serial.println("<MV_NACK LIMIT_BLOCKED>");
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

        // Can only start free run from IDLE_MODE.
    // Must stop the current mode first (MV cannot be interrupted, free run
    // must be stopped before starting a new free run).
    if (stepper_controller.get_running_mode() != RunningMode::IDLE_MODE)
    {
      Serial.println("<MVS_NACK NOT_RDY>");
      return;
    }

    direction_str = strtok(NULL, " ");
    rpm_speed_str = strtok(NULL, " ");

    // strtok returns NULL for a missing parameter. A bare "<MVS>" would
    // otherwise reach strcmp(direction_str, "N") with a null pointer.
    if (direction_str == NULL || rpm_speed_str == NULL)
    {
      Serial.println("<MVS_NACK BAD_PARAMS>");
      return;
    }

    // A non-positive speed produces no valid timer value, so no axis would
    // actually start while the controller still entered FREE_RUN_MODE.
    long rpm_speed_long = atol(rpm_speed_str);
    if (rpm_speed_long <= 0 || rpm_speed_long > 32767L)
    {
      Serial.println("<MVS_NACK BAD_PARAMS>");
      return;
    }
    rpm_speed = (int)rpm_speed_long;

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
    // If the direction is blocked, the command is rejected with <MVS_NACK LIMIT_BLOCKED>
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
      Serial.println("<MVS_NACK UNKNOWN_DIRECTION>");
      return;
    }

    if (limit_blocked)
    {
      Serial.println("<MVS_NACK LIMIT_BLOCKED>");
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
      Serial.println("<MVE_NACK NOT_RDY>");
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
      Serial.println("<MVST IDLE>");
    }
    else if (mode == RunningMode::MOVE_MODE)
    {
      Serial.println("<MVST MOVING>");
    }
    else if (mode == RunningMode::FREE_RUN_MODE)
    {
      Serial.println("<MVST FREE_RUN>");
    }
  }
    else if (!strcmp(cmd, "ALARMS?"))
  {
    check_alarms();
  }
  else if (!strcmp(cmd, "DEBUG"))
  {
    // Only show remaining and current steps for MOVE_MODE, not FREE_RUN_MODE
    // (free run uses 65535 steps_remain and the values are meaningless for debugging)
    if (stepper_controller.get_running_mode() != RunningMode::FREE_RUN_MODE)
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
    else
    {
      // Always answer, so a client waiting on a reply does not hang.
      Serial.println("<DEBUG_NA FREE_RUN>");
    }
  }
    else if (!strcmp(cmd, "STEP_COUNTER?"))
  {
    noInterrupts();
    uint32_t x = StepperController::step_counter_x;
    uint32_t y = StepperController::step_counter_y;
    interrupts();
    Serial.print("<STEP_COUNTER X:");
    Serial.print(x);
    Serial.print(" Y:");
    Serial.print(y);
    Serial.println(">");
  }
    else if (!strcmp(cmd, "STEP_COUNTER_RESET"))
  {
    noInterrupts();
    StepperController::step_counter_x = 0;
    StepperController::step_counter_y = 0;
    StepperController::coord_x = 0;
    StepperController::coord_y = 0;
    interrupts();
    Serial.println("<STEP_COUNTER_RESET_ACK>");
  }
    else if (!strcmp(cmd, "COORDS?"))
  {
    noInterrupts();
    int32_t x = StepperController::coord_x;
    int32_t y = StepperController::coord_y;
    interrupts();
    Serial.print("<COORDS X:");
    Serial.print(x);
    Serial.print(" Y:");
    Serial.print(y);
    Serial.println(">");
  }
    else if (!strcmp(cmd, "STOP"))
    {
      StepperController::emergency_stop();
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
  else
  {
    // Unrecognised command tag. Always answer, so a client waiting on a
    // reply does not hang waiting for a response that will never come.
    Serial.print("<ERR UNKNOWN_COMMAND ");
    Serial.print(cmd);
    Serial.println(">");
  }
}

// Check for any error conditions and return alarms in a single response.
// Response format: <ALARMS [alarm1] [alarm2] ...>
//   ALARM_LOW_BTRY             : Present if battery voltage < 10000 mV
//   ALARM_DRV_HORIZ_FAULT      : Present if horizontal DRV fault pin is LOW
//   ALARM_DRV_VERT_FAULT       : Present if vertical DRV fault pin is LOW
//   ALARM_LIMIT_HORIZ_WEST_ACTIVE  : Present if horizontal west limit is pressed
//   ALARM_LIMIT_HORIZ_EAST_ACTIVE  : Present if horizontal east limit is pressed
//   ALARM_LIMIT_VERT_NORTH_ACTIVE  : Present if vertical north limit is pressed
//   ALARM_LIMIT_VERT_SOUTH_ACTIVE  : Present if vertical south limit is pressed
void check_alarms()
{
  int battery_volt_mv = get_battery_voltage();
  int horiz_fault = digitalRead(horiz_fault_pin);
  int vert_fault  = digitalRead(vert_fault_pin);

  bool horiz_west_active  = (digitalRead(LIMIT_HORIZ_WEST)  == LOW);
  bool horiz_east_active  = (digitalRead(LIMIT_HORIZ_EAST)  == LOW);
  bool vert_north_active  = (digitalRead(LIMIT_VERT_NORTH)  == LOW);
  bool vert_south_active  = (digitalRead(LIMIT_VERT_SOUTH)  == LOW);

  Serial.print("<ALARMS");

  if (battery_volt_mv < BATTERY_LOW_LIMIT_MV)
  {
    Serial.print(" ALARM_LOW_BTRY");
  }

  if (horiz_fault == LOW)
  {
    Serial.print(" ALARM_DRV_HORIZ_FAULT");
  }

  if (vert_fault == LOW)
  {
    Serial.print(" ALARM_DRV_VERT_FAULT");
  }

  if (horiz_west_active)
  {
    Serial.print(" ALARM_LIMIT_HORIZ_WEST_ACTIVE");
  }

  if (horiz_east_active)
  {
    Serial.print(" ALARM_LIMIT_HORIZ_EAST_ACTIVE");
  }

  if (vert_north_active)
  {
    Serial.print(" ALARM_LIMIT_VERT_NORTH_ACTIVE");
  }

  if (vert_south_active)
  {
    Serial.print(" ALARM_LIMIT_VERT_SOUTH_ACTIVE");
  }

  Serial.println(">");
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

// Poll limit switches and stop motors if an axis is travelling into a limit.
// This function is called once per loop() iteration to provide a safety watchdog
// that catches mechanical over-travel even if the movement was started before the
// limit was pressed, or if movement erroneously starts toward a limit.
//
// The check is direction-aware: an axis is only stopped when it is actually
// moving toward the pressed switch, using the same mapping the <MV> and <MVS>
// handlers apply before starting a move:
//   horizontal CW  -> west,  horizontal CCW -> east
//   vertical   CW  -> north, vertical   CCW -> south
// A pressed limit therefore does not cancel a recovery move that drives away
// from it. Stopping on any pressed limit regardless of direction would make it
// impossible to back off a switch once it was hit, because this watchdog would
// abort the recovery move on its very first loop iteration.
//
// An axis counts as moving only while its steps_remain is non-zero, so the
// stale direction state of an idle axis (for example the vertical axis during
// "<MV 200 0 60>", or an IGNORE axis in free run) cannot trigger a stop.
//
// When a stop is warranted the function performs an emergency stop:
//   1. Disables timer interrupts (TIMSK1/TIMSK3)
//   2. Clears all remaining step counts
//   3. Pulls both step pins LOW to release the driver signal
//   4. Sets the controller back to IDLE_MODE
//   5. Reports which limit(s) triggered the stop over Bluetooth, e.g. <LIMIT HORIZ_WEST>
//
// Note that the stop halts both axes (emergency_stop() is all-or-nothing), so a
// diagonal move driving only one axis into a limit stops the other axis too.
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

  // Debounce: a limit must read pressed continuously for LIMIT_DEBOUNCE_MS
  // before it is acted on. The previous filter required two consecutive
  // loop() iterations, which span only tens of microseconds and so filtered
  // nothing; mechanical contact bounce on these switches lasts a few
  // milliseconds. The cost is at most a couple of extra steps of
  // over-travel before the stop takes effect.
  const unsigned long LIMIT_DEBOUNCE_MS = 5;
  static bool          debounce_running = false;
  static unsigned long debounce_start_ms = 0;

  bool any_triggered = (horiz_west_active || horiz_east_active || vert_north_active || vert_south_active);

  if (!any_triggered)
  {
    debounce_running = false;  // Reset debounce when all switches released
    return;
  }

  if (!debounce_running)
  {
    debounce_running = true;
    debounce_start_ms = millis();
    return;  // Wait for the switch to settle
  }

  if (millis() - debounce_start_ms < LIMIT_DEBOUNCE_MS)
    return;  // Still settling

  // Only stop if currently moving (free run or move mode)
  if (stepper_controller.get_running_mode() == RunningMode::IDLE_MODE)
    return;

  // Snapshot the motion state atomically; the ISRs update these.
  noInterrupts();
  bool horiz_moving = (StepperController::horiz_steps_remain > 0);
  bool vert_moving  = (StepperController::vert_steps_remain  > 0);
  StepperDirection horiz_dir = StepperController::horiz_direction_state;
  StepperDirection vert_dir  = StepperController::vert_direction_state;
  interrupts();

  bool horiz_into_west  = (horiz_moving && horiz_dir == StepperDirection::CW  && horiz_west_active);
  bool horiz_into_east  = (horiz_moving && horiz_dir == StepperDirection::CCW && horiz_east_active);
  bool vert_into_north  = (vert_moving  && vert_dir  == StepperDirection::CW  && vert_north_active);
  bool vert_into_south  = (vert_moving  && vert_dir  == StepperDirection::CCW && vert_south_active);

  if (!horiz_into_west && !horiz_into_east && !vert_into_north && !vert_into_south)
    return;  // Limit pressed, but nothing is driving into it

  StepperController::emergency_stop();

  // Report which limit was driven into
  Serial.print("<LIMIT");
  if (horiz_into_west)  Serial.print(" HORIZ_WEST");
  if (horiz_into_east)  Serial.print(" HORIZ_EAST");
  if (vert_into_north)  Serial.print(" VERT_NORTH");
  if (vert_into_south)  Serial.print(" VERT_SOUTH");
  Serial.println(">");
}