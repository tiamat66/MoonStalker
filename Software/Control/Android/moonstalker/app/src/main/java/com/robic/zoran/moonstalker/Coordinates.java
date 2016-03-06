package com.robic.zoran.moonstalker;

/**
 * Created by zoran on 6.3.2016.
 */
public class Coordinates {

    //Sferical coordinates
    double d;   //delta->f(dec)      [rad]
    double fi;  //fi   ->f(ra)       [rad]
    double t;   //theta->f(latitude) [rad]

    //Telescope coordinates
    double azimuth; //[deg]
    double height;  //[deg]

    //Equatorial coordinates
    double ra;
    double dec;
    double latitude;
}
