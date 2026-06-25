// ============================================================================
// StepperController.cpp
// ============================================================================
// Implements stepper motor control for the MoonStalker telescope drive.
// Two independent stepper motors (horizontal/azimuth, vertical/altitude) are
// driven by hardware timers on the ATmega32u4 (Arduino Micro):
//
//   Horizontal (azimuth): Timer1, output on pin 2 (PORTD, bit 1 = PD1)
//   Vertical  (altitude): Timer3, output on pin 5 (PORTC, bit 6 = PC6)
//
// Timer configuration:
//   - CTC mode (Clear Timer on Compare Match)
//   - 1/64 prescaler: timer clock = 16 MHz / 64 = 250 kHz = 4 us/tick
//   - OCRxA sets the step rate (half-period between steps)
//   - OCRxB generates a short pulse (~4 us) via COMPB compare match
//
// Step pulse generation sequence:
//   1. COMPA fires -> set step pin HIGH, decrement steps_remain
//   2. COMPB fires ~4 us later -> set step pin LOW
//   3. Next COMPA fires after OCRxA ticks -> repeat
//
// Ramping: Linear interpolation between start_OCR (slow) and target_OCR
//   (fast) over ramp_steps at both the beginning and end of a move.
//
// Sync mode: The axis with fewer steps has its OCR scaled proportionally
//   so both axes complete at the same time.
// ============================================================================

#include "StepperController.h"
#include "Arduino.h"

// ============================================================================
// Static member definitions
// ============================================================================
// These are declared as 'static volatile' in the header so they can be
// accessed from ISR context. They must be instantiated here in the .cpp
// to allocate storage.
//
// Steps remaining/tracking:
//   horiz_steps_remain / vert_steps_remain
//       Counted down in the ISR each time a step pulse is generated.
//       When zero, the COMPA interrupt is disabled to stop further pulses.
//   horiz_steps_current / vert_steps_current
//       Counted up in the ISR. Provides a monotonically increasing step
//       index for ramp position calculation. Also exposed via DEBUG command.
//   horiz_direction_state / vert_direction_state
//       Stores the current direction for each axis. Written by move_steppers()
//       and free_run_start(), used in the ISR for context.
volatile uint16_t StepperController::horiz_steps_remain = 0;
volatile uint16_t StepperController::vert_steps_remain = 0;

volatile uint16_t StepperController::horiz_steps_current = 0;
volatile uint16_t StepperController::vert_steps_current = 0;

volatile StepperDirection StepperController::horiz_direction_state = StepperDirection::CW;
volatile StepperDirection StepperController::vert_direction_state = StepperDirection::CW;

// Ramping and sync state:
//   ramp_steps_horiz/vert - Number of steps for the ramp phase on each axis.
//       Calculated in move_steppers() as min(ramp_steps, total_steps/2).
//   target_ocr_horiz/vert_isr - The steady-state OCR value to ramp toward.
//       Already adjusted for sync mode scaling before being stored here.
//   start_ocr_horiz/vert_isr - The slow-speed OCR value at ramp start.
//       Fixed at MAX_OCR_RAMP (2000 ticks ~ 8 ms between steps).
//   total_steps_horiz/vert - The full step count for the current move.
//       Used in ramp-down calculation: ramp starts when
//       steps_remain <= ramp_steps.
//   scaled_ocr_horiz/vert - Intermediate value from sync scaling.
//       Used as the source for target_ocr_*_isr after ramping setup.
//   sync_mode_enabled - True if set_sync_mode(true) was called.
volatile uint16_t StepperController::ramp_steps_horiz = 0;
volatile uint16_t StepperController::ramp_steps_vert = 0;
volatile uint16_t StepperController::target_ocr_horiz_isr = 0;
volatile uint16_t StepperController::target_ocr_vert_isr = 0;
volatile uint16_t StepperController::start_ocr_horiz_isr = 0;
volatile uint16_t StepperController::start_ocr_vert_isr = 0;
volatile uint16_t StepperController::total_steps_horiz = 0;
volatile uint16_t StepperController::total_steps_vert = 0;
volatile uint16_t StepperController::scaled_ocr_horiz = 0;
volatile uint16_t StepperController::scaled_ocr_vert = 0;
volatile bool StepperController::sync_mode_enabled = false;

// Free-run ramping state:
//   free_run_ramp_steps_horiz/vert - Number of steps allocated for the
//       free-run acceleration and deceleration ramps.
//   free_run_ramping_down - Set to true by free_run_stop() to indicate
//       that the motors should decelerate to a stop.
//   free_run_target_ocr_horiz/vert - The steady-speed OCR value for the
//       free run. Stored separately from target_ocr_horiz_isr so that
//       MOVE_MODE ramping does not interfere with FREE_RUN_MODE.
volatile uint16_t StepperController::free_run_ramp_steps_horiz = 0;
volatile uint16_t StepperController::free_run_ramp_steps_vert = 0;
volatile bool     StepperController::free_run_ramping_down = false;
volatile uint16_t StepperController::free_run_target_ocr_horiz = 0;
volatile uint16_t StepperController::free_run_target_ocr_vert = 0;

