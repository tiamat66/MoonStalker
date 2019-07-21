#include "StepperController.h"

// Constructor
StepperController::StepperController(int16_t steps_per_revolution)
{
  steps_per_revolution = steps_per_revolution;
}

RunningMode StepperController::get_current_mode()
{
  return running_mode;
}

bool StepperController::free_run_start(int16_t speed_horiz,
                                       StepperDirection horiz_direction,
                                       int16_t speed_vert,
                                       StepperDirection vert_direction)
{
  running_mode = RunningMode::FREE_RUN_MODE;                   
}
 
bool StepperController::free_run_stop(void)
{
  running_mode = RunningMode::IDLE_MODE;
}

bool StepperController::move_steppers(int16_t speed_horiz,
                                      StepperDirection horiz_direction, 
                                      int16_t horiz_steps,
                                      int16_t speed_vert,
                                      StepperDirection vert_direction,
                                      int16_t vert_steps)
{
  running_mode = RunningMode::MOVE_MODE;
}
