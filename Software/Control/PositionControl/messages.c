#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <ncurses.h>
#include <time.h>

#include "moonstalker.h"
#include "control.h"
#include "messages.h"


void vtsk_mv(int horiz, int vert)
{
   char message[VTSK_MAX_MSG_LEN]
   /* <MV A,B> */
   sprintf(message, VTSK_MSG_MV, horiz, vert);
}