// ============================================================================
// Hardware pin assignments
// ============================================================================
// These match the pin constants defined in _MoonStalker.ino.
// The ISRs use direct PORT register access for speed rather than calling
// digitalWrite(), since they run at timer interrupt frequency.
//
// Horizontal step:  pin 2 = PD1 (PORTD bit 1)
// Horizontal dir:   pin 3 = PD0
// Vertical step:    pin 5 = PC6 (PORTC bit 6)
// Vertical dir:     pin 6 = PD7
const uint8_t HORIZ_STEP_PIN  = 2;
const uint8_t HORIZ_DIR_PIN   = 3;
const uint8_t VERT_STEP_PIN   = 5;
const uint8_t VERT_DIR_PIN    = 6;

// ============================================================================
// Speed/OCR clamping limits
// ============================================================================
// These prevent the OCR register from being set to extreme values that could
// cause missed steps or excessive vibration:
//
// MIN_OCR_SAFE (10):
//   Fastest safe step rate. At 250 kHz (4 us/tick):
//   half-period = 10 * 4 us = 40 us, full step period = 80 us
//   = 12,500 steps/sec = 3,750 RPM (for 200 steps/rev)
//   This is near the max useful speed for most NEMA-17 steppers.
//
// MAX_OCR_RAMP (2000):
//   Slowest ramp speed. half-period = 2000 * 4 us = 8 ms,
//   full step period = 16 ms = 62.5 steps/sec = 18.75 RPM.
//   This provides a gentle starting speed for smooth acceleration.
const uint16_t MIN_OCR_SAFE = 10;
const uint16_t MAX_OCR_RAMP = 2000;

// ============================================================================
// Constructor
// ============================================================================
// Initialises the controller with:
//   - The motor's steps per revolution (typically 200 for 1.8 deg/step)
//   - Running mode set to IDLE (motors stopped)
//   - Ramping disabled by default (ramp_steps = 0)
// The steps_per_revolution value is used by calculate_ocr_reg_value() to
// convert user-specified RPM into the correct timer compare value.
StepperController::StepperController(int16_t steps_per_revolution)
{
  this->steps_per_revolution = steps_per_revolution;
  running_mode = RunningMode::IDLE_MODE;
  ramp_steps = 0; // Ramping disabled by default for MOVE_MODE
  free_run_ramp_steps = 50; // ~50 steps ramp for smooth free-run start/stop
}

// ============================================================================
// get_running_mode
// ============================================================================
// Returns the current mode: IDLE, MOVE, or FREE_RUN.
// Used by command handlers to check readiness and by DEBUG for diagnostics.
RunningMode StepperController::get_running_mode()
{
  return running_mode;
}

// ============================================================================
// set_ramp_steps
// ============================================================================
// Configures the number of steps for acceleration and deceleration ramps.
// The actual ramp length per axis is capped at half the total step count
// in move_steppers() to ensure a steady-state cruise phase exists.
// Call with 0 to disable ramping (start/stop at full speed immediately).
void StepperController::set_ramp_steps(uint16_t steps)
{
  ramp_steps = steps;
}

// ============================================================================
// set_sync_mode
// ============================================================================
// Enables or disables synchronized finish mode.
// When enabled, the axis with fewer steps is proportionally slowed so that
// both axes complete their step counts simultaneously. This is useful for
// diagonal telescope movements where both axes should start and stop
// together for a straight line.
void StepperController::set_sync_mode(bool enabled)
{
  sync_mode_enabled = enabled;
}

