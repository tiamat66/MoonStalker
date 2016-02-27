/*
 Main header file for moonstalker
*/
#ifndef MOONSTALKER_H
#define MOONSTALKER_H

#define REC 23.0
#define DEC 20.0
#define LAT 45.0

#define DEGREE 0.0174532925
#define PI     3.141592654
#define SEC_TO_HOUR (1.0 / 3600.0)
#define DEG_TO_RAD(rad, deg) rad = deg * DEGREE;
#define RAD_TO_DEG(deg, rad) deg = rad * (1 / DEGREE);

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
    double rec;
    double dec;
    double latitude;
} t_astronomical_coordinates;

int vtsk_astronomical_to_telescope(
        t_astronomical_coordinates *in,
        t_telescope_coordinates    *out);

int vtsk_astronomical_to_sferical(
        t_astronomical_coordinates *in,
        t_sferical_coordinates     *out);

#endif
