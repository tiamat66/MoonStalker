// StepperController
//
// Class for controlling both horizontal (azimuth) and
// vertical (altitude) stepper motors on the MoonStalker
// telescope drive unit.
//
// The class provides two movement modes:
//   1. FREE_RUN_MODE  - Continuous rotation at a fixed speed (for manual
//                       guiding via Bluetooth directional commands)
//   2. MOVE_MODE      - Move a specified number of steps at a given RPM,
//                       with optional acceleration ramping and sync finish
//   3. IDLE_MODE      - Motors stopped, ready for next command
//
// Hardware: ATmega32u4 (Arduino Micro)
//   - Horizontal motor driven by Timer1 (16-bit) on pin 2 (PD1)
//   - Vertical motor   driven by Timer3 (16-bit) on pin 5 (PC6)
//   - Timers use CTC mode with 1/64 prescaler (250 kHz tick rate = 4 us/tick)
//   - Step pulses are 1 timer tick wide (~4 us) via COMPB compare match
//
// Speed control:
//   Speed is specified in RPM (revolutions per minute).
//   The OCRxA register is calculated as:
//     OCR = 7,500,000 / (RPM x steps_per_revolution)
//   This gives the half-period in timer ticks at 250 kHz.
//
// Ramping (optional):
//   Smooth acceleration/deceleration by linearly interpolating the OCR
//   value between a slow start value and the target speed over a
//   configurable number of steps.
//
// Sync mode (optional):
//   When enabled, the motor with fewer steps is proportionally slowed
//   so both motors finish their moves simultaneously.

#ifndef STEPPER_CONTROLLER_H
#define STEPPER_CONTROLLER_H

#include <stdint.h>

// ---------------------------------------------------------------------------
// StepperDirection
// ---------------------------------------------------------------------------
// Controls the rotation direction of a stepper motor.
//   CW    - Clockwise rotation
//   CCW   - Counter-clockwise rotation
//   IGNORE - Leave this axis unchanged (used in free_run_start to
//            move only one axis while the other keeps its current state)
enum class StepperDirection
{
  CW,
  CCW,
  IGNORE
};

// ---------------------------------------------------------------------------
// RunningMode
// ---------------------------------------------------------------------------
// Tracks the current operational state of the stepper controller.
//   FREE_RUN_MODE - Continuous rotation (started by MVS command).
//                   The motors run indefinitely until MVE (free_run_stop)
//                   is called. Ramping is applied during start and stop:
//                   - Start: accelerates over free_run_ramp_steps
//                   - Stop:  decelerates over free_run_ramp_steps
//   MOVE_MODE     - Finite-step move (started by MV command).
//                   Motors run for the configured number of steps then
//                   stop. Ramping and sync mode apply in this mode.
//   IDLE_MODE     - Motors are stopped. Only in this state can a new
//                   move_steppers() command be accepted. free_run_start()
//                   is also allowed (replaces previous free run).
enum class RunningMode
{
  FREE_RUN_MODE,
  MOVE_MODE,
  IDLE_MODE
};

// ---------------------------------------------------------------------------
// StepperController class
// ---------------------------------------------------------------------------
class StepperController
{
  public:

  // -------------------------------------------------------------------------
  // Constructor
  // -------------------------------------------------------------------------
  // steps_per_revolution: Number of full steps per motor revolution
  //   (typically 200 for a standard NEMA-17 stepper with 1.8 deg/step).
  //   This value is used in calculate_ocr_reg_value() to convert RPM
  //   into timer compare values.
  StepperController(int16_t steps_per_revolution);

  // -------------------------------------------------------------------------
  // free_run_start
  // -------------------------------------------------------------------------
  // Start continuous rotation of one or both motors at the specified RPM.
  // The motors will run indefinitely until free_run_stop() is called.
  //
  // Use StepperDirection::IGNORE for an axis you don't want to change.
  // For example, to move only North (vertical CW), call:
  //   free_run_start(0, IGNORE, rpm, CW)
  //
  // This mode is used for manual telescope guiding via the MVS command.
  // Returns false if currently in MOVE_MODE (must stop first).
  bool free_run_start(int16_t speed_horiz,
                      StepperDirection horiz_direction,
                      int16_t speed_vert,
                      StepperDirection vert_direction);

  // -------------------------------------------------------------------------
  // free_run_stop
  // -------------------------------------------------------------------------
  // Initiate a ramped stop.
  // Instead of stopping instantly, the motors decelerate smoothly over
  // free_run_ramp_steps steps. Once the ramp-down completes, the controller
  // transitions to IDLE_MODE automatically.
  // If free_run_ramp_steps is 0 the motors stop immediately via
  // emergency_stop().
  // Returns false only when already in IDLE_MODE. Called in MOVE_MODE it
  // stops the move outright via emergency_stop(), since a finite move has
  // no free-run ramp state to unwind. (The <MVE> command handler
  // additionally restricts itself to FREE_RUN_MODE.)
  bool free_run_stop();