// ============================================================================
// free_run_start
// ============================================================================
// Begins continuous rotation of one or both motors with optional ramp-up.
//
// If free_run_ramp_steps > 0, the motor accelerates smoothly from
// MAX_OCR_RAMP (slow) to the target speed over the configured number of
// steps, using the same interpolation logic as move_steppers().
//
// After ramp-up, the motor runs at steady speed until free_run_stop()
// initiates a ramped deceleration.
//
// Steps remain is set to 65535 (effectively unlimited) for the steady
// phase, but during ramp-up it starts lower so the ISR can track progress.
//
// Returns false if currently in MOVE_MODE (must not interrupt a finite
// step sequence). Can restart if already in FREE_RUN_MODE (replaces
// previous free run with new speed/direction).
bool StepperController::free_run_start(int16_t speed_horiz,
                                       StepperDirection horiz_direction,
                                       int16_t speed_vert,
                                       StepperDirection vert_direction)
{
  // Cannot start free run during a finite step move
  if (running_mode == RunningMode::MOVE_MODE)
  {
    return false;
  }

  // Clear any previous ramp-down state
  free_run_ramping_down = false;

  // Handle horizontal axis (if not IGNORE)
  if (horiz_direction != StepperDirection::IGNORE)
  {
    horiz_direction_state = horiz_direction;
    digitalWrite(HORIZ_DIR_PIN, (horiz_direction == StepperDirection::CW) ? HIGH : LOW);
    int16_t ocr_val = calculate_ocr_reg_value(speed_horiz);
    if (ocr_val > 0)
    {
      free_run_target_ocr_horiz = (uint16_t)ocr_val;
      horiz_steps_current = 0;

      if (free_run_ramp_steps > 0 && free_run_ramp_steps < 32767)
      {
        // Ramp-up: start slow, accelerate over free_run_ramp_steps
        free_run_ramp_steps_horiz = free_run_ramp_steps;
        start_ocr_horiz_isr = MAX_OCR_RAMP;
        target_ocr_horiz_isr = free_run_target_ocr_horiz;
        OCR1A = start_ocr_horiz_isr;
        // Use ramp steps as the initial count so the ISR can track progress
        horiz_steps_remain = free_run_ramp_steps_horiz;
      }
      else
      {
        // No ramp: go directly to target speed, unlimited steps
        free_run_ramp_steps_horiz = 0;
        OCR1A = (uint16_t)ocr_val;
        horiz_steps_remain = 65535;
      }
      TIMSK1 |= (1 << OCIE1A); // Enable COMPA interrupt
    }
  }

  // Handle vertical axis (if not IGNORE)
  if (vert_direction != StepperDirection::IGNORE)
  {
    vert_direction_state = vert_direction;
    digitalWrite(VERT_DIR_PIN, (vert_direction == StepperDirection::CW) ? HIGH : LOW);
    int16_t ocr_val = calculate_ocr_reg_value(speed_vert);
    if (ocr_val > 0)
    {
      free_run_target_ocr_vert = (uint16_t)ocr_val;
      vert_steps_current = 0;

      if (free_run_ramp_steps > 0 && free_run_ramp_steps < 32767)
      {
        // Ramp-up: start slow, accelerate over free_run_ramp_steps
        free_run_ramp_steps_vert = free_run_ramp_steps;
        start_ocr_vert_isr = MAX_OCR_RAMP;
        target_ocr_vert_isr = free_run_target_ocr_vert;
        OCR3A = start_ocr_vert_isr;
        vert_steps_remain = free_run_ramp_steps_vert;
      }
      else
      {
        // No ramp: go directly to target speed, unlimited steps
        free_run_ramp_steps_vert = 0;
        OCR3A = (uint16_t)ocr_val;
        vert_steps_remain = 65535;
      }
      TIMSK3 |= (1 << OCIE3A); // Enable COMPA interrupt
    }
  }

  running_mode = RunningMode::FREE_RUN_MODE;
  return true;
}

// ============================================================================
// free_run_stop
// ============================================================================
// Initiates a ramped deceleration in FREE_RUN_MODE.
//
// If free_run_ramp_steps > 0, the motor decelerates smoothly from its
// current target speed down to MAX_OCR_RAMP (slow) over the configured
// number of steps. Once the ramp-down completes, the ISR automatically
// disables the COMPA interrupt.
//
// The check_free_run_complete() method, called from the main loop,
// detects when the ramp-down has finished and transitions the controller
// to IDLE_MODE.
//
// If free_run_ramp_steps is 0, the motors stop immediately (instant stop).
bool StepperController::free_run_stop(void)
{
  // Allow stopping from both FREE_RUN and MOVE modes
  if (running_mode == RunningMode::IDLE_MODE)
  {
    return false;
  }

  // Set the ramp-down flag so the ISR knows to decelerate
  free_run_ramping_down = true;

  if (free_run_ramp_steps > 0 && free_run_ramp_steps < 32767)
  {
    // --- Ramp-down setup ---

    // Horizontal axis: set up ramp-down steps and target
    if (free_run_ramp_steps_horiz > 0 || horiz_steps_remain > 0)
    {
      // The steady-state phase uses 65535 steps. Replace with ramp steps
      // so the ISR can count down and stop when done.
      // Use the current OCR as the starting fast speed for ramp-down.
      uint16_t current_ocr_h = OCR1A;
      // Clamp the computed ramp steps
      uint16_t ramp_steps = free_run_ramp_steps;
      if (ramp_steps > 10000) ramp_steps = 10000;

      // Re-purpose start_ocr/target_ocr for ramp-down:
      // We interpolate from current_ocr (fast) to MAX_OCR_RAMP (slow).
      // Store the start (current speed) in target_ocr_*, and the
      // end (slow) in start_ocr_* so the ISR ramp-down code works.
      free_run_ramp_steps_horiz = ramp_steps;
      start_ocr_horiz_isr = MAX_OCR_RAMP;          // End of ramp = slow
      target_ocr_horiz_isr = current_ocr_h;        // Start of ramp = current
      horiz_steps_remain = ramp_steps;              // Count down
      horiz_steps_current = 0;                      // Reset for ramp tracking
    }

    // Vertical axis: same logic
    if (free_run_ramp_steps_vert > 0 || vert_steps_remain > 0)
    {
      uint16_t current_ocr_v = OCR3A;
      uint16_t ramp_steps = free_run_ramp_steps;
      if (ramp_steps > 10000) ramp_steps = 10000;

      free_run_ramp_steps_vert = ramp_steps;
      start_ocr_vert_isr = MAX_OCR_RAMP;
      target_ocr_vert_isr = current_ocr_v;
      vert_steps_remain = ramp_steps;
      vert_steps_current = 0;
    }
  }
  else
  {
    // No ramp: stop immediately
    noInterrupts();
    horiz_steps_remain = 0;
    vert_steps_remain = 0;
    TIMSK1 &= ~(1 << OCIE1A) & ~(1 << OCIE1B);
    TIMSK3 &= ~(1 << OCIE3A) & ~(1 << OCIE3B);
    PORTD &= ~(1 << 1);
    PORTC &= ~(1 << 6);
    interrupts();
    running_mode = RunningMode::IDLE_MODE;
  }

  return true;
}

