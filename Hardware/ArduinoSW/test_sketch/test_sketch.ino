int pulse_width = 5;
int step_pin = 2;
int dir_pin = 3;

//int delays[] = {5000, 4500, 4000, 3500, 3000, 2500, 2000, 1500, 1000, 500, 250, 200};
//int delays[] = {250};
int min_delay = 50;
int max_delay = 200;
int delay_step = 1;
int steps_at_same_delay = 20;
//int delay_level_num = sizeof(delays)/sizeof(int);
int dir = HIGH;

void setup() {
  // put your setup code here, to run once:
  pinMode(step_pin, OUTPUT);
  pinMode(dir_pin, OUTPUT);
  digitalWrite(dir_pin, dir);
}

void loop() {
  // put your main code here, to run repeatedly:

  // accelerate the stepper to max speed
  for(int delay_us = max_delay; delay_us >= min_delay; delay_us -= delay_step)
  {
    // Serial.print("Spinning with delay level "); Serial.print(delay_level);
    // Serial.print("\n");
    int delay_without_pulse = delay_us - pulse_width;
    
    for(int step_num = 0; step_num < steps_at_same_delay; step_num++)
    {
      digitalWrite(step_pin, HIGH);
      delayMicroseconds(pulse_width);
      digitalWrite(step_pin, LOW);
      delayMicroseconds(delay_without_pulse);
    }
  }

  // Run at max speed for a while
  int delay_without_pulse = min_delay - pulse_width;
  for(long step_num = 0; step_num < 100000; step_num++)
  {
    digitalWrite(step_pin, HIGH);
    delayMicroseconds(pulse_width);
    digitalWrite(step_pin, LOW);
    delayMicroseconds(delay_without_pulse);
  }
  
  // deccelerate the stepper to min speed
  for(int delay_us = min_delay; delay_us <= max_delay; delay_us += delay_step)
  {
    // Serial.print("Spinning with delay level "); Serial.print(delay_level);
    // Serial.print("\n");
    int delay_without_pulse = delay_us - pulse_width;
    for(int step_num = 0; step_num < steps_at_same_delay; step_num++)
    {
      digitalWrite(step_pin, HIGH);
      delayMicroseconds(pulse_width);
      digitalWrite(step_pin, LOW);
      delayMicroseconds(delay_without_pulse);
    }
  }

  delay(1000);
  // Change the direction
  if (dir == HIGH)
    dir = LOW;
  else
    dir = HIGH;
  digitalWrite(dir_pin, dir);
  delay(1000);
}
