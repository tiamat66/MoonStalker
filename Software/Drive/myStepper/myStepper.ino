/* MoonStalker

   This SW controls the MoonStalker drive unit.
*/

/*
   Variable definitions
*/
const uint16_t PROGMEM pulse_timings[] = {
  200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200,
  200, 200, 200, 200, 200, 201, 201, 201, 201, 201, 201, 201, 201, 201, 201, 201, 201, 201, 201, 201,
  201, 201, 201, 201, 201, 201, 201, 201, 201, 201, 202, 202, 202, 202, 202, 202, 202, 202, 202, 202,
  202, 202, 202, 202, 202, 202, 202, 202, 202, 202, 202, 202, 202, 202, 203, 203, 203, 203, 203, 203,
  203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 203, 204, 204,
  204, 204, 204, 204, 204, 204, 204, 204, 204, 204, 204, 204, 204, 204, 204, 204, 204, 204, 204, 204,
  204, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205, 205,
  205, 205, 205, 205, 206, 206, 206, 206, 206, 206, 206, 206, 206, 206, 206, 206, 206, 206, 206, 206,
  206, 206, 206, 206, 206, 206, 206, 207, 207, 207, 207, 207, 207, 207, 207, 207, 207, 207, 207, 207,
  207, 207, 207, 207, 207, 207, 207, 207, 207, 208, 208, 208, 208, 208, 208, 208, 208, 208, 208, 208,
  208, 208, 208, 208, 208, 208, 208, 208, 208, 208, 208, 209, 209, 209, 209, 209, 209, 209, 209, 209,
  209, 209, 209, 209, 209, 209, 209, 209, 209, 209, 209, 209, 209, 210, 210, 210, 210, 210, 210, 210,
  210, 210, 210, 210, 210, 210, 210, 210, 210, 210, 210, 210, 210, 210, 211, 211, 211, 211, 211, 211,
  211, 211, 211, 211, 211, 211, 211, 211, 211, 211, 211, 211, 211, 211, 211, 211, 212, 212, 212, 212,
  212, 212, 212, 212, 212, 212, 212, 212, 212, 212, 212, 212, 212, 212, 212, 212, 213, 213, 213, 213,
  213, 213, 213, 213, 213, 213, 213, 213, 213, 213, 213, 213, 213, 213, 213, 213, 213, 214, 214, 214,
  214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 215, 215, 215,
  215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 216, 216, 216,
  216, 216, 216, 216, 216, 216, 216, 216, 216, 216, 216, 216, 216, 216, 216, 216, 216, 217, 217, 217,
  217, 217, 217, 217, 217, 217, 217, 217, 217, 217, 217, 217, 217, 217, 217, 217, 218, 218, 218, 218,
  218, 218, 218, 218, 218, 218, 218, 218, 218, 218, 218, 218, 218, 218, 218, 218, 219, 219, 219, 219,
  219, 219, 219, 219, 219, 219, 219, 219, 219, 219, 219, 219, 219, 219, 220, 220, 220, 220, 220, 220,
  220, 220, 220, 220, 220, 220, 220, 220, 220, 220, 220, 220, 220, 221, 221, 221, 221, 221, 221, 221,
  221, 221, 221, 221, 221, 221, 221, 221, 221, 221, 221, 222, 222, 222, 222, 222, 222, 222, 222, 222,
  222, 222, 222, 222, 222, 222, 222, 222, 222, 222, 223, 223, 223, 223, 223, 223, 223, 223, 223, 223,
  223, 223, 223, 223, 223, 223, 223, 223, 224, 224, 224, 224, 224, 224, 224, 224, 224, 224, 224, 224,
  224, 224, 224, 224, 224, 225, 225, 225, 225, 225, 225, 225, 225, 225, 225, 225, 225, 225, 225, 225,
  225, 225, 225, 226, 226, 226, 226, 226, 226, 226, 226, 226, 226, 226, 226, 226, 226, 226, 226, 226,
  227, 227, 227, 227, 227, 227, 227, 227, 227, 227, 227, 227, 227, 227, 227, 227, 227, 228, 228, 228,
  228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 228, 229, 229, 229, 229, 229, 229,
  229, 229, 229, 229, 229, 229, 229, 229, 229, 229, 230, 230, 230, 230, 230, 230, 230, 230, 230, 230,
  230, 230, 230, 230, 230, 230, 230, 231, 231, 231, 231, 231, 231, 231, 231, 231, 231, 231, 231, 231,
  231, 231, 231, 232, 232, 232, 232, 232, 232, 232, 232, 232, 232, 232, 232, 232, 232, 232, 232, 233,
  233, 233, 233, 233, 233, 233, 233, 233, 233, 233, 233, 233, 233, 233, 234, 234, 234, 234, 234, 234,
  234, 234, 234, 234, 234, 234, 234, 234, 234, 234, 235, 235, 235, 235, 235, 235, 235, 235, 235, 235,
  235, 235, 235, 235, 235, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236, 236,
  237, 237, 237, 237, 237, 237, 237, 237, 237, 237, 237, 237, 237, 237, 237, 238, 238, 238, 238, 238,
  238, 238, 238, 238, 238, 238, 238, 238, 238, 238, 239, 239, 239, 239, 239, 239, 239, 239, 239, 239,
  239, 239, 239, 239, 240, 240, 240, 240, 240, 240, 240, 240, 240, 240, 240, 240, 240, 240, 240, 241,
  241, 241, 241, 241, 241, 241, 241, 241, 241, 241, 241, 241, 241, 242, 242, 242, 242, 242, 242, 242,
  242, 242, 242, 242, 242, 242, 242, 243, 243, 243, 243, 243, 243, 243, 243, 243, 243, 243, 243, 243,
  243, 244, 244, 244, 244, 244, 244, 244, 244, 244, 244, 244, 244, 244, 244, 245, 245, 245, 245, 245,
  245, 245, 245, 245, 245, 245, 245, 245, 246, 246, 246, 246, 246, 246, 246, 246, 246, 246, 246, 246,
  246, 246, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 247, 248, 248, 248, 248, 248,
  248, 248, 248, 248, 248, 248, 248, 248, 249, 249, 249, 249, 249, 249, 249, 249, 249, 249, 249, 249,
  249, 250, 250, 250, 250, 250, 250, 250, 250, 250, 250, 250, 250, 251, 251, 251, 251, 251, 251, 251,
  251, 251, 251, 251, 251, 251, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 252, 253, 253,
  253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 253, 254, 254, 254, 254, 254, 254, 254, 254, 254,
  254, 254, 254, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 256, 256, 256, 256, 256,
  256, 256, 256, 256, 256, 256, 256, 257, 257, 257, 257, 257, 257, 257, 257, 257, 257, 257, 258, 258,
  258, 258, 258, 258, 258, 258, 258, 258, 258, 258, 259, 259, 259, 259, 259, 259, 259, 259, 259, 259,
  259, 260, 260, 260, 260, 260, 260, 260, 260, 260, 260, 260, 260, 261, 261, 261, 261, 261, 261, 261,
  261, 261, 261, 261, 262, 262, 262, 262, 262, 262, 262, 262, 262, 262, 262, 263, 263, 263, 263, 263,
  263, 263, 263, 263, 263, 263, 264, 264, 264, 264, 264, 264, 264, 264, 264, 264, 264, 265, 265, 265,
  265, 265, 265, 265, 265, 265, 265, 266, 266, 266, 266, 266, 266, 266, 266, 266, 266, 266, 267, 267,
  267, 267, 267, 267, 267, 267, 267, 267, 268, 268, 268, 268, 268, 268, 268, 268, 268, 268, 268, 269,
  269, 269, 269, 269, 269, 269, 269, 269, 269, 270, 270, 270, 270, 270, 270, 270, 270, 270, 270, 271,
  271, 271, 271, 271, 271, 271, 271, 271, 271, 272, 272, 272, 272, 272, 272, 272, 272, 272, 272, 273,
  273, 273, 273, 273, 273, 273, 273, 273, 273, 274, 274, 274, 274, 274, 274, 274, 274, 274, 275, 275,
  275, 275, 275, 275, 275, 275, 275, 275, 276, 276, 276, 276, 276, 276, 276, 276, 276, 277, 277, 277,
  277, 277, 277, 277, 277, 277, 277, 278, 278, 278, 278, 278, 278, 278, 278, 278, 279, 279, 279, 279,
  279, 279, 279, 279, 279, 280, 280, 280, 280, 280, 280, 280, 280, 280, 281, 281, 281, 281, 281, 281,
  281, 281, 281, 282, 282, 282, 282, 282, 282, 282, 282, 282, 283, 283, 283, 283, 283, 283, 283, 283,
  283, 284, 284, 284, 284, 284, 284, 284, 284, 284, 285, 285, 285, 285, 285, 285, 285, 285, 286, 286,
  286, 286, 286, 286, 286, 286, 286, 287, 287, 287, 287, 287, 287, 287, 287, 288, 288, 288, 288, 288,
  288, 288, 288, 289, 289, 289, 289, 289, 289, 289, 289, 289, 290, 290, 290, 290, 290, 290, 290, 290,
  291, 291, 291, 291, 291, 291, 291, 291, 292, 292, 292, 292, 292, 292, 292, 292, 293, 293, 293, 293,
  293, 293, 293, 293, 294, 294, 294, 294, 294, 294, 294, 294, 295, 295, 295, 295, 295, 295, 295, 296,
  296, 296, 296, 296, 296, 296, 296, 297, 297, 297, 297, 297, 297, 297, 297, 298, 298, 298, 298, 298,
  298, 298, 299, 299, 299, 299, 299, 299, 299, 299, 300, 300, 300, 300, 300, 300, 300, 301, 301, 301,
  301, 301, 301, 301, 302, 302, 302, 302, 302, 302, 302, 303, 303, 303, 303, 303, 303, 303, 303, 304,
  304, 304, 304, 304, 304, 304, 305, 305, 305, 305, 305, 305, 305, 306, 306, 306, 306, 306, 306, 306,
  307, 307, 307, 307, 307, 307, 307, 308, 308, 308, 308, 308, 308, 309, 309, 309, 309, 309, 309, 309,
  310, 310, 310, 310, 310, 310, 310, 311, 311, 311, 311, 311, 311, 312, 312, 312, 312, 312, 312, 312,
  313, 313, 313, 313, 313, 313, 314, 314, 314, 314, 314, 314, 314, 315, 315, 315, 315, 315, 315, 316,
  316, 316, 316, 316, 316, 316, 317, 317, 317, 317, 317, 317, 318, 318, 318, 318, 318, 318, 319, 319,
  319, 319, 319, 319, 320, 320, 320, 320, 320, 320, 321, 321, 321, 321, 321, 321, 322, 322, 322, 322,
  322, 322, 323, 323, 323, 323, 323, 323, 324, 324, 324, 324, 324, 324, 325, 325, 325, 325, 325, 325,
  326, 326, 326, 326, 326, 326, 327, 327, 327, 327, 327, 328, 328, 328, 328, 328, 328, 329, 329, 329,
  329, 329, 330, 330, 330, 330, 330, 330, 331, 331, 331, 331, 331, 331, 332, 332, 332, 332, 332, 333,
  333, 333, 333, 333, 334, 334, 334, 334, 334, 334, 335, 335, 335, 335, 335, 336, 336, 336, 336, 336,
  337, 337, 337, 337, 337, 338, 338, 338, 338, 338, 338, 339, 339, 339, 339, 339, 340, 340, 340, 340,
  340, 341, 341, 341, 341, 341, 342, 342, 342, 342, 342, 343, 343, 343, 343, 343, 344, 344, 344, 344,
  344, 345, 345, 345, 345, 346, 346, 346, 346, 346, 347, 347, 347, 347, 347, 348, 348, 348, 348, 348,
  349, 349, 349, 349, 350, 350, 350, 350, 350, 351, 351, 351, 351, 351, 352, 352, 352, 352, 353, 353,
  353, 353, 353, 354, 354, 354, 354, 355, 355, 355, 355, 355, 356, 356, 356, 356, 357, 357, 357, 357,
  357, 358, 358, 358, 358, 359, 359, 359, 359, 360, 360, 360, 360, 361, 361, 361, 361, 361, 362, 362,
  362, 362, 363, 363, 363, 363, 364, 364, 364, 364, 365, 365, 365, 365, 366, 366, 366, 366, 367, 367,
  367, 367, 368, 368, 368, 368, 369, 369, 369, 369, 370, 370, 370, 370, 371, 371, 371, 371, 372, 372,
  372, 372, 373, 373, 373, 373, 374, 374, 374, 374, 375, 375, 375, 376, 376, 376, 376, 377, 377, 377,
  377, 378, 378, 378, 378, 379, 379, 379, 380, 380, 380, 380, 381, 381, 381, 381, 382, 382, 382, 383,
  383, 383, 383, 384, 384, 384, 385, 385, 385, 385, 386, 386, 386, 387, 387, 387, 387, 388, 388, 388,
  389, 389, 389, 390, 390, 390, 390, 391, 391, 391, 392, 392, 392, 393, 393, 393, 393, 394, 394, 394,
  395, 395, 395, 396, 396, 396, 397, 397, 397, 397, 398, 398, 398, 399, 399, 399, 400, 400, 400, 401,
  401, 401, 402, 402, 402, 403, 403, 403, 404, 404, 404, 405, 405, 405, 406, 406, 406, 407, 407, 407,
  408, 408, 408, 409, 409, 409, 410, 410, 410, 411, 411, 411, 412, 412, 412, 413, 413, 414, 414, 414,
  415, 415, 415, 416, 416, 416, 417, 417, 417, 418, 418, 419, 419, 419, 420, 420, 420, 421, 421, 422,
  422, 422, 423, 423, 423, 424, 424, 425, 425, 425, 426, 426, 427, 427, 427, 428, 428, 428, 429, 429,
  430, 430, 430, 431, 431, 432, 432, 432, 433, 433, 434, 434, 435, 435, 435, 436, 436, 437, 437, 437,
  438, 438, 439, 439, 440, 440, 440, 441, 441, 442, 442, 443, 443, 443, 444, 444, 445, 445, 446, 446,
  447, 447, 447, 448, 448, 449, 449, 450, 450, 451, 451, 452, 452, 452, 453, 453, 454, 454, 455, 455,
  456, 456, 457, 457, 458, 458, 459, 459, 460, 460, 461, 461, 462, 462, 463, 463, 464, 464, 465, 465,
  466, 466, 467, 467, 468, 468, 469, 469, 470, 470, 471, 471, 472, 472, 473, 473, 474, 474, 475, 475,
  476, 477, 477, 478, 478, 479, 479, 480, 480, 481, 481, 482, 483, 483, 484, 484, 485, 485, 486, 487,
  487, 488, 488, 489, 490, 490, 491, 491, 492, 492, 493, 494, 494, 495, 496, 496, 497, 497, 498, 499,
  499, 500, 500, 501, 502, 502, 503, 504, 504, 505, 506, 506, 507, 508, 508, 509, 509, 510, 511, 511,
  512, 513, 514, 514, 515, 516, 516, 517, 518, 518, 519, 520, 520, 521, 522, 523, 523, 524, 525, 525,
  526, 527, 528, 528, 529, 530, 531, 531, 532, 533, 534, 534, 535, 536, 537, 537, 538, 539, 540, 541,
  541, 542, 543, 544, 545, 545, 546, 547, 548, 549, 550, 550, 551, 552, 553, 554, 555, 555, 556, 557,
  558, 559, 560, 561, 562, 562, 563, 564, 565, 566, 567, 568, 569, 570, 571, 572, 573, 573, 574, 575,
  576, 577, 578, 579, 580, 581, 582, 583, 584, 585, 586, 587, 588, 589, 590, 591, 592, 593, 594, 595,
  597, 598, 599, 600, 601, 602, 603, 604, 605, 606, 607, 609, 610, 611, 612, 613, 614, 615, 617, 618,
  619, 620, 621, 623, 624, 625, 626, 627, 629, 630, 631, 632, 634, 635, 636, 638, 639, 640, 642, 643,
  644, 646, 647, 648, 650, 651, 652, 654, 655, 657, 658, 659, 661, 662, 664, 665, 667, 668, 670, 671,
  673, 674, 676, 677, 679, 681, 682, 684, 685, 687, 689, 690, 692, 694, 695, 697, 699, 700, 702, 704,
  705, 707, 709, 711, 713, 714, 716, 718, 720, 722, 724, 726, 728, 730, 731, 733, 735, 737, 739, 741,
  744, 746, 748, 750, 752, 754, 756, 758, 761, 763, 765, 767, 769, 772, 774, 776, 779, 781, 784, 786,
  788, 791, 793, 796, 798, 801, 804, 806, 809, 811, 814, 817, 820, 822, 825, 828, 831, 834, 837, 840,
  842, 845, 849, 852, 855, 858, 861, 864, 867, 871, 874, 877, 881, 884, 888, 891, 895, 898, 902, 906,
  910, 913, 917, 921, 925, 929, 933, 937, 941, 945, 950, 954, 958, 963, 967, 972, 976, 981, 986, 991,
  996, 1000, 1006, 1011, 1016, 1021, 1026, 1032, 1037, 1043, 1049, 1055, 1060, 1066, 1073, 1079, 1085, 1092, 1098, 1105,
  1112, 1119, 1126, 1133, 1140, 1148, 1155, 1163, 1171, 1179, 1187, 1196, 1204, 1213, 1222, 1231, 1241, 1250, 1260, 1270,
  1281, 1291, 1302, 1313, 1325, 1336, 1348, 1361, 1374, 1387, 1400, 1414, 1428, 1443, 1458, 1474, 1490, 1507, 1524, 1542,
  1561, 1580, 1600, 1621, 1642, 1665, 1688, 1713, 1738, 1765, 1793, 1822, 1853, 1885, 1919, 1955, 1994, 2034, 2077, 2123,
  2172, 2224, 2281, 2342, 2408, 2480, 2559, 2645, 2741, 2848, 2969, 3106, 3263, 3446, 3664, 3928, 4257, 4681, 5257, 6100,
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
const int horiz_fault = 11;
const int vert_fault = 12;

// global variables

// whether horiz and vert step signals
// are currently high
volatile bool horiz_step_high = false;
volatile bool vert_step_high = false;

// how many more steps in this run
// remaining for horiz and vertical
volatile uint16_t horiz_steps_remain = 0;
volatile uint16_t vert_steps_remain = 0;

void setup()
{
  /* Open bluetooth serial port */
  Serial.begin(115200);
  while (!Serial)
  {
    ; // Wait for serial port to connect
  }
  Serial.println("<INFO System start>");
  initialize_pins();
  initialize_timers();
}

void loop()
{
  static char command_buffer[65] = "";
  static int num_recv_char = 0;
  char incoming_char = 0;

  if (Serial.available() > 0)
  {
    if (num_recv_char == 64)
    {
      Serial.println("<FATAL_ERROR RCV_BUFFER_OVERFLOW>");
    }
    incoming_char = Serial.read();
    // ignore newline characters
    if (incoming_char != 10)
    {
      command_buffer[num_recv_char] = incoming_char;
      command_buffer[num_recv_char + 1] = 0;
      num_recv_char++;
      if (incoming_char == '>')
      {
        Serial.print("<INFO Handling command: ");
        Serial.print(command_buffer);
        Serial.println(">");
        handle_incoming_command(command_buffer);

        // clear command buffer, NULL terminate
        *command_buffer = 0;
        num_recv_char = 0;
      }
    }
  }
}


// interrupt routine that pulls horiz
// and/or vert pins high if needed
ISR(TIMER1_COMPA_vect)
{
  if (horiz_steps_remain > 0)
  {
    // set horiz_step_pin 2 high
    PORTD |= B00000010;
    horiz_step_high = true;
    horiz_steps_remain--;
  }

  if (vert_steps_remain > 0)
  {
    // set vert_step_pin 5 high
    PORTC |= B10000000; 
    vert_step_high = true;
    vert_steps_remain--;
  }

  if (horiz_step_high || vert_step_high)
  {
    // set 1 pulse in OCR1B
    OCR1B = 1;
    // unmask COMPB interrupt
    TIMSK1 |= (1 << OCIE1B);
  }
}


// COMPB interrupt is used to
// pull the signal low after high
ISR(TIMER1_COMPB_vect)
{
  if (horiz_step_high)
  {
    // pull the horiz pin 2 low
    PORTD &= B11111101;
  }

  if (vert_step_high)
  {
    // pull the vert pin 5 low
    PORTC &= B01111111;
  }
  // disable OCR1B interrupt
  TIMSK1 &= (1 << OCIE1B);
}


int get_battery_voltage()
{
  uint16_t value = 0;
  uint32_t voltage_mv = 0;

  value = analogRead(battery_voltage_pin);
  // Convert read 10 bit value to 0-5000 mV and
  // multiply with the voltage divider 1k and 3.3k
  // voltage_mv = value * (5000 * 4.3)/ 1023;
  voltage_mv = value * 21;

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

    x_str = strtok(NULL, " ");
    y_str = strtok(NULL, " ");

    x = atoi(x_str);
    y = atoi(y_str);
    Serial.print("<MV_ACK ");
    Serial.print(x);
    Serial.print(y);
    Serial.println(">");

    // TODO - check if we are still moving
    
    // Set step variables for
    // the interrupt routine
    noInterrupts();
    horiz_steps_remain = x;
    vert_steps_remain = y;
    interrupts();
  }
  else if (!strcmp(cmd, "BTRY?"))
  {
    int volt_mv = get_battery_voltage();
    Serial.print("<BTRY ");
    Serial.print(volt_mv);
    Serial.println(" mV>");
  }
  else if (!strcmp(cmd, "ST?"))
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
    Serial.println(ret_msg);
  }
  else if (!strcmp(cmd, "SYS_CHK"))
  {
    // system_check();
    Serial.println("Would execute system check");
  }
  else if (!strcmp(cmd, "MV_STATE"))
  {
    uint16_t x;
    uint16_t y;
    
    noInterrupts();
    x = horiz_steps_remain;
    y = vert_steps_remain;
    Serial.print("<MV_STATE X: ");
    Serial.print(x);
    Serial.print(" Y: ");
    Serial.print(y);
    Serial.println(">");
  }
}

// Check for any error conditions
void system_check()
{
  int battery_volt_mv;

  battery_volt_mv = get_battery_voltage();

  if (battery_volt_mv < BATTERY_LOW_LIMIT_MV)
  {
    Serial.print("<LOW_BTRY>");
  }

  // TODO - Check fault pins from driver
}


/* initialize timers

   initialize timers for stepper
   interrupt generation
   /
*/
void initialize_timers()
{
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
  OCR1A = 125;

  interrupts();
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
  pinMode(horiz_fault, INPUT);
  pinMode(vert_fault, INPUT);

  pinMode(battery_voltage_pin, INPUT_PULLUP);

}