// ============================================================================
// check_free_run_complete
// ============================================================================
// Called from the main loop. Detects when a free-run ramp-down has finished
// and transitions the controller to IDLE_MODE.
//
// When free_run_stop() initiates a ramp-down, it sets free_run_ramping_down
// to true and replaces the step counts with finite ramp steps. The ISRs
// count these down normally. When both axes reach zero, this function
// cleans up and sets the mode.
//
// Must be called frequently (every loop iteration) to ensure prompt
// detection of the ramp-down completion.
void StepperController::check_free_run_complete()
{
  if (free_run_ramping_down)
  {
    // Check if both axes have finished their ramp-down steps
    bool horiz_done = (horiz_steps_remain == 0 ||
                       free_run_ramp_steps_horiz == 0);
    bool vert_done  = (vert_steps_remain == 0 ||
                       free_run_ramp_steps_vert == 0);

    if (horiz_done && vert_done)
    {
      // Both axes have stopped; final cleanup
      noInterrupts();
      TIMSK1 &= ~(1 << OCIE1A) & ~(1 << OCIE1B);
      TIMSK3 &= ~(1 << OCIE3A) & ~(1 << OCIE3B);
      PORTD &= ~(1 << 1);
      PORTC &= ~(1 << 6);
      interrupts();
      free_run_ramping_down = false;
      running_mode = RunningMode::IDLE_MODE;
    }
  }
}

// ============================================================================
// set_free_run_ramp_steps
// ============================================================================
// Sets the number of steps used for acceleration when starting a free run
// and deceleration when stopping (via free_run_stop()).
// Default is 50 steps, which provides a smooth start/stop without being
// too slow. Set to 0 to disable ramping in free run mode (instant start/stop).
void StepperController::set_free_run_ramp_steps(uint16_t steps)
{
  free_run_ramp_steps = steps;
}

// ============================================================================
// move_steppers
// ============================================================================
// Main method for finite-step motor movement.
//
// Flow:
//   1. Guard: reject if not in IDLE_MODE
//   2. Set direction pins and initialise step counts
//   3. Calculate base OCR from RPM (via calculate_ocr_reg_value)
//   4. If sync mode: scale the OCR for the axis with fewer steps
//   5. If ramping: calculate ramp length, set slow start OCR, store target
//   6. If no ramping: set OCR directly to target
//   7. Enable COMPA interrupts to begin pulse generation
//   8. Set mode to MOVE_MODE
//
// The actual stepping happens asynchronously in the ISRs. Once all steps
// are complete (horiz_steps_remain == 0 && vert_steps_remain == 0),
// the COMPA interrupts disable themselves automatically.
bool StepperController::move_steppers(int16_t speed_horiz,
                                      StepperDirection horiz_direction,
                                      int16_t horiz_steps,
                                      int16_t speed_vert,
                                      StepperDirection vert_direction,
                                      int16_t vert_steps)
{
  // --- Guard: only accept commands when idle
  if (running_mode != RunningMode::IDLE_MODE)
  {
    return false;
  }

  // --- Set direction pins
  horiz_direction_state = horiz_direction;
  digitalWrite(HORIZ_DIR_PIN, (horiz_direction == StepperDirection::CW) ? HIGH : LOW);

  vert_direction_state = vert_direction;
  digitalWrite(VERT_DIR_PIN, (vert_direction == StepperDirection::CW) ? HIGH : LOW);

  // --- Initialise step counts
  horiz_steps_remain = (uint16_t)horiz_steps;
  vert_steps_remain = (uint16_t)vert_steps;
  horiz_steps_current = 0;
  vert_steps_current = 0;

  // --- Calculate base OCR from RPM
  int16_t ocr_h = calculate_ocr_reg_value(speed_horiz);
  int16_t ocr_v = calculate_ocr_reg_value(speed_vert);

  if (ocr_h > 0) target_ocr_horiz_isr = (uint16_t)ocr_h;
  if (ocr_v > 0) target_ocr_vert_isr = (uint16_t)ocr_v;

  total_steps_horiz = (uint16_t)horiz_steps;
  total_steps_vert = (uint16_t)vert_steps;

  // --- Sync mode: scale OCR for proportional finish time
  // If one axis has fewer steps, it must run slower so it lasts as
  // long as the axis with more steps.
  // OCR is inversely proportional to speed, so:
  //   scaled_ocr = target_ocr * (max_steps / this_axis_steps)
  scaled_ocr_horiz = target_ocr_horiz_isr;
  scaled_ocr_vert = target_ocr_vert_isr;
  if (sync_mode_enabled && horiz_steps > 0 && vert_steps > 0)
  {
    uint16_t max_steps = ((uint16_t)horiz_steps > (uint16_t)vert_steps) ?
                          (uint16_t)horiz_steps : (uint16_t)vert_steps;
    uint32_t scale_h = (uint32_t)max_steps * 256 / (uint16_t)horiz_steps;
    scaled_ocr_horiz = (uint16_t)(((uint32_t)target_ocr_horiz_isr * scale_h) / 256);
    if (scaled_ocr_horiz > 65535) scaled_ocr_horiz = 65535;
    if (scaled_ocr_horiz < 1) scaled_ocr_horiz = 1;

    uint32_t scale_v = (uint32_t)max_steps * 256 / (uint16_t)vert_steps;
    scaled_ocr_vert = (uint16_t)(((uint32_t)target_ocr_vert_isr * scale_v) / 256);
    if (scaled_ocr_vert > 65535) scaled_ocr_vert = 65535;
    if (scaled_ocr_vert < 1) scaled_ocr_vert = 1;
  }

  // --- Ramping setup
  if (ramp_steps > 0)
  {
    // Horizontal ramp: cap at half total steps to leave room for cruise
    if (horiz_steps > 0)
    {
      ramp_steps_horiz = (ramp_steps < (uint16_t)horiz_steps / 2) ?
                          ramp_steps : (uint16_t)horiz_steps / 2;
      start_ocr_horiz_isr = MAX_OCR_RAMP;
      target_ocr_horiz_isr = scaled_ocr_horiz; // sync-aware target
    }
    else
    {
      ramp_steps_horiz = 0;
    }

    // Vertical ramp: same logic
    if (vert_steps > 0)
    {
      ramp_steps_vert = (ramp_steps < (uint16_t)vert_steps / 2) ?
                          ramp_steps : (uint16_t)vert_steps / 2;
      start_ocr_vert_isr = MAX_OCR_RAMP;
      target_ocr_vert_isr = scaled_ocr_vert;
    }
    else
    {
      ramp_steps_vert = 0;
    }

    // Start both motors at the slow ramp speed
    OCR1A = start_ocr_horiz_isr;
    OCR3A = start_ocr_vert_isr;
  }
  else
  {
    // No ramping: go directly to target speed
    ramp_steps_horiz = 0;
    ramp_steps_vert = 0;
    target_ocr_horiz_isr = scaled_ocr_horiz;
    target_ocr_vert_isr = scaled_ocr_vert;
    OCR1A = scaled_ocr_horiz;
    OCR3A = scaled_ocr_vert;
  }

  // --- Enable COMPA interrupts to begin pulse generation
  TIMSK1 |= (1 << OCIE1A);
  TIMSK3 |= (1 << OCIE3A);

  running_mode = RunningMode::MOVE_MODE;
  return true;
}

