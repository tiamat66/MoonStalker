/* MoonStalker

   This SW controls the MoonStalker drive unit.
*/

/*
   Variable definitions
*/
const PROGMEM uint16_t pulse_timings[] = {
1500, 1492, 1484, 1476, 1468, 1460, 1452, 1444, 1436, 1429, 1421, 1413, 1406, 1398, 1390, 1383, 1375, 1368, 1361, 1353, 
1346, 1339, 1331, 1324, 1317, 1310, 1303, 1296, 1289, 1282, 1275, 1268, 1261, 1254, 1248, 1241, 1234, 1228, 1221, 1214, 
1208, 1201, 1195, 1188, 1182, 1175, 1169, 1163, 1157, 1150, 1144, 1138, 1132, 1126, 1120, 1113, 1107, 1101, 1096, 1090, 
1084, 1078, 1072, 1066, 1060, 1055, 1049, 1043, 1038, 1032, 1027, 1021, 1015, 1010, 1005, 999, 994, 988, 983, 978, 
972, 967, 962, 957, 952, 946, 941, 936, 931, 926, 921, 916, 911, 906, 901, 897, 892, 887, 882, 877, 
873, 868, 863, 858, 854, 849, 845, 840, 836, 831, 827, 822, 818, 813, 809, 804, 800, 796, 791, 787, 
783, 779, 775, 770, 766, 762, 758, 754, 750, 746, 742, 738, 734, 730, 726, 722, 718, 714, 710, 706, 
703, 699, 695, 691, 687, 684, 680, 676, 673, 669, 666, 662, 658, 655, 651, 648, 644, 641, 637, 634, 
630, 627, 624, 620, 617, 614, 610, 607, 604, 600, 597, 594, 591, 588, 584, 581, 578, 575, 572, 569, 
566, 563, 560, 557, 554, 551, 548, 545, 542, 539, 536, 533, 530, 527, 524, 522, 519, 516, 513, 510, 
508, 505, 502, 499, 497, 494, 491, 489, 486, 483, 481, 478, 476, 473, 471, 468, 465, 463, 460, 458, 
455, 453, 451, 448, 446, 443, 441, 439, 436, 434, 431, 429, 427, 424, 422, 420, 418, 415, 413, 411, 
409, 406, 404, 402, 400, 398, 396, 393, 391, 389, 387, 385, 383, 381, 379, 377, 375, 373, 371, 369, 
367, 365, 363, 361, 359, 357, 355, 353, 351, 349, 347, 345, 344, 342, 340, 338, 336, 334, 333, 331, 
329, 327, 326, 324, 322, 320, 319, 317, 315, 313, 312, 310, 308, 307, 305, 303, 302, 300, 298, 297, 
295, 294, 292, 291, 289, 287, 286, 284, 283, 281, 280, 278, 277, 275, 274, 272, 271, 269, 268, 266, 
265, 264, 262, 261, 259, 258, 256, 255, 254, 252, 251, 250, 248, 247, 246, 244, 243, 242, 240, 239, 
238, 236, 235, 234, 233, 231, 230, 229, 228, 226, 225, 224, 223, 222, 220, 219, 218, 217, 216, 214, 
213, 212, 211, 210, 209, 208, 206, 205, 204, 203, 202, 201, 200, 199, 198, 197, 196, 195, 193, 192, 
191, 190, 189, 188, 187, 186, 185, 184, 183, 182, 181, 180, 179, 178, 177, 176, 176, 175, 174, 173, 
172, 171, 170, 169, 168, 167, 166, 165, 164, 164, 163, 162, 161, 160, 159, 158, 157, 157, 156, 155, 
154, 153, 152, 152, 151, 
};

// constants

const int BATTERY_LOW_LIMIT_MV = 10000;

// battery input pin
const int battery_voltage_pin = A0;

// bluetooth serial input and output
const int bluetooth_tx_pin = 1;
const int bluetooth_rx_pin = 0;

// output pins
const int horiz_step_pin = 2;
const int horiz_direction_pin = 3;
const int horiz_reset_pin = 7;
const int horiz_sleep_pin = 8;

const int vert_step_pin = 5;
const int vert_direction_pin = 6;
const int vert_reset_pin = 9;
const int vert_sleep_pin = 10;

