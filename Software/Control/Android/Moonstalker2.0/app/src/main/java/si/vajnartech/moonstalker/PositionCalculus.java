package si.vajnartech.moonstalker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static java.lang.Math.acos;
import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static si.vajnartech.moonstalker.C.DEF_LATITUDE;
import static si.vajnartech.moonstalker.C.DEF_LONGITUDE;
import static si.vajnartech.moonstalker.C.MINIMUM_DISTANCE;
import static si.vajnartech.moonstalker.C.MINIMUM_TIME;
import static si.vajnartech.moonstalker.C.TAG;

public class PositionCalculus implements LocationListener
{
  private double ra;
  private double dec;
  double az = 0;
  double h = 0;

  Location curLocation = new Location("GPS");

  PositionCalculus(MainActivity act)
  {
    enableGPSService(act);
  }

  void setPosition(AstroObject obj)
  {
    this.ra = obj.ra;
    this.dec = obj.dec;
    curLocation.setLatitude(DEF_LATITUDE);
    curLocation.setLongitude(DEF_LONGITUDE);
  }

  void setPosition(double ra, double dec)
  {
    this.ra = ra;
    this.dec = dec;
    curLocation.setLatitude(DEF_LATITUDE);
    curLocation.setLongitude(DEF_LONGITUDE);
    raDec2AltAz();
  }

  private void enableGPSService(MainActivity act)
  {
    LocationManager locationManager = (LocationManager) act.getSystemService(Context.LOCATION_SERVICE);

    if (locationManager == null)
      Log.d(TAG, "Cannot get the LocationManager");
    else
      Log.d(TAG, "The LocationManager successfully granted");
    if (ContextCompat.checkSelfPermission(act, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(act, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
           PackageManager.PERMISSION_GRANTED)
      if (locationManager != null)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINIMUM_TIME, MINIMUM_DISTANCE, this);
  }

  void altAz2RaDec()
  {
    double A  = toRadians(h);
    double AZ = toRadians(az);
    double L  = toRadians(curLocation.getLatitude());

    double sinD = sin(A) * sin(L) + cos(A) * cos(L) * cos(AZ);
    double D    = asin(sinD);
    double cosH = (sin(A) - sin(L) * sinD) / (cos(L) * cos(D));

    double RA = LST(curLocation.getLongitude()) - acos(cosH);
    dec = toDegrees(D);
    ra = toDegrees(RA);
  }

  void raDec2AltAz()
  {
    Log.i(TAG, "RA=" + convertDec2Hour(ra));
    Log.i(TAG, "DEC=" + convertDec2Hour(dec));

    double RA  = ra * 15.0;
    double DEC = dec;
    double LAT = curLocation.getLatitude();
    double LON = curLocation.getLongitude();
    Log.i(TAG, "LON=" + convertDec2Hour(LON));
    Log.i(TAG, "LAT=" + convertDec2Hour(LAT));
    double LST = LST(LON);
    double HA  = LST - RA;
    if (HA < 0.0) HA += 360.0;
    HA = toRadians(HA);
    DEC = toRadians(DEC);
    LAT = toRadians(LAT);

    // Altitude
    double sinALT = sin(DEC) * sin(LAT) + cos(DEC) * cos(LAT) * cos(HA);
    double ALT    = asin(sinALT);
    h = toDegrees(ALT);

    // Azimuth
    double b1   = sin(DEC) - sin(ALT) * sin(LAT);
    double b2   = cos(ALT) * cos(LAT);
    double cosA = b1 / b2;
    double A    = acos(cosA);
    az = toDegrees(A);
    //If sin(HA) is positive, the angle AZ is 360 - A
    if (sin(HA) > 0.0) az = 360.0 - az;

    Log.i(TAG, "Altitude=" + h + " " + convertDec2Hour(h));
    Log.i(TAG, "Azimuth=" + az + " " + convertDec2Hour(az));
  }

  private static double LST(double longitude)
  {
    double lst  = 100.46 + 0.985647 * j2000() + longitude + 15 * UTC();
    long   lstO = (long) (lst / 360);
    lst = lst - lstO * 360.0;
    if (lst < 0.0) lst += 360.0;
    return lst;
  }

  private static double j2000()
  {
    double y2k = new GregorianCalendar(2000, Calendar.JANUARY, 1, 0, 0).getTimeInMillis();
    return (System.currentTimeMillis() - y2k) / 86400000;
  }

  @SuppressLint("DefaultLocale")
  private static String convertDec2Hour(double num)
  {
    long   hours = (long) num;
    double fPart = num - hours;
    fPart *= 60;
    long   minutes = (long) fPart;
    double seconds = fPart - minutes;
    seconds *= 60;
    return String.format("%d %d\' %.2f\"", hours, minutes, seconds);
  }

  private static double convertHour2Dec(double h, double min, double s)
  {
    if (h < 0.0)
      return (h - (min / 60.0) - (s / 3600.0));
    return (h + (min / 60.0) + (s / 3600.0));
  }

  private static double UTC()
  {
    final int timezone = -2;

    int hour   = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    int minute = Calendar.getInstance().get(Calendar.MINUTE);
    int second = Calendar.getInstance().get(Calendar.SECOND);

    return hour +
           timezone +
           minute / 60.0 +
           second / 3600.0;
  }

  public static double getRaFromString(String s)
  {
    double h = Double.valueOf(s.substring(0, s.indexOf('h')));
    Log.i(TAG, "ddddddddd=" + h);
    double min = Double.valueOf(s.substring(s.indexOf('h') + 1, s.indexOf('m')));
    Log.i(TAG, "ddddddddd=" + min);
    double sec = Double.valueOf(s.substring(s.indexOf('m') + 1, s.length()));
    Log.i(TAG, "ddddddddd=" + sec);

    return convertHour2Dec(h, min, sec);
  }

  public static double getDecFromString(String s)
  {
    double d = Double.valueOf(s.substring(0, s.indexOf('d')));
    Log.i(TAG, "ddddddddd=" + d);
    double min = Double.valueOf(s.substring(s.indexOf('d') + 1, s.indexOf('\'')));
    Log.i(TAG, "ddddddddd=" + min);
    double sec = Double.valueOf(s.substring(s.indexOf('\'') + 1, s.indexOf('\"')));
    Log.i(TAG, "ddddddddd=" + sec);

    return convertHour2Dec(d, min, sec);
  }

  @Override
  public void onLocationChanged(Location location)
  {
    // TODO:
    //curLocation = location;
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras)
  {}

  @Override
  public void onProviderEnabled(String provider)
  {}

  @Override
  public void onProviderDisabled(String provider)
  {}
}