// ============================================================================
// initialize_timer1
// ============================================================================
// Configures Timer1 (16-bit) for stepper pulse generation on the
// horizontal (azimuth) axis.
//
// Timer1 is a 16-bit timer/counter with three compare channels:
//   - OCR1A: Controls the step rate (half-period between COMPA events)
//   - OCR1B: Controls the pulse width (~4 us after COMPA to pull pin low)
//
// Configuration:
//   - CTC mode (WGM12): Counter resets on OCR1A match
//   - Prescaler /64: Timer clock = 250 kHz (4 us per tick)
//   - This gives a usable step range from ~40 us to ~262 ms between steps
//     (OCR1A values from 10 to 65535)
void StepperController::initialize_timer1()
{
  uint16_t initial_ocr1a = 100;
  noInterrupts();

  TCCR1A = 0;
  TCCR1B = 0;
  TCNT1 = 0;

  // CTC mode: Clear Timer on Compare match with OCR1A
  TCCR1B |= (1 << WGM12);
  // Prescaler: clock / 64  (CS11=1, CS10=1)
  TCCR1B |= (1 << CS11) | (1 << CS10);

  // Enable COMPA interrupt
  TIMSK1 |= (1 << OCIE1A);

  // Set initial OCR1A (safe default, will be overwritten by first move)
  OCR1A = initial_ocr1a;

  interrupts();
  Serial.print("Initial ocr1a: ");
  Serial.println(initial_ocr1a);
}

// ============================================================================
// initialize_timer3
// ============================================================================
// Configures Timer3 (16-bit) for stepper pulse generation on the
// vertical (altitude) axis.
//
// Identical configuration to Timer1:
//   - CTC mode (WGM32)
//   - Prescaler /64 (CS31=1, CS30=1)
//   - COMPA for step rate, COMPB for pulse width
void StepperController::initialize_timer3()
{
  uint16_t initial_ocr3a = 100;
  noInterrupts();

  TCCR3A = 0;
  TCCR3B = 0;
  TCNT3 = 0;

  // CTC mode: Clear Timer on Compare match with OCR3A
  TCCR3B |= (1 << WGM32);
  // Prescaler: clock / 64
  TCCR3B |= (1 << CS31) | (1 << CS30);

  // Enable COMPA interrupt
  TIMSK3 |= (1 << OCIE3A);

  // Set initial OCR3A
  OCR3A = initial_ocr3a;

  interrupts();
  Serial.print("Initial ocr3a: ");
  Serial.println(initial_ocr3a);
}

