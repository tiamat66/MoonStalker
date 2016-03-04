#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <ncurses.h>
#include <time.h>

#include "moonstalker.h"
#include "control.h"

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
   printf("%s\n", message);
}
