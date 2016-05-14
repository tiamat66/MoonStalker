package com.robic.zoran.moonstalker;

import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by zoran on 7.3.2016.
 */
public class Position {
    GregorianCalendar calendar;
    GregorianCalendar testCalendar;

    private static final String TAG = "Position";
//    double testCnst = 0.28122999999999976;
    double testCnst = 0.3526933333332993;
    double testAlt = 32.60971039;

    //Equatorial coordinates
    double ra;
    double dec;
    double testUtc;

    //Telescope coordinates
    double azimuth; //[deg]
    double height;  //[deg]

    private GPSService gpsService;

    public Position(GPSService myGpsService) {

        gpsService = myGpsService;
        calendar = new GregorianCalendar(2000, Calendar.JANUARY, 1, 0, 0);
        testCalendar = new GregorianCalendar(2016, Calendar.MAY, 14, 15, 54, 37);
        testUtc = 13.91027778;
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

    public void RaDec2AltAz() {

        Log.d(TAG, "RA=" + convertDec2Hour(ra));
        Log.d(TAG, "DEC=" + convertDec2Hour(dec));

        double RA = ra * 15.0;
        double DEC = dec;

        double LAT = gpsService.getLatitude();
        double daysFromY2k = getTime();
        daysFromY2k /= (86400 * 1000);
        daysFromY2k -= testCnst;


        double LST = 100.46 +
                0.985647 * daysFromY2k +
                gpsService.getLongitude() +
                15.0 * getUTC();

        Log.d(TAG, "LST=" + LST);
        Log.d(TAG, "daysFromY2k=" + daysFromY2k);
        Log.d(TAG, "Longitude=" + gpsService.getLongitude());
        Log.d(TAG, "UTC=" + getUTC());

        long a = (long) LST / 360;
        LST = LST -
                a * 360.0;
        if (LST < 0.0) LST += 360.0;

        Log.d(TAG, "LST=" + LST);

        double HA = LST - RA;

        if (HA < 0.0) HA += 360.0;
        Log.d(TAG, "HA=" + HA);

        HA = Math.toRadians(HA);
        DEC = Math.toRadians(DEC);
        LAT = Math.toRadians(LAT);

        // Altitude
        double sinALT = Math.sin(DEC) * Math.sin(LAT) + Math.cos(DEC) * Math.cos(LAT) * Math.cos(HA);
        double ALT = Math.asin(sinALT);
        height = Math.toDegrees(ALT);

        // Azimuth
        double b1 = Math.sin(DEC) - Math.sin(ALT) * Math.sin(LAT);
        double b2 = Math.cos(ALT) * Math.cos(LAT);
        double cosA = b1 / b2;
        double A = Math.acos(cosA);
        Log.d(TAG, "Azimuth=" + A);
        azimuth = Math.toDegrees(A);
        //As sin(HA) is positive, the angle AZ is 360 - A
        if (Math.sin(HA) > 0.0) azimuth = 360.0 - azimuth;


        Log.d(TAG, "Altitude=" + height + " " + convertDec2Hour(height));
        Log.d(TAG, "Azimuth=" + azimuth + " " + convertDec2Hour(azimuth));
//        if(height > testAlt) testCnst -= 0.00001;
//        else testCnst += 0.00001;
//        Log.d(TAG, "constant=" + testCnst);
    }

    private long getCurrentTime() {
        return (System.currentTimeMillis());
//        return (testCalendar.getTimeInMillis());
    }

    private long getVernalEquinoxTime() {
        return (calendar.getTimeInMillis());
    }

    public long getTime() {
        return (getCurrentTime() - getVernalEquinoxTime());
    }

    public double getUTC() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int minute = Calendar.getInstance().get(Calendar.MINUTE);
        int second = Calendar.getInstance().get(Calendar.SECOND);
        Log.d(TAG, "h=" + hour);
        Log.d(TAG, "minute=" + minute);
        Log.d(TAG, "sec=" + second);
        double utc = hour - 2 +
                minute / 60.0 +
                second / 3600.0;
        Log.d(TAG, "UTC=" + utc);
//        return testUtc;
        return utc;
    }

    private String convertDec2Hour(double num) {

        long hours;
        long minutes;
        double seconds;
        double fPart;
        String hour;

        hours = (long) num;
        fPart = num - hours;
        fPart *= 60;
        minutes = (long) fPart;
        seconds = fPart - minutes;
        seconds *= 60;

        hour = String.format("%d %d\' %.2f\"", hours, minutes, seconds);
        return hour;
    }
}