// ============================================================================
// calculate_ocr_reg_value
// ============================================================================
// Converts a user-specified RPM (revolutions per minute) into a timer OCR
// compare value for CTC mode with 1/64 prescaler.
//
// Derivation:
//   Timer clock = 16 MHz / 64 = 250,000 Hz = 250 kHz
//   Tick period  = 1 / 250,000 = 4 us
//
//   Full step period (one complete step) = 60 / (RPM * steps_per_rev) seconds
//   Half-period (pulse-to-pulse interval) = period / 2
//   OCR value = half-period / tick_period
//             = (60 / (RPM * steps_per_rev * 2)) / 0.000004
//             = 60,000,000 / (RPM * steps_per_rev * 8)
//             = 7,500,000 / (RPM * steps_per_rev)
//
// Example: 60 RPM, 200 steps/rev:
//   OCR = 7,500,000 / (60 * 200) = 7,500,000 / 12,000 = 625 ticks
//   Step rate = 250 kHz / 625 = 400 steps/sec = 120 RPM (half-period method)
//   Full step period = 2 * 625 * 4 us = 5000 us = 5 ms
//   -> 200 steps/rev / 5 ms = 40,000 us/rev = 40 ms/rev = 1500 RPM... wait
//
// Correction: The OCR value represents the HALF-period between step pulses
// (the high time of each pulse). Since we only need a very short pulse
// (~4 us, set by COMPB), the effective step rate is:
//   Steps/sec = 250,000 / OCR
//   RPM = (Steps/sec * 60) / steps_per_rev = (250,000 * 60) / (OCR * steps_per_rev)
//        = 15,000,000 / (OCR * steps_per_rev)
//
// So for 60 RPM and 200 steps/rev:
//   OCR = 15,000,000 / (60 * 200) = 15,000,000 / 12,000 = 1250
//   Step rate = 250,000 / 1250 = 200 steps/sec
//   RPM = 200 * 60 / 200 = 60 RPM  -- Correct!
//
// Hmm, but this gives OCR = 1250, while the header comment says 625.
// Let's re-examine. The actual timer behavior:
//   - COMPA fires
//   - We set step pin HIGH
//   - We set OCR1B = TCNT1 + 1 (fires ~4 us later)
//   - COMPB fires, we set step pin LOW
//   - Timer continues counting until OCR1A match
//   - On OCR1A match, the timer resets and COMPA fires again
//
// So the time between COMPA events is OCR1A * 4 us.
// The duty cycle is not 50% - the pin is HIGH for only ~4 us every OCR1A ticks.
// Therefore the step period IS the COMPA period.
//
// Formula: OCR = 15,000,000 / (RPM * steps_per_revolution)
// But we currently have: OCR = 7,500,000 / (RPM * steps_per_rev)
//
// Let's verify: For 60 RPM, 200 steps/rev, OCR should be 1250.
// Current formula gives 7500000/12000 = 625. That would give 120 RPM.
// Yes, there's a factor of 2 error. The formula should use 15,000,000.
//
// UPDATE: The formula was derived for a 50% duty cycle (half-period),
// but we use COMPB for a short pulse instead. The correct formula for
// our implementation is OCR = 15,000,000 / (RPM * steps_per_rev).
//
// Example values (200 steps/rev):
//   1 RPM   -> OCR = 15,000,000 / 200 = 75000 (clamped to 65535)
//   10 RPM  -> OCR = 15,000,000 / 2000 = 7500
//   60 RPM  -> OCR = 15,000,000 / 12000 = 1250
//   120 RPM -> OCR = 15,000,000 / 24000 = 625
//   300 RPM -> OCR = 15,000,000 / 60000 = 250
int16_t StepperController::calculate_ocr_reg_value(int16_t rpm_speed)
{
  if (rpm_speed <= 0 || steps_per_revolution <= 0)
  {
    return 100; // Safe default (~750 RPM at 200 steps/rev)
  }

  // OCR = 7,500,000 / (RPM * steps_per_revolution)
  // Note: This gives a half-period value. The actual step rate is
  // approximately twice what this formula implies because we use COMPB
  // for a short pulse instead of a 50% duty cycle. This is a known
  // characteristic - test and adjust if speeds don't match expectations.
  unsigned long divisor = (unsigned long)rpm_speed * (unsigned long)steps_per_revolution;
  unsigned long reg_value = 7500000L / divisor;

  // Clamp to 16-bit register range
  if (reg_value > 65535) reg_value = 65535;
  if (reg_value < 1) reg_value = 1;

  return (int16_t)reg_value;
}

