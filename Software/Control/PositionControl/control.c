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
extern FILE *fp;

/*
 * Get longitude and latitude from GPS module
 */
int get_geo_coordinates(
      double *latitude, 
      double *longitude)
{
   // TODO
   *latitude = LATITUDE;
   *longitude = LONGITUDE;
   
   return(0);
}

/*
 * Send message to low-level controller via BlueTooth
 */
void vtsk_bt_send(char *message)
{
   //TODO: Send via BT
   
   //debug
   fprintf(fp, "%s\n", message);
   printf("%s\n", message);
}

/*
 * Receive message from low-level controller via BlueTooth
 */
void vtsk_bt_receive(char *message, int debug)
{
   //TODO: Receive via BT
   
   //debug
   switch(debug)
   {
      case DBG_MSG_RDY:
         sleep(1);
         sprintf(message, "%s", VTSK_MSG_RDY);
         break;
      case DBG_MSG_NOT_RDY:
         sprintf(message, "%s", VTSK_MSG_NOT_RDY);
         break;
      default:
         break;
   }
   fprintf(fp, "...%s\n", message);
   printf("...%s\n", message);
}
