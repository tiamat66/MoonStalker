step_pin = 2
dir_pin = 3

int delays[] = {5000, 4500, 4000, 3500, 3000, 2500, 2000, 1500, 1000, 500};
int delay_level_num = sizeof(delays)/sizeof(int);

void setup() {
  // put your setup code here, to run once:

}

void loop() {
  // put your main code here, to run repeatedly:

  // accelerate the stepper to max speed
  for(int level = 0; level < 10; level++)
  {
    delay_level = delays[level];
    
    for(int step_num = 0; step_num < 100; step_num++)
    {
      digitalWrite(step_pin, HIGH);
      delayMicroseconds(5);
      digitalWrite(step_pin, LOW);
      delayMicroseconds(delay_level);
    }
  }

  // Run at max speed for a while
  int delay_min = delays[delay_level_num-1];
  for(int step_num = 0; step_num < 10000; step_num++)
  {
    digitalWrite(step_pin, HIGH);
    delayMicroseconds(5);
    digitalWrite(step_pin, LOW);
    delayMicroseconds(delay_min);
  }
  
  // deccelerate the stepper to min speed
  for(int level = 10; level >= 0; level--)
  {
    delay_level = delays[level];
    
    for(int step_num = 0; step_num < 100; step_num++)
    {
      digitalWrite(step_pin, HIGH);
      delayMicroseconds(5);
      digitalWrite(step_pin, LOW);
      delayMicroseconds(delay_level);
    }
  }
}