// input pins
const int horiz_fault_pin = 11;
const int vert_fault_pin = 12;

// global variables

// how many more steps in this run
// remaining for horiz and vertical
volatile uint16_t horiz_steps_remain = 0;
volatile uint16_t vert_steps_remain = 0;

volatile uint16_t horiz_steps_current = 0;
volatile uint16_t vert_steps_current = 0;

volatile uint16_t pulse_timings_num = sizeof(pulse_timings)/sizeof(uint16_t);

void setup()
{
  /* Open bluetooth serial port */
  Serial1.begin(115200);
  while (!Serial1)
  {
    ; // Wait for serial port to connect
  }
  Serial1.println("<INFO System start>");
  Serial1.print("<INFO pulse_timings_num: ");
  Serial1.print(pulse_timings_num);
  Serial1.println(" >");
  
  initialize_pins();
  initialize_timer1();
  initialize_timer3();
}

void loop()
{
  static char command_buffer[65] = "";
  static int num_recv_char = 0;
  char incoming_char = 0;

  if (Serial1.available() > 0)
  {
    if (num_recv_char == 64)
    {
      Serial1.println(F("<FATAL_ERROR RCV_BUFFER_OVERFLOW>"));
    }
    incoming_char = Serial1.read();
    // ignore newline characters
    if ((incoming_char != 10) && (incoming_char != 13))
    {
      command_buffer[num_recv_char] = incoming_char;
      command_buffer[num_recv_char + 1] = 0;
      num_recv_char++;
      if (incoming_char == '>')
      {
        Serial1.print("<INFO Handling command: ");
        Serial1.print(command_buffer);
        Serial1.println(">");
        handle_incoming_command(command_buffer);

        // clear command buffer, NULL terminate
        *command_buffer = 0;
        num_recv_char = 0;
      }
    }
  }
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
    if ((horiz_steps_current < pulse_timings_num) && (horiz_steps_current < horiz_steps_remain))
    {
      OCR1A = pgm_read_word_near(pulse_timings + horiz_steps_current);
    }
    else if ((horiz_steps_remain < pulse_timings_num) && (horiz_steps_remain < horiz_steps_current))
    {
      OCR1A = pgm_read_word_near(pulse_timings + horiz_steps_remain);
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
    if ((vert_steps_current < pulse_timings_num) && (vert_steps_current < vert_steps_remain))
    {
      OCR3A = pgm_read_word_near(pulse_timings + vert_steps_current);
    }
    else if ((vert_steps_remain < pulse_timings_num) && (vert_steps_remain < vert_steps_current))
    {
      OCR3A = pgm_read_word_near(pulse_timings + vert_steps_remain);
    }
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


int get_battery_voltage()
{
  uint16_t value = 0;
  uint32_t voltage_mv = 0;

  value = analogRead(battery_voltage_pin);
  // Convert read 10 bit value to 0-5000 mV and
  // multiply with the voltage divider 5088R and 14k93
  // voltage_mv = value * (5000 * ((5088 + 14930)/5088)/ 1023;
  voltage_mv = value * 19229 / 1000;

  return voltage_mv;
}


/* handle_incoming_command

   handle command tag that arrived
   over seial

   commands:
   <MV a b>
   <BTRY?>
   <ST?>
   <SYS_CHK>
   <DEBUG>
   <STOP>

*/
void handle_incoming_command(char *command_buff)
{
  char command[65];
  char *cmd;

  // extract command without starting '<'
  // and ending '>'
  strcpy(command, command_buff + 1);
  command[strlen(command) - 1] = 0;

  cmd = strtok(command, " ");
  if (!strcmp(cmd, "MV"))
  {
    char *x_str;
    char *y_str;
    int x, y;
    int x_remain, y_remain;

    // check if we are still moving
    noInterrupts();
    x_remain = horiz_steps_remain;
    y_remain = vert_steps_remain;
    interrupts();
    if ((x_remain > 0) || (y_remain > 0))
    {
      Serial1.println("<NOT_RDY>");
      return;
    }
    x_str = strtok(NULL, " ");
    y_str = strtok(NULL, " ");

    x = atoi(x_str);
    y = atoi(y_str);
    Serial1.print("<MV_ACK ");
    Serial1.print(x);
    Serial1.print(" ");
    Serial1.print(y);
    Serial1.println(">");

    // Set direction for horiz and vert
    if (x < 0)
    {
      digitalWrite(horiz_direction_pin, HIGH);
      x = -x;
    }
    else
    {
      digitalWrite(horiz_direction_pin, LOW);
    }
    if (y < 0)
    {
      digitalWrite(vert_direction_pin, HIGH);
      y = -y;
    }
    else
    {
      digitalWrite(vert_direction_pin, LOW);
    }   
    // Set step variables for
    // the interrupt routine
    // and initialize current values
    noInterrupts();
    horiz_steps_remain = x;
    vert_steps_remain = y;
    horiz_steps_current = 0;
    vert_steps_current = 0;
    interrupts();
  }
  else if (!strcmp(cmd, "BTRY?"))
  {
    int volt_mv = get_battery_voltage();
    Serial1.print("<BTRY ");
    Serial1.print(volt_mv);
    Serial1.println(">");
  }
  else if (!strcmp(cmd, "MVST?"))
  {
    char ret_msg[32];

    if ((horiz_steps_remain > 0) || (vert_steps_remain > 0))
    {
      strcpy(ret_msg, "<NOT_RDY>");
    }
    else
    {
      strcpy(ret_msg, "<RDY>");
    }
    Serial1.println(ret_msg);
  }
  else if (!strcmp(cmd, "SYS_CHK"))
  {
    system_check();
    Serial1.println("Would execute system check");
  }
  else if (!strcmp(cmd, "DEBUG"))
  {
    uint16_t x;
    uint16_t y;
    
    noInterrupts();
    x = horiz_steps_remain;
    y = vert_steps_remain;
    interrupts();
    Serial1.print("<MV_REMAIN X: ");
    Serial1.print(x);
    Serial1.print(" Y: ");
    Serial1.print(y);
    Serial1.println(">");
  }
  else if (!strcmp(cmd, "STOP"))
  {
    noInterrupts();
    horiz_steps_remain = 0;
    vert_steps_remain = 0;
    interrupts();
    Serial1.println("<STOP_ACK>");
  }
}

// Check for any error conditions
void system_check()
{
  int battery_volt_mv;
  int horiz_fault;
  int vert_fault;

  battery_volt_mv = get_battery_voltage();

  if (battery_volt_mv < BATTERY_LOW_LIMIT_MV)
  {
    Serial1.println("<ALARM_LOW_BTRY>");
  }

  // check drv fault pins
  horiz_fault = digitalRead(horiz_fault_pin);
  vert_fault = digitalRead(vert_fault_pin);

  if (horiz_fault == LOW)
  {
    Serial1.println("<ALARM_DRV_HORIZ_FAULT>");
  }

  if (vert_fault == LOW)
  {
    Serial1.println("<ALARM_DRV_VERT_FAULT>");
  }
}


/* initialize timers

   initialize timers for stepper
   interrupt generation
   /
*/
void initialize_timer1()
{
  uint16_t initial_ocr1a = pgm_read_word_near(pulse_timings);
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


void initialize_timer3()
{
  uint16_t initial_ocr3a = pgm_read_word_near(pulse_timings);
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


void initialize_pins()
{
  // output pins horizontal
  pinMode(horiz_step_pin, OUTPUT);
  pinMode(horiz_direction_pin, OUTPUT);
  pinMode(horiz_reset_pin, OUTPUT);
  pinMode(horiz_sleep_pin, OUTPUT);

  // output pins vertical
  pinMode(vert_step_pin, OUTPUT);
  pinMode(vert_direction_pin, OUTPUT);
  pinMode(vert_reset_pin, OUTPUT);
  pinMode(vert_sleep_pin, OUTPUT);

  // input fault pins
  pinMode(horiz_fault_pin, INPUT);
  pinMode(vert_fault_pin, INPUT);

  pinMode(battery_voltage_pin, INPUT_PULLUP);

  // set sleep pins
  digitalWrite(horiz_sleep_pin, HIGH);
  digitalWrite(vert_sleep_pin, HIGH);

  // set reset pins
  digitalWrite(horiz_reset_pin, HIGH);
  digitalWrite(vert_reset_pin, HIGH);
}
