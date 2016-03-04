#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <ncurses.h>
#include <time.h>

#include "moonstalker.h"

/* 
 * Globals
 */

int is_calibrated = false;

/*
 * Prints menu with available commands
 */
static 
void vtsk_print_menu()
{
      printf("0-Exit\n");
      printf("1-Calbration\n");
      if(is_calibrated)
      {
         printf("2-Move\n");
         printf("3-Trace\n");
         printf("4-Move to current\n");
      }
}

/*
 * Main
 */
int main(int argc, char **argv)
{
#if !VTSK_DEBUG_ON

   char c;
   t_equatorial_coordinates eq_crds;
   double lat, lon;

   get_geo_coordinates(&lat, &lon);

   do
   {
      vtsk_print_menu();
      vtsk_print_current();
      printf("moonstalker>");
      scanf("%c", &c);
      vtsk_get_time();

      switch(c)
      {
         case '0':
            break;
         case '1':
            printf("Manually move telescope to POLARIS and hit ENTER\n");
            scanf("%c", &c);
            scanf("%c", &c);
            vtsk_calibration();
            printf("Telescope calibrated to POLARIS\n");
            is_calibrated = true;
            break;
         case '2':
            printf("ra=");
            scanf("%lf", &eq_crds.ra);
            printf("dec=");
            scanf("%lf", &eq_crds.dec);
            eq_crds.latitude = lat;
            vtsk_move(&eq_crds);
            break;
         case '3':
            printf("TRACKING MODE\n");
            vtsk_track();
            break;
         case '4':
            vtsk_move_to_current();
            break;
         default:
            break;
      }
   } 
   while (c != '0');














#else

   // Get geographical coordinates from GPS module
   eq_crds.ra =        RA;
   eq_crds.dec =       DEC;
   eq_crds.latitude =  lat;


#if 0  
    t_sferical_coordinates   sf_crds;
    vtsk_equatorial_to_sferical(&eq_crds, &sf_crds);
#endif
#if 0
    t_telescope_coordinates tel_crds;
    vtsk_equatorial_to_telescope(&eq_crds, &tel_crds);
#endif
#if 1
    vtsk_calibration();
#endif
#if 1
    vtsk_move(&eq_crds);
#endif
#if 1
    char c;
    printf("\nHit ENTER to start tracking mode\n");
    scanf("%c", &c);
    vtsk_track();
#endif
#endif

    return(0);
}

