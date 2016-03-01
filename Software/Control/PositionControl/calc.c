#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <ncurses.h>
#include <time.h>

#include "moonstalker.h"

t_telescope_coordinates    cur_tel_pos;
t_equatorial_coordinates   cur_eq_crds;
double                     cur_time;

/*
 * Converts alpha angle to azimuth coordinate
 * 
 * azimuth: [0..360]
 *          azimuth in degrees, 0 is N, 90 is E, 180 is
 * X, Y, Z, alpha: 
 *          coordinates of sferical system
 */
static 
int vtsk_alpha_to_azimuth(double *azimuth, 
       double alpha, 
       double X, 
       double Y, 
       double Z)
{
   RAD_TO_DEG(*azimuth, alpha);

   if(Y < 0.0) 
      *azimuth += 180.0;

   if(Y >= 0.0)
      *azimuth += 360.0;

   if(*azimuth >= 360.0)
	*azimuth -= 360.0;
   return(0);
}

/* 
 * Converts equatorial to sferical coordinates 
 * 
 * in:   ra [0..24] 
 *          right accesion, at the vernal equinox in hours
 *       dec [+90..-90]
 *          declination +90 on N pole, -90 S pole in degrees
 *       latitude [+90..-90]
 *          +90 on N pole, -90 on S pole in degrees
 * 
 * out:  d  delta->f(dec)      [rad]
 *       fi fi   ->f(ra)       [rad]
 *       t  theta->f(latitude) [rad]
*/

int vtsk_equatorial_to_sferical(
        t_equatorial_coordinates   *in,
        t_sferical_coordinates     *out)
{
    double ra =  in->ra;
    double dec = in->dec;
    double tmp1;
   
    //delta
    out->d = 90.0 - dec;
    DEG_TO_RAD(out->d, out->d);

    //fi
    tmp1 = (ra * 15.0) + RA_OFFSET;
    if(tmp1 > 360.0) tmp1 -= 360.0;
    tmp1 = 360.0 - tmp1;
    DEG_TO_RAD(out->fi, tmp1);

    //theta 
    DEG_TO_RAD(out->t, in->latitude); 

    //debugging
#if 0
    VTSK_DEBUG("vtsk_astronomical_to_sferical:\n");
    VTSK_DEBUG("\tra=%4.1lf, dec=%4.1lf latitude=%4.1lf\n", 
          in->ra, in->dec, in->latitude);
    VTSK_DEBUG("\tdelta=%4.1lf, fi=%4.1lf, theta=%4.1lf\n", 
          out->d*VTSK_DEGREE, out->fi*VTSK_DEGREE, out->t*VTSK_DEGREE);
#endif

    return(0);
}

/* 
 * Converts equatorial to telescope coordinates
 * 
 * in:   ra [0..24] 
 *          right accesion, at the vernal equinox in hours
 *       dec [+90..-90]
 *          declination +90 on N pole, -90 S pole in degrees
 *       latitude [+90..-90]
 *          +90 on N pole, -90 on S pole in degrees
 *
 * out:  
 *       azimuth
 *       height
 */
int vtsk_equatorial_to_telescope(
        t_equatorial_coordinates *in,
        t_telescope_coordinates  *out)
{
   double tmp1, tmp2, tmp3, tmp4;
   double X, Y, Z;
   double alpha, omega;
   t_sferical_coordinates sfr;

   vtsk_equatorial_to_sferical(in, &sfr);

   // alpha-> azimuth
   tmp1 = sin(sfr.d)*cos(sfr.fi);
   tmp2 = cos(sfr.t)*cos(sfr.d);
   tmp3 = sin(sfr.t)*sin(sfr.d)*sin(sfr.fi);
   tmp4 = tmp2 - tmp3;
   alpha = atan(tmp1/tmp4);
   X = tmp1;
   Y = tmp4;

   //debugging
#if 0
   VTSK_DEBUG("vtsk_equatorial_to_telescope:\n");
   VTSK_DEBUG("\tdelta=%4.1lf, fi=%4.1lf, theta=%4.1lf\n", 
          sfr.d*VTSK_DEGREE, sfr.fi*VTSK_DEGREE, sfr.t*VTSK_DEGREE);
   VTSK_DEBUG("\talpha=%4.1lf, omega=%4.1lf\n", 
         alpha*VTSK_DEGREE, omega*VTSK_DEGREE);
#endif
#if 0
   VTSK_DEBUG("X=%4.1lf, Y=%4.1lf ",
         tmp1, tmp4);

#endif

   // omega -> heigh
   tmp1 = sin(sfr.t)*cos(sfr.d);
   tmp2 = cos(sfr.t)*sin(sfr.d)*sin(sfr.fi);
   omega = asin(tmp1 + tmp2); 
   Z = tmp1 + tmp2;
   RAD_TO_DEG(out->height, omega);

   vtsk_alpha_to_azimuth(&out->azimuth, alpha, X, Y, Z);

   // debugging
#if 0
   VTSK_DEBUG("Z=%4.1lf  \n",
         tmp1+tmp2);

#endif

   return(0);
}

