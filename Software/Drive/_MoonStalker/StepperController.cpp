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
//   sync_mode_enabled - True if set_sync_mode(true) was called.
volatile uint16_t StepperController::ramp_steps_horiz = 0;
volatile uint16_t StepperController::ramp_steps_vert = 0;
volatile uint16_t StepperController::target_ocr_horiz_isr = 0;
volatile uint16_t StepperController::target_ocr_vert_isr = 0;
volatile uint16_t StepperController::start_ocr_horiz_isr = 0;
volatile uint16_t StepperController::start_ocr_vert_isr = 0;
volatile bool StepperController::sync_mode_enabled = true;

// Free-run ramping state:
//   free_run_active_horiz/vert - True from the moment a free run starts on
//       that axis until it has fully stopped. The ISRs branch on this to
//       choose free-run behaviour over MOVE_MODE ramping.
//   free_run_ramp_steps_horiz/vert - Number of steps allocated for the
//       free-run acceleration and deceleration ramps. Cleared to 0 when the
//       ramp-up completes, which is why it cannot double as the
//       "this axis is in free run" flag.
//   free_run_ramping_down - Set to true by free_run_stop() to indicate
//       that the motors should decelerate to a stop.
//   free_run_target_ocr_horiz/vert - The steady-speed OCR value for the
//       free run. Stored separately from target_ocr_horiz_isr so that
//       MOVE_MODE ramping does not interfere with FREE_RUN_MODE.
volatile bool     StepperController::free_run_active_horiz = false;
volatile bool     StepperController::free_run_active_vert = false;
volatile uint16_t StepperController::free_run_ramp_steps_horiz = 0;
volatile uint16_t StepperController::free_run_ramp_steps_vert = 0;
volatile bool     StepperController::free_run_ramping_down = false;
volatile uint16_t StepperController::free_run_target_ocr_horiz = 0;
volatile uint16_t StepperController::free_run_target_ocr_vert = 0;

// Current running mode. Starts IDLE (motors stopped).
volatile RunningMode StepperController::running_mode = RunningMode::IDLE_MODE;

