// MoonStalker control software
//
// This software controls custom MoonStalker hardware, that
// in turn drives the stepper motors that turn the telescope.
// Arduino Micro is used as the main board.

// pin numbers
const char RXD         = 3;
const char TXD         = 4;
const char HORIZ_STEP  = 7;
const char HORIZ_DIR   = 8;
const char VERT_STEP   = 10;
const char VERT_DIR    = 11;
const char HORIZ_RESET = 12;
const char HORIZ_SLEEP = 13;
const char VERT_RESET  = 14;
const char VERT_SLEEP  = 15;
const char HORIZ_FAULT = 16;
const char VERT_FAULT  = 17;
const char VBATT       = 21;

// serial speed
const int SERIAL_SPEED = 115200;

// initialize pins
// Set the direction of the pins,
// setup everything else
void initialize_pins() {
    const char unused_pins[] = {9, 22, 23, 24, 25, 26};

    // Set digital pins as input and output
    pinMode(RXD, OUTPUT);
    pinMode(TXD, INPUT);
    pinMode(HORIZ_STEP, OUTPUT);
    pinMode(HORIZ_DIR, OUTPUT);
    pinMode(VERT_STEP, OUTPUT);
    pinMode(VERT_DIR, OUTPUT);
    pinMode(HORIZ_RESET, OUTPUT);
    pinMode(HORIZ_SLEEP, OUTPUT);
    pinMode(VERT_RESET, OUTPUT);
    pinMode(VERT_SLEEP, OUTPUT);
    pinMode(HORIZ_FAULT, INPUT);
    pinMode(VERT_FAULT, INPUT);
    // Set unconnected pins as internal pullup
    for (int i; i < sizeof(unused_pins); i++) {
        pinMode(unused_pins[i], INTERNAP_PULLUP);
    }
}

// Setup the board at the start
void setup() {
    Serial1.begin(SERIAL_SPEED);
    initialize_pins();
}

// Main processor loop
void loop() {
    serial_message_processor();


}
