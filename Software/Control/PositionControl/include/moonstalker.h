/*
 Main header file for moonstalker
*/

#ifndef MOONSTALKER_H
#define MOONSTALKER_H

/*
 * Vernal equinox 2015
 * TODO: Check correct vernal equinox date!!!
 */
#define EQUINOX_SEC   0
#define EQUINOX_MIN   0
#define EQUINOX_HOUR  0
#define EQUINOX_DAY  20
#define EQUINOX_MON  2
#define EQUINOX_YEAR 115

#define VTSK_HOUR 3600
#define VTSK_DAY  86400
#define VTSK_YEAR 31536000

#define RA  15.0
#define DEC 0.0

#define VTSK_STEPS 200.0
#define VTSK_I 30.0
#define VTSK_R (48.0 / 14.0)
#define VTSK_K (VTSK_STEPS * VTSK_I * VTSK_R)

#define VTSK_PRECISION 2.0

/*
 * Polaris coordinates
 */
#define RA_POLARIS 0.0
#define DEC_POLARIS 90.0

#define VTSK_WILDCARD 0.0


#define VTSK_FOLLOW_TIME 1000000
#define VTSK_DEBUG printf
#define VTSK_DEBUG_ON 0

#define RA_OFFSET 9*15

#define DEGREE 0.0174532925
#define PI     3.141592654
#define SEC_TO_HOUR_DEBUG (1.0)
#define DEG_TO_RAD(rad, deg) rad = deg * DEGREE;
#define RAD_TO_DEG(deg, rad) deg = rad * (1 / DEGREE);

#define VTSK_DEGREE (1/DEGREE)

typedef struct
{
    double d;   //delta->f(deklinacija)   [rad]
    double fi;  //fi   ->f(rektascenzija) [rad]
    double t;   //theta->f(latitude)      [rad]
} t_sferical_coordinates;

typedef struct
{
    double azimuth; //[deg]
    double height;  //[deg]
} t_telescope_coordinates;

typedef struct
{
    double ra;
    double dec;
    double latitude;
} t_equatorial_coordinates;

/* PROTOTYPES */
int vtsk_equatorial_to_telescope(
        t_equatorial_coordinates *in,
        t_telescope_coordinates  *out);

int vtsk_equatorial_to_sferical(
        t_equatorial_coordinates *in,
        t_sferical_coordinates   *out);

int vtsk_get_time();

int vtsk_calibration();

int vtsk_move();

void vtsk_set(t_equatorial_coordinates *new_pos);

void vtsk_track();

void vtsk_print_current();

#endif