// Step counters and coordinate tracking.
// Initialized to 0; the first free_run_start or move_steppers will set
// step_counters_initialized to true so the ISRs start counting.
volatile uint32_t StepperController::step_counter_x = 0;
volatile uint32_t StepperController::step_counter_y = 0;
volatile int32_t  StepperController::coord_x = 0;
volatile int32_t  StepperController::coord_y = 0;
volatile bool     StepperController::step_counters_initialized = false;

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
//   - Ramping enabled by default (ramp_steps = 100)
// The steps_per_revolution value is used by calculate_ocr_reg_value() to
// convert user-specified RPM into the correct timer compare value.
StepperController::StepperController(int16_t steps_per_revolution)
{
  this->steps_per_revolution = steps_per_revolution;
  running_mode = RunningMode::IDLE_MODE;
  ramp_steps = 100; // Default 100-step ramp for smooth MOVE_MODE accel/decel
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
// set_running_mode_idle (static, ISR-safe)
// ============================================================================
// Transitions the controller to IDLE_MODE. Called from ISR context when
// both axes have completed their step counts.
// This is a static method so it can be called from the ISRs without needing
// a reference to the StepperController instance.
void StepperController::set_running_mode_idle()
{
  running_mode = RunningMode::IDLE_MODE;
}

// ============================================================================
// emergency_stop (static, thread-safe)
// ============================================================================
// Immediately halts both motors, disables all timer interrupts, clears
// step counts and ramp state, pulls step pins LOW, and transitions to IDLE.
// Uses noInterrupts() internally for atomic register access.
void StepperController::emergency_stop()
{
  // Save and restore the global interrupt flag instead of calling
  // interrupts() unconditionally, so this stays safe to call from an ISR.
  uint8_t sreg = SREG;
  noInterrupts();
  horiz_steps_remain = 0;
  vert_steps_remain = 0;
  TIMSK1 &= ~(1 << OCIE1A) & ~(1 << OCIE1B);
  TIMSK3 &= ~(1 << OCIE3A) & ~(1 << OCIE3B);
  PORTD &= ~(1 << 1);
  PORTC &= ~(1 << 6);
  free_run_ramping_down = false;
  free_run_active_horiz = false;
  free_run_active_vert = false;
  free_run_ramp_steps_horiz = 0;
  free_run_ramp_steps_vert = 0;
  running_mode = RunningMode::IDLE_MODE;
  SREG = sreg;
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
  // Only start free run from IDLE_MODE or FREE_RUN_MODE (restart)
  if (running_mode == RunningMode::MOVE_MODE)
  {
    return false;
  }

  // Clear any previous ramp-down state
  free_run_ramping_down = false;

  // Activate step counting
  step_counters_initialized = true;

  // Handle horizontal axis (if not IGNORE)
  if (horiz_direction != StepperDirection::IGNORE)
  {
    horiz_direction_state = horiz_direction;
    digitalWrite(HORIZ_DIR_PIN, (horiz_direction == StepperDirection::CW) ? HIGH : LOW);
    uint16_t ocr_val = calculate_ocr_reg_value(speed_horiz);
    if (ocr_val != 0)
    {
      free_run_target_ocr_horiz = ocr_val;
      horiz_steps_current = 0;

      free_run_active_horiz = true;

      // Ramping is pointless when the requested speed is already slower
      // than the ramp's start speed; starting the ramp would run the motor
      // faster than asked for.
      if (free_run_ramp_steps > 0 && free_run_ramp_steps < 32767 &&
          ocr_val < MAX_OCR_RAMP)
      {
        // Ramp-up: start slow, accelerate over free_run_ramp_steps
        free_run_ramp_steps_horiz = free_run_ramp_steps;
        start_ocr_horiz_isr = MAX_OCR_RAMP;
        target_ocr_horiz_isr = free_run_target_ocr_horiz;
        OCR1A = start_ocr_horiz_isr;
      }
      else
      {
        // No ramp: go directly to target speed
        free_run_ramp_steps_horiz = 0;
        target_ocr_horiz_isr = free_run_target_ocr_horiz;
        OCR1A = ocr_val;
      }
      // Free run is unbounded. The ISR keeps this budget topped up for as
      // long as the run lasts; it is only allowed to count down to zero
      // once free_run_stop() has started the ramp-down.
      horiz_steps_remain = 65535;
      TIMSK1 |= (1 << OCIE1A); // Enable COMPA interrupt
    }
  }
  else
  {
    // Clear stale ramp state from previous runs so free_run_stop()
    // does not falsely detect a phantom ramp for an unused axis.
    free_run_active_horiz = false;
    free_run_ramp_steps_horiz = 0;
    horiz_steps_remain = 0;
    horiz_steps_current = 0;
    TIMSK1 &= ~(1 << OCIE1A);  // ensure timer is off
  }

  // Handle vertical axis (if not IGNORE)
  if (vert_direction != StepperDirection::IGNORE)
  {
    vert_direction_state = vert_direction;
    digitalWrite(VERT_DIR_PIN, (vert_direction == StepperDirection::CW) ? HIGH : LOW);
    uint16_t ocr_val = calculate_ocr_reg_value(speed_vert);
    if (ocr_val != 0)
    {
      free_run_target_ocr_vert = ocr_val;
      vert_steps_current = 0;

      free_run_active_vert = true;

      if (free_run_ramp_steps > 0 && free_run_ramp_steps < 32767 &&
          ocr_val < MAX_OCR_RAMP)
      {
        // Ramp-up: start slow, accelerate over free_run_ramp_steps
        free_run_ramp_steps_vert = free_run_ramp_steps;
        start_ocr_vert_isr = MAX_OCR_RAMP;
        target_ocr_vert_isr = free_run_target_ocr_vert;
        OCR3A = start_ocr_vert_isr;
      }
      else
      {
        // No ramp: go directly to target speed
        free_run_ramp_steps_vert = 0;
        target_ocr_vert_isr = free_run_target_ocr_vert;
        OCR3A = ocr_val;
      }
      vert_steps_remain = 65535;
      TIMSK3 |= (1 << OCIE3A); // Enable COMPA interrupt
    }
  }
  else
  {
    // Clear stale ramp state from previous runs so free_run_stop()
    // does not falsely detect a phantom ramp for an unused axis.
    free_run_active_vert = false;
    free_run_ramp_steps_vert = 0;
    vert_steps_remain = 0;
    vert_steps_current = 0;
    TIMSK3 &= ~(1 << OCIE3A);  // ensure timer is off
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

  // A finite move carries no free-run ramp state to unwind, and the
  // ramp-down path below keys off free_run_active_*, which is false during
  // a move. Stop it outright instead of falling through and marking the
  // controller IDLE while the motors are still stepping.
  if (running_mode != RunningMode::FREE_RUN_MODE)
  {
    emergency_stop();
    return true;
  }

  if (free_run_ramp_steps > 0 && free_run_ramp_steps < 32767)
  {
    // --- Ramp-down setup ---
    // Disable interrupts to prevent the ISR from seeing a partially-updated
    // ramp-down state (e.g., free_run_ramp_steps set but horiz_steps_remain
    // still at 65535, or vice versa).
    noInterrupts();

    // An axis needs a ramp-down exactly when it is currently running.
    // This is evaluated BEFORE any modifications to the ramp state,
    // so we capture the pre-ramp-down condition of each axis.
    bool need_ramp_horiz = free_run_active_horiz;
    bool need_ramp_vert  = free_run_active_vert;

    // Horizontal axis: set up ramp-down steps and target
    if (need_ramp_horiz)
    {
      // Use the current OCR as the starting fast speed for ramp-down.
      uint16_t current_ocr_h = OCR1A;
      // Clamp the computed ramp steps
      uint16_t ramp_steps = free_run_ramp_steps;
      if (ramp_steps > 10000) ramp_steps = 10000;

      if (current_ocr_h >= MAX_OCR_RAMP)
      {
        // Already at or below the speed a ramp would decelerate to, so
        // there is nothing to decelerate from and the axis can stop at
        // once. Ramping here would interpolate toward MAX_OCR_RAMP and so
        // speed the motor UP on its way to stopping.
        free_run_ramp_steps_horiz = 0;
        free_run_active_horiz = false;
        horiz_steps_remain = 0;
        TIMSK1 &= ~(1 << OCIE1A);
      }
      else
      {
        // The steady-state phase uses 65535 steps. Replace with ramp steps
        // so the ISR can count down and stop when done.
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
    }

    // Vertical axis: same logic
    if (need_ramp_vert)
    {
      uint16_t current_ocr_v = OCR3A;
      uint16_t ramp_steps = free_run_ramp_steps;
      if (ramp_steps > 10000) ramp_steps = 10000;

      if (current_ocr_v >= MAX_OCR_RAMP)
      {
        // See the horizontal axis above.
        free_run_ramp_steps_vert = 0;
        free_run_active_vert = false;
        vert_steps_remain = 0;
        TIMSK3 &= ~(1 << OCIE3A);
      }
      else
      {
        free_run_ramp_steps_vert = ramp_steps;
        start_ocr_vert_isr = MAX_OCR_RAMP;
        target_ocr_vert_isr = current_ocr_v;
        vert_steps_remain = ramp_steps;
        vert_steps_current = 0;
      }
    }

    // Now that all ramp-down state is fully configured, atomically flag
    // the ISR to start decelerating (race-safe: no ISR can fire until
    // interrupts() below because we are still inside noInterrupts).
    free_run_ramping_down = true;

    interrupts();

    // If no axis is left running -- either nothing was moving, or every
    // moving axis was slow enough to stop at once -- transition to
    // IDLE_MODE right away. Otherwise the ISRs handle the ramp-down and
    // check_free_run_complete() transitions to IDLE when both finish.
    if (!free_run_active_horiz && !free_run_active_vert)
    {
      free_run_ramping_down = false;
      free_run_ramp_steps_horiz = 0;
      free_run_ramp_steps_vert  = 0;
      running_mode = RunningMode::IDLE_MODE;
    }
  }
  else
  {
    // No ramp: stop immediately via emergency_stop()
    emergency_stop();
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
    // Each ISR clears its own free_run_active_* flag once that axis has
    // run out its ramp-down steps, so both flags clear means both axes
    // have physically stopped.
    if (!free_run_active_horiz && !free_run_active_vert)
    {
      // Both axes have stopped; final cleanup
      noInterrupts();
      TIMSK1 &= ~(1 << OCIE1A) & ~(1 << OCIE1B);
      TIMSK3 &= ~(1 << OCIE3A) & ~(1 << OCIE3B);
      PORTD &= ~(1 << 1);
      PORTC &= ~(1 << 6);
      interrupts();
      free_run_ramping_down = false;
      free_run_ramp_steps_horiz = 0;
      free_run_ramp_steps_vert  = 0;
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

  // --- Guard: reject a move with nothing to do. Entering MOVE_MODE with
  // both step counts at zero would wedge the controller: neither ISR body
  // ever runs, so nothing would ever call set_running_mode_idle() and every
  // later command would be refused with NOT_RDY until <STOP>.
  if (horiz_steps <= 0 && vert_steps <= 0)
  {
    return false;
  }

  // --- Calculate base OCR from RPM
  // Done before any state is touched so an unusable speed leaves the
  // controller exactly as it was. calculate_ocr_reg_value() returns 0 for a
  // non-positive RPM; arming an axis with OCR 0 would match on every timer
  // tick and step at 250 kHz instead of the requested rate.
  uint16_t ocr_h = calculate_ocr_reg_value(speed_horiz);
  uint16_t ocr_v = calculate_ocr_reg_value(speed_vert);

  if ((horiz_steps > 0 && ocr_h == 0) || (vert_steps > 0 && ocr_v == 0))
  {
    return false;
  }

  // Activate step counting
  step_counters_initialized = true;

  // No free run is in progress (we are in IDLE_MODE); make sure the ISRs
  // take the MOVE_MODE path.
  free_run_active_horiz = false;
  free_run_active_vert = false;
  free_run_ramping_down = false;
  free_run_ramp_steps_horiz = 0;
  free_run_ramp_steps_vert = 0;

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

  if (ocr_h != 0) target_ocr_horiz_isr = ocr_h;
  if (ocr_v != 0) target_ocr_vert_isr = ocr_v;

  // --- Sync mode: scale OCR for proportional finish time
  // If one axis has fewer steps, it must run slower so it lasts as
  // long as the axis with more steps.
  // OCR is inversely proportional to speed, so:
  //   scaled_ocr = target_ocr * (max_steps / this_axis_steps)
  uint16_t scaled_ocr_horiz = target_ocr_horiz_isr;
  uint16_t scaled_ocr_vert = target_ocr_vert_isr;
  if (sync_mode_enabled && horiz_steps > 0 && vert_steps > 0)
  {
    uint16_t max_steps = ((uint16_t)horiz_steps > (uint16_t)vert_steps) ?
                          (uint16_t)horiz_steps : (uint16_t)vert_steps;

    // Saturate at the 16-bit register range rather than truncating: a very
    // lopsided step ratio can scale the slower axis past 65535 ticks.
    uint32_t scale_h = (uint32_t)max_steps * 256 / (uint16_t)horiz_steps;
    uint32_t wide_h = ((uint32_t)target_ocr_horiz_isr * scale_h) / 256;
    if (wide_h > 65535UL) wide_h = 65535UL;
    if (wide_h < 1UL) wide_h = 1UL;
    scaled_ocr_horiz = (uint16_t)wide_h;

    uint32_t scale_v = (uint32_t)max_steps * 256 / (uint16_t)vert_steps;
    uint32_t wide_v = ((uint32_t)target_ocr_vert_isr * scale_v) / 256;
    if (wide_v > 65535UL) wide_v = 65535UL;
    if (wide_v < 1UL) wide_v = 1UL;
    scaled_ocr_vert = (uint16_t)wide_v;
  }

  // --- Ramping setup and timer arming, per axis
  // Each axis is configured and armed independently. An axis with no steps
  // to run must be left disarmed: its OCR would otherwise be loaded with a
  // stale value from the previous move, or with 0 on the first move after
  // boot. OCR 0 in CTC mode matches on every timer tick, so the COMPA
  // interrupt would fire at 250 kHz doing no work, starving the main loop
  // and jittering the step timing of the axis that is actually moving.

  // Horizontal ramp: cap at half total steps to leave room for cruise
  if (horiz_steps > 0)
  {
    uint16_t half_steps_h = (uint16_t)horiz_steps / 2;
    ramp_steps_horiz = (ramp_steps < half_steps_h) ? ramp_steps : half_steps_h;

    // Skip the ramp when the target is already slower than the ramp's start
    // speed. Ramping from MAX_OCR_RAMP down to a larger (slower) target OCR
    // would run the axis FASTER than requested for the whole ramp phase --
    // the case for any speed below ~18.75 RPM at 200 steps/rev.
    if (scaled_ocr_horiz >= MAX_OCR_RAMP) ramp_steps_horiz = 0;

    target_ocr_horiz_isr = scaled_ocr_horiz; // sync-aware target
    if (ramp_steps_horiz > 0)
    {
      // Start at the slow ramp speed and accelerate from there
      start_ocr_horiz_isr = MAX_OCR_RAMP;
      OCR1A = start_ocr_horiz_isr;
    }
    else
    {
      // Ramping disabled, or the move is too short for a ramp phase:
      // go straight to the target speed
      OCR1A = scaled_ocr_horiz;
    }
    TIMSK1 |= (1 << OCIE1A);
  }
  else
  {
    ramp_steps_horiz = 0;
    TIMSK1 &= ~(1 << OCIE1A);
  }

  // Vertical ramp: same logic
  if (vert_steps > 0)
  {
    uint16_t half_steps_v = (uint16_t)vert_steps / 2;
    ramp_steps_vert = (ramp_steps < half_steps_v) ? ramp_steps : half_steps_v;

    // See the horizontal axis above.
    if (scaled_ocr_vert >= MAX_OCR_RAMP) ramp_steps_vert = 0;

    target_ocr_vert_isr = scaled_ocr_vert;
    if (ramp_steps_vert > 0)
    {
      start_ocr_vert_isr = MAX_OCR_RAMP;
      OCR3A = start_ocr_vert_isr;
    }
    else
    {
      OCR3A = scaled_ocr_vert;
    }
    TIMSK3 |= (1 << OCIE3A);
  }
  else
  {
    ramp_steps_vert = 0;
    TIMSK3 &= ~(1 << OCIE3A);
  }

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

  // COMPA interrupt enabled by move_steppers/free_run_start when needed

  // Set initial OCR1A (safe default, will be overwritten by first move)
  OCR1A = initial_ocr1a;

  interrupts();
  Serial.print("<INFO Initial OCR1A: ");
  Serial.print(initial_ocr1a);
  Serial.println(">");
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

  // COMPA interrupt enabled by move_steppers/free_run_start when needed

  // Set initial OCR3A
  OCR3A = initial_ocr3a;

  interrupts();
  Serial.print("<INFO Initial OCR3A: ");
  Serial.print(initial_ocr3a);
  Serial.println(">");
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
uint16_t StepperController::calculate_ocr_reg_value(int16_t rpm_speed)
{
  if (rpm_speed <= 0 || steps_per_revolution <= 0)
  {
    return 0; // Error: invalid parameters
  }

  // OCR = 15,000,000 / (RPM * steps_per_revolution)
  // Formula validated: 15,000,000 = (16 MHz / 64 prescaler) * 60 sec/min
  unsigned long divisor = (unsigned long)rpm_speed * (unsigned long)steps_per_revolution;
  unsigned long reg_value = 15000000UL / divisor;

  // Clamp to 16-bit register range
  if (reg_value > 65535) reg_value = 65535;
  if (reg_value < 1) reg_value = 1;

  return (uint16_t)reg_value;
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
  if (progress > 256) progress = 256;
  uint32_t delta = (uint32_t)target_ocr * progress +
                   (uint32_t)start_ocr * (256 - progress);
  uint16_t new_ocr = (uint16_t)(delta / 256);

  // Clamp to the interval this ramp actually spans, so rounding can never
  // push the result past either endpoint. Clamping to a fixed MAX_OCR_RAMP
  // here would be wrong whenever an endpoint lies beyond it: the motor
  // would be driven faster than the slower endpoint asks for.
  uint16_t lo = (start_ocr < target_ocr) ? start_ocr : target_ocr;
  uint16_t hi = (start_ocr < target_ocr) ? target_ocr : start_ocr;
  if (new_ocr < lo) new_ocr = lo;
  if (new_ocr > hi) new_ocr = hi;

  // Never exceed the fastest safe step rate
  if (new_ocr < MIN_OCR_SAFE) new_ocr = MIN_OCR_SAFE;

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

    // Update step counter and coordinate tracking
    // Only count when the counters are initialized (after first start command)
    if (StepperController::step_counters_initialized)
    {
      // Step counter: always positive, cumulative total of steps
      StepperController::step_counter_x++;

      // Coordinates: CW = positive, CCW = negative
      if (StepperController::horiz_direction_state == StepperDirection::CW)
      {
        StepperController::coord_x++;
      }
      else
      {
        StepperController::coord_x--;
      }
    }

    // Schedule the falling edge
    OCR1B = TCNT1 + 1;
    TIMSK1 |= (1 << OCIE1B);

    // --- Determine if this is MOVE_MODE or FREE_RUN_MODE ramping ---
    if (StepperController::free_run_active_horiz)
    {
      if (StepperController::free_run_ramping_down)
      {
        // FREE-RUN RAMP-DOWN: decelerate from target (current) to slow (start)
        // current_step counts forward 0..ramp_steps-1 during ramp-down.
        // steps_remain is finite here and counts down to the stop.
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
        if (StepperController::free_run_ramp_steps_horiz > 0 &&
            step_idx < StepperController::free_run_ramp_steps_horiz)
        {
          // FREE-RUN RAMP-UP: accelerate from start_ocr (slow) to target_ocr
          OCR1A = compute_ramp_ocr(step_idx,
                                    StepperController::free_run_ramp_steps_horiz,
                                    StepperController::start_ocr_horiz_isr,
                                    StepperController::target_ocr_horiz_isr);
        }
        else
        {
          // FREE-RUN STEADY STATE: hold the target speed.
          // Clearing the ramp step count makes this branch sticky, so the
          // wrap-around of the 16-bit step_idx cannot restart the ramp.
          OCR1A = StepperController::target_ocr_horiz_isr;
          StepperController::free_run_ramp_steps_horiz = 0;
        }

        // Re-arm the step budget on every step. Free run is unbounded, so
        // letting the countdown from 65535 expire would silently stop the
        // axis (about 5.5 minutes at 60 RPM with 200 steps/rev). Only the
        // ramp-down above is allowed to run steps_remain down to zero.
        StepperController::horiz_steps_remain = 65535;
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
      StepperController::free_run_ramp_steps_horiz = 0;  // cleanup stale state
      // Signals check_free_run_complete() that this axis has come to rest.
      StepperController::free_run_active_horiz = false;
      TIMSK1 &= ~(1 << OCIE1A);
      // Transition to IDLE_MODE when both axes have completed.
      // The last axis to finish sets the mode. We check if the other axis
      // has also finished by testing its steps_remain (volatile read from ISR-safe static).
      if (StepperController::vert_steps_remain == 0)
      {
        StepperController::set_running_mode_idle();
      }
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

    // Update step counter and coordinate tracking
    // Only count when the counters are initialized (after first start command)
    if (StepperController::step_counters_initialized)
    {
      // Step counter: always positive, cumulative total of steps
      StepperController::step_counter_y++;

      // Coordinates: CW = positive, CCW = negative
      if (StepperController::vert_direction_state == StepperDirection::CW)
      {
        StepperController::coord_y++;
      }
      else
      {
        StepperController::coord_y--;
      }
    }

    // Schedule the falling edge
    OCR3B = TCNT3 + 1;
    TIMSK3 |= (1 << OCIE3B);

    // --- Determine if this is MOVE_MODE or FREE_RUN_MODE ramping ---
    if (StepperController::free_run_active_vert)
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
        if (StepperController::free_run_ramp_steps_vert > 0 &&
            step_idx < StepperController::free_run_ramp_steps_vert)
        {
          // FREE-RUN RAMP-UP: accelerate from start_ocr (slow) to target_ocr
          OCR3A = compute_ramp_ocr(step_idx,
                                    StepperController::free_run_ramp_steps_vert,
                                    StepperController::start_ocr_vert_isr,
                                    StepperController::target_ocr_vert_isr);
        }
        else
        {
          // FREE-RUN STEADY STATE: hold the target speed (sticky, see the
          // horizontal ISR for the reasoning).
          OCR3A = StepperController::target_ocr_vert_isr;
          StepperController::free_run_ramp_steps_vert = 0;
        }

        // Re-arm the step budget; free run is unbounded. See TIMER1_COMPA.
        StepperController::vert_steps_remain = 65535;
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
      StepperController::free_run_ramp_steps_vert = 0;  // cleanup stale state
      // Signals check_free_run_complete() that this axis has come to rest.
      StepperController::free_run_active_vert = false;
      TIMSK3 &= ~(1 << OCIE3A);
      // Transition to IDLE_MODE when both axes have completed.
      if (StepperController::horiz_steps_remain == 0)
      {
        StepperController::set_running_mode_idle();
      }
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