  // -------------------------------------------------------------------------
  // check_free_run_complete
  // -------------------------------------------------------------------------
  // Called periodically from the main loop. Detects when a free-run ramp-down
  // has finished and transitions the controller to IDLE_MODE.
  // Without this, free_run_stop() would keep the mode as FREE_RUN even after
  // the motors have stopped ramping down.
  void check_free_run_complete();

  // -------------------------------------------------------------------------
  // set_free_run_ramp_steps
  // -------------------------------------------------------------------------
  // Sets the number of steps used for acceleration when starting a free run
  // and deceleration when stopping (via free_run_stop()).
  // Default is 50 steps, which provides a smooth start/stop without being
  // too slow. Set to 0 to disable ramping in free run mode.
  void set_free_run_ramp_steps(uint16_t steps);

  // -------------------------------------------------------------------------
  // move_steppers
  // -------------------------------------------------------------------------
  // Move both steppers a finite number of steps at the specified RPM.
  // Speed is specified in RPM (revolutions per minute).
  //
  // Parameters:
  //   speed_horiz    - Target speed for the horizontal axis (RPM)
  //   horiz_direction- CW or CCW
  //   horiz_steps    - Number of steps for horizontal axis (0 = no move)
  //   speed_vert     - Target speed for the vertical axis (RPM)
  //   vert_direction - CW or CCW
  //   vert_steps     - Number of steps for vertical axis (0 = no move)
  //
  // Ramping: If ramp_steps was set > 0, speed accelerates from slow to
  //   target over the first ramp_steps, cruises, then decelerates over
  //   the last ramp_steps.
  //
  // Sync: If sync mode is enabled, the axis with fewer steps runs slower
  //   so both axes complete at the same time.
  //
  // Returns false, leaving the controller untouched, if:
  //   - not in IDLE_MODE, or
  //   - both step counts are zero (nothing to do; entering MOVE_MODE would
  //     wedge the controller because no ISR would run to return it to
  //     IDLE_MODE), or
  //   - an axis that has steps was given a speed with no valid timer value
  //     (RPM <= 0).
  bool move_steppers(int16_t speed_horiz,
                     StepperDirection horiz_direction,
                     int16_t horiz_steps,
                     int16_t speed_vert,
                     StepperDirection vert_direction,
                     int16_t vert_steps);

  // -------------------------------------------------------------------------
  // set_ramp_steps
  // -------------------------------------------------------------------------
  // Set the number of steps used for acceleration (ramp up) and
  // deceleration (ramp down) at the start and end of a move.
  // Default is 100 steps; set to 0 to disable ramping.
  //
  // The ramp length is automatically capped to half the total step count
  // so there is always a steady-state cruise phase in between. Ramping is
  // also skipped for an axis whose target speed is already slower than the
  // ramp's start speed, since there would be nothing to accelerate from.
  void set_ramp_steps(uint16_t steps);

  // -------------------------------------------------------------------------
  // set_sync_mode
  // -------------------------------------------------------------------------
  // Enable synchronized finish mode. When enabled, both motors in a
  // move_steppers() call will complete their step counts at the same
  // time. The motor with fewer steps is proportionally slowed:
  //   new_speed = target_speed * (my_steps / max_steps)
  // Set to true (default) for synchronized finish; false for independent per-axis speed control.
  void set_sync_mode(bool enabled);

  // -------------------------------------------------------------------------
  // calculate_ocr_reg_value
  // -------------------------------------------------------------------------
  // Convert RPM to an OCR compare register value for the timer.
  // Formula (with 1/64 prescaler, 16 MHz clock):
  //   OCR = 15,000,000 / (RPM x steps_per_revolution)
  // Returns 0 on error (RPM <= 0 or steps_per_rev <= 0).
  uint16_t calculate_ocr_reg_value(int16_t rpm_speed);

  // -------------------------------------------------------------------------
  // emergency_stop
  // -------------------------------------------------------------------------
  // Immediately stop both motors, disable all timer interrupts, clear step
  // counts, and transition to IDLE_MODE. Safe to call from any context
  // (main loop or ISR): the interrupt enable flag is saved and restored
  // rather than unconditionally re-enabled, so calling this from an ISR
  // does not re-enable interrupts part way through that ISR.
  static void emergency_stop();

