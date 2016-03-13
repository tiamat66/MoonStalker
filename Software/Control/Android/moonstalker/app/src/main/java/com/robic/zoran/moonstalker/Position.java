package com.robic.zoran.moonstalker;

import java.util.GregorianCalendar;

/**
 * Created by zoran on 7.3.2016.
 */
public class Position {
    GregorianCalendar calendar;

    private static final int VTSK_HOUR = 3600;
    private static final int VTSK_DAY = 86400;
    private static final int VTSK_YEAR = 31536000;
    private static final int RA_OFFSET= 9 * 15;
    //Vernal equinox time
    private static final int VEQ_YEAR = 2015;
    private static final int VEQ_MONTH = 2;
    private static final int VEQ_DAY = 20;
    private static final int VEQ_HOUR = 5;
    private static final int VEQ_MIN = 30;

    //Globe coordinates
    double latitude;
    double longitude;

    //Equatorial coordinates
    double ra;
    double dec;

    //Sferical coordinates
    double d;   //delta->f(dec)         [rad]
    double fi;  //fi   ->f(ra)          [rad]
    double t;   //theta->f(latitude)    [rad]

    //Telescope coordinates
    double azimuth; //[deg]
    double height;  //[deg]

    public Position() {
        calendar = new GregorianCalendar(VEQ_YEAR,VEQ_MONTH, VEQ_DAY, VEQ_HOUR, VEQ_MIN);
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setDec(double dec) {
        this.dec = dec;
    }

    public void setRa(double ra) {
        this.ra = ra;
    }

    public double getAzimuth() {
        return azimuth;
    }

    public double getHeight() {
        return height;
    }

    public double getRa() {
        return ra;
    }

    public double getDec() {
        return dec;
    }

    private void equatorialToSferical()
    {
        double ra_tmp =  ra;

        long seconds;
        double day_modulo_offset;  //The Earth is turning around its own axis
        double year_modulo_offset; //The Earth is turning around the sun
        double tmp1;

        //delta
        d = 90.0 - dec;
        d = Math.toRadians(d);

        //fi
        seconds = getTime() / 1000;
        seconds %= VTSK_DAY;
        day_modulo_offset = (double)seconds / (double)VTSK_HOUR;
        seconds = getTime() / 1000;
        seconds %= VTSK_YEAR;
        year_modulo_offset = ((double)seconds*24.0) / (double)VTSK_YEAR;
        ra_tmp -= day_modulo_offset;
        if(ra_tmp < 0.0) ra_tmp += 24.0;
        ra_tmp -= year_modulo_offset;
        if(ra_tmp < 0.0) ra_tmp += 24.0;
        tmp1 = (ra_tmp * 15.0) + RA_OFFSET;
        if(tmp1 > 360.0) tmp1 -= 360.0;
        tmp1 = 360.0 - tmp1;
        fi = Math.toRadians(tmp1);

        //theta
        t = Math.toRadians(latitude);
    }

    private void alphaToAzimuth(double alpha, double Y)
    {
        azimuth = Math.toDegrees(alpha);

        if(Y < 0.0) {
            azimuth += 180.0;
        }

        if(Y >= 0.0) {
            azimuth += 360.0;
        }

        if(azimuth >= 360.0) {
            azimuth -= 360.0;
        }
    }

    public void equatorialToTelescope() {
        double tmp1, tmp2, tmp3, tmp4;
        double X, Y, Z;
        double alpha, omega;

        equatorialToSferical();
        // alpha-> azimuth
        tmp1 = Math.sin(d) * Math.cos(fi);
        tmp2 = Math.cos(t) * Math.cos(d);
        tmp3 = Math.sin(t) * Math.sin(d) * Math.sin(fi);
        tmp4 = tmp2 - tmp3;
        alpha = Math.atan(tmp1 / tmp4);
        X = tmp1;
        Y = tmp4;
        alphaToAzimuth(alpha,Y);

        // omega -> heigh
        tmp1 = Math.sin(t) * Math.cos(d);
        tmp2 = Math.cos(t) * Math.sin(d) * Math.sin(fi);
        omega = Math.asin(tmp1 + tmp2);
        Z = tmp1 + tmp2;
        height = Math.toDegrees(omega);
    }

    private long getCurrentTime() {
        return(System.currentTimeMillis());
    }

    private long getVernalEquinoxTime() {
        return(calendar.getTimeInMillis());
    }

    public long getTime() {
        return(getCurrentTime() - getVernalEquinoxTime());
    }
}
