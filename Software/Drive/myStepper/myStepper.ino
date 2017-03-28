/* MoonStalker
 * 
 * This SW controls the MoonStalker drive unit.
 */

/* pin numbers */

const int battery_voltage_pin = A0;
const int bluetooth_tx_pin = 1;
const int bluetooth_rx_pin = 0;
int step_pin = 2;
int direction_pin = 3;
int enable_pin = 4;
int direction = HIGH;

void setup()
{
  /* Open bluetooth serial port */
  Serial1.begin(115200);
  while (!Serial)
  {
      ; // Wait for serial port to connect
  }
}

void loop()
{
    handle_input_requests();
    int battery_value = 0;
    battery_value = read_battery_value();
}

int read_battery_value()
{
    int value = 0;

    value = analogRead(battery_voltage_pin);
    return value;
}

/* handle_input_requests
 * 
 * Read requests from bluetooth serial and
 * act on them.
 */
void handle_input_requests()
{
}

