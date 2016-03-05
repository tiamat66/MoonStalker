#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <ncurses.h>
#include <time.h>

#include "moonstalker.h"

/* Globals */
t_telescope_coordinates    cur_tel_pos;
t_equatorial_coordinates   cur_eq_crds;
double h_steps = 0.0;
double v_steps = 0.0;

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
    int seconds;
    double day_modulo_offset;  //The Earth is turning around its own axis
    double year_modulo_offset; //The Earth is turning around the sun
    double tmp1;
   
    //delta
    out->d = 90.0 - dec;
    DEG_TO_RAD(out->d, out->d);

    //fi
    seconds = vtsk_get_time();
    //printf("Seconds:%d\n", seconds);
    seconds %= VTSK_DAY;
    day_modulo_offset = (double)seconds / (double)VTSK_HOUR;
    seconds = vtsk_get_time();
    seconds %= VTSK_YEAR;
    year_modulo_offset = ((double)seconds*24.0) / (double)VTSK_YEAR;
    ra -= day_modulo_offset;
    if(ra < 0.0) ra += 24.0;
    ra -= year_modulo_offset;
    if(ra < 0.0) ra += 24.0;
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
 *
 * return: number of second since vernal equinox 2015
 */
int vtsk_get_time()
{
   time_t timer;
   struct tm vernal_equinox;
   double seconds;

   vernal_equinox.tm_hour =   EQUINOX_HOUR;
   vernal_equinox.tm_min =    EQUINOX_MIN; 
   vernal_equinox.tm_sec =    EQUINOX_SEC;
   vernal_equinox.tm_year =   EQUINOX_YEAR;
   vernal_equinox.tm_mon =    EQUINOX_MON;
   vernal_equinox.tm_mday =   EQUINOX_DAY;

   /* get current time; same as: timer = time(NULL)  */
   time(&timer);     
   seconds = difftime(timer,mktime(&vernal_equinox));

   return (int)seconds;
}

/* 
 * Calibration
 */
int vtsk_calibration()
{
   char a;
   double lat, lon;

   // The default calibration position is POLARIS
   cur_eq_crds.ra =       RA_POLARIS;
   cur_eq_crds.dec =      DEC_POLARIS;

   // Get geographical coordinates from GPS module
   get_geo_coordinates(&lat, &lon);
   cur_eq_crds.latitude = lat;

   vtsk_equatorial_to_telescope(&cur_eq_crds, &cur_tel_pos);

   return(0);
}

/*
 * Set new equatorial coordinates
 *
 * in:   ra [0..24] 
 *          right accesion, at the vernal equinox in hours
 *       dec [+90..-90]
 *          declination +90 on N pole, -90 S pole in degrees
 *       latitude [+90..-90]
 *          +90 on N pole, -90 on S pole in degrees
 */
void vtsk_set(t_equatorial_coordinates *new_pos)
{
   memcpy(&cur_eq_crds, new_pos, sizeof(t_equatorial_coordinates));
}

/*
 * Move the telescope
 */
int vtsk_move()
{
    t_telescope_coordinates   tel;
    double                    dif_az;
    double                    dif_hi;
    int                       cur_h_steps = 0;
    int                       cur_v_steps = 0;

    vtsk_get_time();
    vtsk_get_time();
    vtsk_get_time();

    //vtsk_print_current();
    vtsk_equatorial_to_telescope(&cur_eq_crds, &tel);
    dif_az = tel.azimuth - cur_tel_pos.azimuth;
    dif_hi = tel.height  - cur_tel_pos.height;
    cur_tel_pos.azimuth = tel.azimuth;
    cur_tel_pos.height = tel.height;
    //vtsk_print_current();

    //printf("dif_az=%lf\n", dif_az);
    //printf("dif_hi=%lf\n", dif_hi);

    h_steps += (dif_az * VTSK_K) / 360.0;
    v_steps += (dif_hi * VTSK_K) / 360.0;

   // printf("h_steps=%lf\n", h_steps);
   // printf("v_steps=%lf\n", v_steps);

    if((abs(h_steps) >= VTSK_PRECISION) ||
          (abs(v_steps) >= VTSK_PRECISION))
    {
       cur_h_steps = (int)h_steps;
       cur_v_steps = (int)v_steps;
       h_steps -= cur_h_steps;
       v_steps -= cur_v_steps;
       vtsk_mv(cur_h_steps, cur_v_steps);
    }

    return(0); 
}

/*
 * Track
 */
void vtsk_track()
{
   int i=0;
   while(1)
   {
      vtsk_move();
      usleep(1000000);
      if(i++ == 60) break;
   }
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


