int step_pin = 2;
int direction_pin = 3;
int enable_pin = 4;
int direction = HIGH;

void setup()
{
  Serial.begin(9600);
  pinMode(step_pin, OUTPUT);
  pinMode(direction_pin, OUTPUT);
  pinMode(enable_pin, OUTPUT);
  digitalWrite(step_pin, LOW);
  digitalWrite(direction_pin, LOW);
  digitalWrite(enable_pin, HIGH);
}

void loop()
{
  while(1)
  {
    Serial.write("Moving\n");
    digitalWrite(enable_pin, LOW);
    digitalWrite(direction_pin, direction);
    for (int j = 0; j <= 2000; j++)
    {
      digitalWrite(step_pin, HIGH);
      delayMicroseconds(1000);
      digitalWrite(step_pin, LOW);
      delayMicroseconds(1000);
    }
    digitalWrite(enable_pin, HIGH);
    Serial.write("Stopped\n");
    delay(2000);
    if (direction == HIGH)
    {
      direction = LOW;
    }
    else
    {
      direction = HIGH;
    }
  }
}