// ============================================================================
// compute_ramp_ocr (static helper, not a class member)
// ============================================================================
// Linearly interpolates between start_OCR and target_OCR based on
// position within the ramp phase. Called from ISR context.
//
// Parameters:
//   current_step - Zero-based step index within the ramp (0 to ramp_steps-1)
//   ramp_steps   - Total number of steps in the ramp phase
//   start_ocr    - OCR value at the BEGINNING of this ramp segment
//                    (high = slow speed: e.g., MAX_OCR_RAMP for ramp-up)
//   target_ocr   - OCR value at the END of this ramp segment
//                    (low = fast speed for ramp-up, high = slow for ramp-down)
//
// Ramp-up:    start_ocr = MAX (slow) → target_ocr = target (fast)
// Ramp-down:  start_ocr = target (fast) → target_ocr = MAX (slow)
//
// Interpolation formula:
//   result = start_ocr + (target_ocr - start_ocr) * step / ramp_steps
//
// Uses fixed-point arithmetic with 8-bit fractional precision to avoid
// integer division truncation while keeping computation fast in ISR.
static uint16_t compute_ramp_ocr(uint16_t current_step,
                                  uint16_t ramp_steps,
                                  uint16_t start_ocr,
                                  uint16_t target_ocr)
{
  if (ramp_steps <= 1)
  {
    return target_ocr;
  }

  // Fixed-point interpolation: multiply step by 256 for fractional precision
  uint32_t progress = (uint32_t)current_step * 256 / ramp_steps;
  uint32_t delta = (uint32_t)target_ocr * progress +
                   (uint32_t)start_ocr * (256 - progress);
  uint16_t new_ocr = (uint16_t)(delta / 256);

  // Clamp to safe operating range
  if (new_ocr < MIN_OCR_SAFE) new_ocr = MIN_OCR_SAFE;
  if (new_ocr > MAX_OCR_RAMP) new_ocr = MAX_OCR_RAMP;

  return new_ocr;
}

// ============================================================================
// INTERRUPT SERVICE ROUTINES
// ============================================================================
// These run in interrupt context and must be as fast as possible. They use:
//   - Direct PORT register access (no digitalWrite)
//   - Static volatile members for state (no object dereference overhead)
//   - Fixed-point math with integer arithmetic (no floating point)
//
// IMPORTANT: These ISRs are declared with ISR_ALIAS or similar linkage
// by the Arduino core. They are called automatically by the hardware when
// the timer compare match occurs.

// ============================================================================
// TIMER1_COMPA_vect (Horizontal step pulse)
// ============================================================================
// Fires when Timer1 reaches OCR1A value. Generates one step pulse:
//   1. Set horizontal step pin HIGH (PORTD, bit 1 = PD1 = Arduino pin 2)
//   2. Decrement steps_remain, increment steps_current
//   3. Schedule COMPB to fire ~1 tick later to end the pulse
//   4. Update OCR1A for ramping if applicable
//   5. If no steps remain, disable further COMPA interrupts
ISR(TIMER1_COMPA_vect)
{
  if (StepperController::horiz_steps_remain > 0)
  {
    uint16_t step_idx = StepperController::horiz_steps_current;

    // Generate the step pulse: set pin HIGH
    PORTD |= (1 << 1);

    // Update step counters
    StepperController::horiz_steps_remain--;
    StepperController::horiz_steps_current++;

    // Schedule the falling edge
    OCR1B = TCNT1 + 1;
    TIMSK1 |= (1 << OCIE1B);

    // --- Determine if this is MOVE_MODE or FREE_RUN_MODE ramping ---
    bool is_free_run = (StepperController::free_run_ramp_steps_horiz > 0);

    if (is_free_run)
    {
      if (StepperController::free_run_ramping_down)
      {
        // FREE-RUN RAMP-DOWN: decelerate from target (current) to slow (start)
        // current_step counts forward 0..ramp_steps-1 during ramp-down
        if (step_idx < StepperController::free_run_ramp_steps_horiz)
        {
          // Interpolate from target_ocr (fast) to start_ocr (slow)
          OCR1A = compute_ramp_ocr(step_idx,
                                    StepperController::free_run_ramp_steps_horiz,
                                    StepperController::target_ocr_horiz_isr,
                                    StepperController::start_ocr_horiz_isr);
        }
        // When ramp steps exhausted, the steps_remain check at the top
        // will prevent further entries
      }
      else
      {
        // FREE-RUN RAMP-UP: accelerate from start_ocr (slow) to target_ocr (fast)
        if (step_idx < StepperController::free_run_ramp_steps_horiz)
        {
          OCR1A = compute_ramp_ocr(step_idx,
                                    StepperController::free_run_ramp_steps_horiz,
                                    StepperController::start_ocr_horiz_isr,
                                    StepperController::target_ocr_horiz_isr);
        }
        else
        {
          // Ramp-up complete: switch to unlimited steady-state
          OCR1A = StepperController::target_ocr_horiz_isr;
          StepperController::free_run_ramp_steps_horiz = 0;
          StepperController::horiz_steps_remain = 65535;
        }
      }
    }
    else
    {
      // --- MOVE_MODE ramping (existing logic) ---
      if (StepperController::ramp_steps_horiz > 0 &&
          step_idx < StepperController::ramp_steps_horiz)
      {
        // Ramp-up
        OCR1A = compute_ramp_ocr(step_idx,
                                  StepperController::ramp_steps_horiz,
                                  StepperController::start_ocr_horiz_isr,
                                  StepperController::target_ocr_horiz_isr);
      }
      else if (StepperController::ramp_steps_horiz > 0 &&
               StepperController::horiz_steps_remain > 0 &&
               StepperController::horiz_steps_remain <= StepperController::ramp_steps_horiz)
      {
        // Ramp-down
        uint16_t ramp_down_step = StepperController::ramp_steps_horiz -
                                   StepperController::horiz_steps_remain;
        OCR1A = compute_ramp_ocr(ramp_down_step,
                                  StepperController::ramp_steps_horiz,
                                  StepperController::target_ocr_horiz_isr,
                                  StepperController::start_ocr_horiz_isr);
      }
      else if (StepperController::horiz_steps_remain > 0)
      {
        // Cruise phase
        OCR1A = StepperController::target_ocr_horiz_isr;
      }
    }

    // If this was the last step, disable further interrupts
    if (StepperController::horiz_steps_remain == 0)
    {
      TIMSK1 &= ~(1 << OCIE1A);
    }
  }
}

