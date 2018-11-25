package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static com.robic.zoran.moonstalker.MSUtil.convertDec2Hour;
import static java.lang.Math.*;

class Position
{
  private static final String TAG = "IZAA";

  private double ra;
  private double dec;
  double az;
  double h;

  private GPSService gpsService;

  Position(GPSService myGpsService)
  {
    gpsService = myGpsService;
  }

  void set(double ra, double dec)
  {
    this.ra =  ra;
    this.dec = dec;
  }

//  "Practical astronomy with your calculator" (Duffett-Smith) gives this formula:
//
//  sinD=sinAsinL+cosAcosLcosAZ
//  cosH=(sinA-sinLsinD)/cosLcosD
//
//  D=declination
//  H=hour angle
//  A=altitude
//  AZ-azimuth
//  L=latitude


  void altAz2RaDec()
  {
    double A  = toRadians(h);
    double AZ = toRadians(az);
    double L = toRadians(gpsService.getLatitude());

    double sinD = sin(A) * sin(L) + cos(A) * cos(L) * cos(AZ);
    double D = asin(sinD);
    double cosH = (sin(A) - sin(L) * sinD) / (cos(L) * cos(D));

    double RA = MSUtil.LST(gpsService.getLongitude()) - acos(cosH);
    dec = toDegrees(D);
    ra = toDegrees(RA);
  }

  void raDec2AltAz()
  {
    Log.i(TAG, "RA=" + convertDec2Hour(ra));
    Log.i(TAG, "DEC=" + convertDec2Hour(dec));

    double RA  = ra * 15.0;
    double DEC = dec;
    double LAT = gpsService.getLatitude();
    double LON = gpsService.getLongitude();
    double LST = MSUtil.LST(LON);
    double HA = LST - RA;
    if (HA < 0.0) HA += 360.0;
    HA  = toRadians(HA);
    DEC = toRadians(DEC);
    LAT = toRadians(LAT);

    // Altitude
    double sinALT = sin(DEC) * sin(LAT) + cos(DEC) * cos(LAT) * cos(HA);
    double ALT = asin(sinALT);
    h = toDegrees(ALT);

    // Azimuth
    double b1 = sin(DEC) - sin(ALT) * sin(LAT);
    double b2 = cos(ALT) * cos(LAT);
    double cosA = b1 / b2;
    double A = acos(cosA);
    az = toDegrees(A);
    //If sin(HA) is positive, the angle AZ is 360 - A
    if (sin(HA) > 0.0) az = 360.0 - az;

    Log.i(TAG, "Altitude=" + h + " " + convertDec2Hour(h));
    Log.i(TAG, "Azimuth=" + az + " " + convertDec2Hour(az));
  }
}
