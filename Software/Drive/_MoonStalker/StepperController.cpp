#include "StepperController.h"
#include "Arduino.h"

// how many more steps in this run
// remaining for horiz and vertical
volatile uint16_t horiz_steps_remain = 0;
volatile uint16_t vert_steps_remain = 0;

volatile uint16_t horiz_steps_current = 0;
volatile uint16_t vert_steps_current = 0;

// Constructor
StepperController::StepperController(int16_t steps_per_revolution)
{
  steps_per_revolution = steps_per_revolution;
}

RunningMode StepperController::get_running_mode()
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

void StepperController::initialize_timer1()
{
  uint16_t initial_ocr1a = 100;
  noInterrupts();
  
  TCCR1A = 0;
  TCCR1B = 0;
  TCNT1 = 0;

  // enable CTC mode
  TCCR1B |= (1 << WGM12);
  // set prescaler to 1/64
  TCCR1B |= (1 << CS11) | (1 << CS10);

  // enable interrupt for OCR1A
  TIMSK1 |= (1 << OCIE1A);

  // generate interrupt every
  // 100th timer tick
  
  OCR1A = initial_ocr1a;

  interrupts();
  Serial1.print("Initial ocr1a: ");
  Serial1.println(initial_ocr1a);
}

int16_t StepperController::calculate_ocr_reg_value(int16_t rpm_speed)
{
  // WRONG
  // reg_value = (rpm_speed * steps_per_revolution) * 1000000 / (60 * 4)
  reg_value = (rpm_speed * steps_per_revolution) * 12500 / 3;
}

void StepperController::initialize_timer3()
{
  uint16_t initial_ocr3a = 100;
  noInterrupts();
  
  TCCR3A = 0;
  TCCR3B = 0;
  TCNT3 = 0;

  // enable CTC mode
  TCCR3B |= (1 << WGM32);
  // set prescaler to 1/64
  TCCR3B |= (1 << CS31) | (1 << CS30);

  // enable interrupt for OCR1A
  TIMSK3 |= (1 << OCIE3A);

  // generate interrupt every
  // 100th timer tick
  
  OCR3A = initial_ocr3a;

  interrupts();
  Serial1.print("Initial ocr3a: ");
  Serial1.println(initial_ocr3a);
}

// interrupt routines that pull horiz
// and/or vert pins high if needed
ISR(TIMER1_COMPA_vect)
{ 
  if (horiz_steps_remain > 0)
  {
    // set horiz_step_pin 2 high
    PORTD |= B00000010;
    horiz_steps_remain--;
    horiz_steps_current++;
    // set 1 pulse in OCR1B
    OCR1B = 1;
    // unmask COMPB interrupt
    TIMSK1 |= (1 << OCIE1B);
  }

  // check if we need to prepare
  // next step also
  if (horiz_steps_remain > 0)
  {
    if (1) // TODO - Do the correct thing here
    {
      OCR1A = 100; // TODO - calculate the correct value
    }
    else if (1)
    {
      OCR1A = 100; // TODO - calculate the correct value
    }
  }
}

ISR(TIMER3_COMPA_vect)
{
  if (vert_steps_remain > 0)
  {
    // set vert_step_pin 5 high
    PORTC |= B01000000; 
    vert_steps_remain--;
    vert_steps_current++;
    // set 1 pulse in OCR1B
    OCR3B = 1;
    // unmask COMPB interrupt
    TIMSK3 |= (1 << OCIE3B);
  }
  
  // check if we need to prepare
  // next step also
  if (vert_steps_remain > 0)
  {
    OCR3A = 100; // TODO - Use the correct value
  }
}

// COMPB interrupt is used to
// pull the signal low after high
ISR(TIMER1_COMPB_vect)
{
  // pull the horiz pin 2 low
  PORTD &= B11111101;

  // disable OCR1B interrupt
  TIMSK1 &= ~(1 << OCIE1B);
}


ISR(TIMER3_COMPB_vect)
{
  // pull the vert pin 5 low
  PORTC &= B10111111;
  // disable OCR3B interrupt
  TIMSK3 &= ~(1 << OCIE3B);
}
