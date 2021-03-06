// StepperController
//
// Class for controlling both horizontal and
// vertical stepper motor.
// Class can run the steppers in a continous
// free run mode, or it can run them with the
// specified number of steps.

#include <stdint.h>

enum class StepperDirection
{
  CW,
  CCW,
  IGNORE
};

enum class RunningMode
{
  FREE_RUN_MODE,
  MOVE_MODE,
  IDLE_MODE
};

class StepperController
{
  public:

  // Constructor
  StepperController(int16_t steps_per_revolution);

  // Start running the motors with the specified
  // speed and direction
  bool free_run_start(int16_t speed_horiz,
                      StepperDirection horiz_direction,
                      int16_t speed_vert,
                      StepperDirection vert_direction);
 
  // Stop both motors
  bool free_run_stop();

  // Move both steppers with the specified speed,
  // direction and number of steps
  // speed is specified in RPM
  bool move_steppers(int16_t speed_horiz,
                     StepperDirection horiz_direction, 
                     int16_t horiz_steps,
                     int16_t speed_vert,
                     StepperDirection vert_direction,
                     int16_t vert_steps);

  // calculation of ocr register value
  // for pulse timing
  int16_t calculate_ocr_reg_value(int16_t rpm_speed);
  
  // Return current running mode
  RunningMode get_running_mode();

  // methods for timer initialization
  void initialize_timer1();
  void initialize_timer3();
  private:
  
  RunningMode running_mode;
  int16_t     steps_per_revolution;
  char        error[256];

};