  // -------------------------------------------------------------------------
  // get_running_mode
  // -------------------------------------------------------------------------
  // Returns the current running mode (FREE_RUN_MODE, MOVE_MODE, IDLE_MODE).
  // Used by command handlers to check if the system is ready for a new
  // command, and by the DEBUG command for diagnostics.
  RunningMode get_running_mode();

  // -------------------------------------------------------------------------
  // Timer initialization
  // -------------------------------------------------------------------------
  // Configure Timer1 (horizontal) and Timer3 (vertical) for CTC mode
  // with a 1/64 prescaler. Must be called once during setup().
  void initialize_timer1();
  void initialize_timer3();

  // -------------------------------------------------------------------------
  // Static variables (accessible from ISR context)
  // -------------------------------------------------------------------------
  // These must be static and volatile because they are read/written by
  // both the main loop and the interrupt service routines.
  //
  // Steps tracking:
  //   horiz_steps_remain / vert_steps_remain - steps left to execute
  //   horiz_steps_current / vert_steps_current - steps completed so far
  //   horiz_direction_state / vert_direction_state - current direction
  //
  // Ramping state (set by move_steppers, read by ISRs):
  //   ramp_steps_horiz/vert - how many steps the ramp covers
  //   target_ocr_horiz/vert_isr - steady-state speed OCR (sync-aware)
  //   start_ocr_horiz/vert_isr - slow speed OCR at ramp start
  //   sync_mode_enabled - whether sync mode is active
  static volatile uint16_t horiz_steps_remain;
  static volatile uint16_t vert_steps_remain;
  static volatile uint16_t horiz_steps_current;
  static volatile uint16_t vert_steps_current;
  static volatile StepperDirection horiz_direction_state;
  static volatile StepperDirection vert_direction_state;
  static volatile uint16_t ramp_steps_horiz;
  static volatile uint16_t ramp_steps_vert;
  static volatile uint16_t target_ocr_horiz_isr;
  static volatile uint16_t target_ocr_vert_isr;
  static volatile uint16_t start_ocr_horiz_isr;
  static volatile uint16_t start_ocr_vert_isr;
  static volatile bool sync_mode_enabled;

  // Transition the controller to IDLE_MODE.
  // Called from ISR context when both axes have completed their step counts.
  // Must be static to be callable from ISRs (no object pointer available).
  static void set_running_mode_idle();

  // Free-run ramping state
  //   free_run_active_horiz/vert - true for the whole life of a free run on
  //       that axis (ramp-up, steady state and ramp-down). This is what the
  //       ISRs use to select free-run behaviour; free_run_ramp_steps_* must
  //       not be used for that purpose because it is cleared to 0 as soon as
  //       the ramp-up finishes, which would drop the axis into MOVE_MODE
  //       ramping logic for the rest of the run.
  //   free_run_ramp_steps_horiz/vert - steps allocated for free-run ramp,
  //       0 once the ramp-up has completed (steady state)
  //   free_run_ramping_down - true after free_run_stop() initiates ramp-down
  //   free_run_target_ocr_horiz/vert - the steady-speed OCR for free run
  static volatile bool     free_run_active_horiz;
  static volatile bool     free_run_active_vert;
  static volatile uint16_t free_run_ramp_steps_horiz;
  static volatile uint16_t free_run_ramp_steps_vert;
  static volatile bool     free_run_ramping_down;
  static volatile uint16_t free_run_target_ocr_horiz;
  static volatile uint16_t free_run_target_ocr_vert;

  // Current running mode (IDLE, MOVE, FREE_RUN).
  // Must be static and volatile so it can be written from ISR context
  // (via set_running_mode_idle()) and read from the main loop.
  static volatile RunningMode running_mode;

  // -------------------------------------------------------------------------
  // Step counters and coordinate tracking
  // -------------------------------------------------------------------------
  // step_counter_x / step_counter_y:
  //   Cumulative count of all steps executed since system start or last reset.
  //   Always incremented (positive) regardless of direction.
  //   Used by the <STEP_COUNTER?> query command.
  //
  // coord_x / coord_y:
  //   Signed position tracking where positive/negative movements cancel out.
  //   Incremented for CW direction, decremented for CCW direction.
  //   Used by the <COORDS?> query command.
  static volatile uint32_t step_counter_x;
  static volatile uint32_t step_counter_y;
  static volatile int32_t  coord_x;
  static volatile int32_t  coord_y;
  static volatile bool     step_counters_initialized;

  private:

  int16_t     steps_per_revolution;

  // Number of steps for acceleration/deceleration ramp.
  // Set via set_ramp_steps(). 0 = no ramping.
  uint16_t ramp_steps;

  // Number of steps for free run ramp (accel + decel).
  // Set via set_free_run_ramp_steps(). Default 50.
  uint16_t free_run_ramp_steps;
};

#endif