/* 
 * Get current time 
 */
double vtsk_get_time()
{
    time_t timer;
    struct tm y2k;
    double seconds;

    y2k.tm_hour = 0;   y2k.tm_min = 0; y2k.tm_sec = 0;
    y2k.tm_year = 100; y2k.tm_mon = 0; y2k.tm_mday = 1;
    time(&timer);  /* get current time; same as: timer = time(NULL)  */
    seconds = difftime(timer,mktime(&y2k));

    return seconds;
}

/* 
 * Calibration
 */
int vtsk_calibration()
{
   char a;

   // The default calibration position is POLARIS
   cur_eq_crds.ra =       RA_POLARIS;
   cur_eq_crds.dec =      DEC_POLARIS;
   cur_eq_crds.latitude = LATITUDE;
   vtsk_equatorial_to_telescope(&cur_eq_crds, &cur_tel_pos);

   return(0);
}

/*
 * Move
 *
 * in:   ra [0..24] 
 *          right accesion, at the vernal equinox in hours
 *       dec [+90..-90]
 *          declination +90 on N pole, -90 S pole in degrees
 *       latitude [+90..-90]
 *          +90 on N pole, -90 on S pole in degrees
 */

int vtsk_move(t_equatorial_coordinates *new_pos)
{
    t_telescope_coordinates   tel;
    double                    dif_az;
    double                    dif_hi;

    vtsk_equatorial_to_telescope(new_pos, &tel);

    dif_az    = tel.azimuth - cur_tel_pos.azimuth;
    dif_hi    = tel.height  - cur_tel_pos.height;

    cur_eq_crds.ra = new_pos->ra;
    cur_eq_crds.dec = new_pos->dec;
    cur_tel_pos.azimuth = tel.azimuth;
    cur_tel_pos.height = tel.height;

    //debugging
#if 0
    VTSK_DEBUG("vtsk_move:\n");
    VTSK_DEBUG("\tra=%lf, dec=%lf\n", 
          new_pos->ra, new_pos->dec);
    VTSK_DEBUG("\tdif_az=%lf, dif_hi=%lf\n",
          dif_az, dif_hi);
#endif
    return(0); 
}

/*
 * Tracking
 */
void vtsk_track()
{
    t_equatorial_coordinates new_eq_crds;
    double                   seconds = 0.0;

    new_eq_crds.ra =       cur_eq_crds.ra;
    new_eq_crds.dec =      cur_eq_crds.dec;
    new_eq_crds.latitude = cur_eq_crds.latitude;
    do
    {
        new_eq_crds.ra -= SEC_TO_HOUR_DEBUG;
	if(new_eq_crds.ra < 0.0) new_eq_crds.ra = 24 + new_eq_crds.ra;
        vtsk_move(&new_eq_crds);
        usleep(VTSK_FOLLOW_TIME);
        seconds++;
        if(seconds >= 24) break;

     // debugging
#if 1
        VTSK_DEBUG("Second: [%d]", (int)seconds);
        vtsk_print_current();
#endif
    } 
    while (1);
}

/*
 * vtsk_print_current
 */

void vtsk_print_current()
{
   VTSK_DEBUG("  Azimuth=%4.3lf, Height=%4.3lf", 
         cur_tel_pos.azimuth, cur_tel_pos.height);
   VTSK_DEBUG("  ra=%4.3lf, dec=%4.3lf\n",
      cur_eq_crds.ra,
      cur_eq_crds.dec);
}
/*
 * Draw
 */
void vtsk_draw()
{
    initscr();                   //Start curses mode
    printw("Hello World !!!");   //Print Hello World
    refresh();                   //Print it on to the real screen
    getch();                     //Wait for user input
    endwin();                    //End curses modev
}

