#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <ncurses.h>
#include <time.h>

#include "moonstalker.h"
#include "control.h"
#include "messages.h"

/* globals */
char message[VTSK_MAX_MSG_LEN];

void wait_until_msg_received(char *msg)
{
   while(1)
   {
      vtsk_bt_receive(message, DBG_MSG_RDY);
      usleep(VTSK_POLL_TIME);
      if(!strcmp(message, msg)) break;
   }
}

void vtsk_mv(int horiz, int vert)
{
   /* <MV A,B> */
   sprintf(message, VTSK_MSG_MV, horiz, vert);
   vtsk_bt_send(message);
   wait_until_msg_received(VTSK_MSG_RDY);
}

void vtsk_st()
{
   /* <ST?> */
   sprintf(message, VTSK_MSG_ST);
   vtsk_bt_send(message);
}