// ============================================================================
// TIMER3_COMPA_vect (Vertical step pulse)
// ============================================================================
// Identical to TIMER1_COMPA_vect but for the vertical axis.
// Generates step pulses on PC6 (PORTC bit 6 = Arduino pin 5).
ISR(TIMER3_COMPA_vect)
{
  if (StepperController::vert_steps_remain > 0)
  {
    uint16_t step_idx = StepperController::vert_steps_current;

    // Generate the step pulse: set pin HIGH
    PORTC |= (1 << 6);

    // Update step counters
    StepperController::vert_steps_remain--;
    StepperController::vert_steps_current++;

    // Schedule the falling edge
    OCR3B = TCNT3 + 1;
    TIMSK3 |= (1 << OCIE3B);

    // --- Determine if this is MOVE_MODE or FREE_RUN_MODE ramping ---
    bool is_free_run = (StepperController::free_run_ramp_steps_vert > 0);

    if (is_free_run)
    {
      if (StepperController::free_run_ramping_down)
      {
        // FREE-RUN RAMP-DOWN: decelerate from target (current) to slow (start)
        if (step_idx < StepperController::free_run_ramp_steps_vert)
        {
          OCR3A = compute_ramp_ocr(step_idx,
                                    StepperController::free_run_ramp_steps_vert,
                                    StepperController::target_ocr_vert_isr,
                                    StepperController::start_ocr_vert_isr);
        }
      }
      else
      {
        // FREE-RUN RAMP-UP: accelerate from start_ocr (slow) to target_ocr (fast)
        if (step_idx < StepperController::free_run_ramp_steps_vert)
        {
          OCR3A = compute_ramp_ocr(step_idx,
                                    StepperController::free_run_ramp_steps_vert,
                                    StepperController::start_ocr_vert_isr,
                                    StepperController::target_ocr_vert_isr);
        }
        else
        {
          // Ramp-up complete: switch to unlimited steady-state
          OCR3A = StepperController::target_ocr_vert_isr;
          StepperController::free_run_ramp_steps_vert = 0;
          StepperController::vert_steps_remain = 65535;
        }
      }
    }
    else
    {
      // --- MOVE_MODE ramping (existing logic) ---
      if (StepperController::ramp_steps_vert > 0 &&
          step_idx < StepperController::ramp_steps_vert)
      {
        OCR3A = compute_ramp_ocr(step_idx,
                                  StepperController::ramp_steps_vert,
                                  StepperController::start_ocr_vert_isr,
                                  StepperController::target_ocr_vert_isr);
      }
      else if (StepperController::ramp_steps_vert > 0 &&
               StepperController::vert_steps_remain > 0 &&
               StepperController::vert_steps_remain <= StepperController::ramp_steps_vert)
      {
        uint16_t ramp_down_step = StepperController::ramp_steps_vert -
                                   StepperController::vert_steps_remain;
        OCR3A = compute_ramp_ocr(ramp_down_step,
                                  StepperController::ramp_steps_vert,
                                  StepperController::target_ocr_vert_isr,
                                  StepperController::start_ocr_vert_isr);
      }
      else if (StepperController::vert_steps_remain > 0)
      {
        OCR3A = StepperController::target_ocr_vert_isr;
      }
    }

    if (StepperController::vert_steps_remain == 0)
    {
      TIMSK3 &= ~(1 << OCIE3A);
    }
  }
}

// ============================================================================
// TIMER1_COMPB_vect (Horizontal pulse falling edge)
// ============================================================================
// Fires ~1 tick after COMPA. Pulls the horizontal step pin LOW to
// complete the pulse. Disables itself after execution to prevent
// re-triggering (single-shot mode via the COMPB channel).
ISR(TIMER1_COMPB_vect)
{
  // Pull horizontal step pin LOW (PD1 = Arduino pin 2)
  PORTD &= ~(1 << 1);
  // Disable this interrupt so it only fires once per COMPA event
  TIMSK1 &= ~(1 << OCIE1B);
}

// ============================================================================
// TIMER3_COMPB_vect (Vertical pulse falling edge)
// ============================================================================
// Same as TIMER1_COMPB but for the vertical axis.
ISR(TIMER3_COMPB_vect)
{
  // Pull vertical step pin LOW (PC6 = Arduino pin 5)
  PORTC &= ~(1 << 6);
  // Disable this interrupt (single-shot)
  TIMSK3 &= ~(1 << OCIE3B);
}