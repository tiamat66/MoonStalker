package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;

//TODO: Refactor
class Position
{
  private GregorianCalendar calendar;

  private static final String TAG = "IZAA";

  private double ra;
  private double dec;

  double az;
  double h;

  private GPSService gpsService;

  Position(GPSService myGpsService)
  {
    gpsService = myGpsService;
    calendar = new GregorianCalendar(2000, Calendar.JANUARY, 1, 0, 0);
  }

  void set(double ra, double dec)
  {
    this.ra = ra;
    this.dec = dec;
  }

  void RaDec2AltAz()
  {
    Log.i(TAG, "RA=" + convertDec2Hour(ra));
    Log.i(TAG, "DEC=" + convertDec2Hour(dec));

    double RA = ra * 15.0;
    double DEC = dec;

    double LAT = gpsService.getLatitude();
    double daysFromY2k = getTime();
    daysFromY2k /= (86400 * 1000);

    double LST = 100.46 +
      0.985647 * daysFromY2k +
      gpsService.getLongitude() +
      15.0 * getUTC();

    long a = (long) LST / 360;
    LST = LST -
      a * 360.0;
    if (LST < 0.0) LST += 360.0;
    double HA = LST - RA;

    if (HA < 0.0) HA += 360.0;

    HA = Math.toRadians(HA);
    DEC = Math.toRadians(DEC);
    LAT = Math.toRadians(LAT);

    // Altitude
    double sinALT = Math.sin(DEC) * Math.sin(LAT) + Math.cos(DEC) * Math.cos(LAT) * Math.cos(HA);
    double ALT = Math.asin(sinALT);
    h = Math.toDegrees(ALT);

    // Azimuth
    double b1 = Math.sin(DEC) - Math.sin(ALT) * Math.sin(LAT);
    double b2 = Math.cos(ALT) * Math.cos(LAT);
    double cosA = b1 / b2;
    double A = Math.acos(cosA);
    az = Math.toDegrees(A);
    //As sin(HA) is positive, the angle AZ is 360 - A
    if (Math.sin(HA) > 0.0) az = 360.0 - az;

    Log.i(TAG, "Altitude=" + h + " " + convertDec2Hour(h));
    Log.i(TAG, "Azimuth=" + az + " " + convertDec2Hour(az));
  }

  private long getCurrentTime()
  {
    return (System.currentTimeMillis());
  }

  private long getVernalEquinoxTime()
  {
    return (calendar.getTimeInMillis());
  }

  private long getTime()
  {
    return (getCurrentTime() - getVernalEquinoxTime());
  }

  private double getUTC()
  {
    int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    int minute = Calendar.getInstance().get(Calendar.MINUTE);
    int second = Calendar.getInstance().get(Calendar.SECOND);
    double utc = hour - 2 +
      minute / 60.0 +
      second / 3600.0;
    return utc;
  }

  @SuppressLint("DefaultLocale")
  String convertDec2Hour(double num)
  {
    long hours = (long) num;
    double fPart = num - hours;
    fPart *= 60;
    long minutes = (long) fPart;
    double seconds = fPart - minutes;
    seconds *= 60;

    return String.format("%d %d\' %.2f\"", hours, minutes, seconds);
  }
}
