package si.vajnartech.moonstalker;

import android.annotation.SuppressLint;
import android.location.Location;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static java.lang.Math.acos;
import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static si.vajnartech.moonstalker.C.TAG;

interface TelescopeLocation
{
  Location getCurrentLocation();
}

@SuppressWarnings("unused")
public abstract class PositionCalculus
{
  protected double height;
  protected double azimuth;
  private   double ra;
  private   double dec;

  private final TelescopeLocation location;

  PositionCalculus(TelescopeLocation location)
  {
    this.location = location;
  }

  protected void set(double ra, double dec)
  {
    this.ra = ra;
    this.dec = dec;
    raDec2AltAz();
  }

  void calculatePosition()
  {
    raDec2AltAz();
  }

  private void altAz2RaDec()
  {
    Location curLocation = location.getCurrentLocation();

    double A  = toRadians(height);
    double AZ = toRadians(azimuth);
    double L  = toRadians(curLocation.getLatitude());

    double sinD = sin(A) * sin(L) + cos(A) * cos(L) * cos(AZ);
    double D    = asin(sinD);
    double cosH = (sin(A) - sin(L) * sinD) / (cos(L) * cos(D));

    double RA = LST(curLocation.getLongitude()) - acos(cosH);
    dec = toDegrees(D);
    ra = toDegrees(RA);
  }

  private void raDec2AltAz()
  {
    Location curLocation = location.getCurrentLocation();

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
    height = toDegrees(ALT);

    // Azimuth
    double b1   = sin(DEC) - sin(ALT) * sin(LAT);
    double b2   = cos(ALT) * cos(LAT);
    double cosA = b1 / b2;
    double A    = acos(cosA);
    azimuth = toDegrees(A);
    //If sin(HA) is positive, the angle AZ is 360 - A
    if (sin(HA) > 0.0) azimuth = 360.0 - azimuth;

    Log.i(TAG, "Altitude=" + height + " " + convertDec2Hour(height));
    Log.i(TAG, "Azimuth=" + azimuth + " " + convertDec2Hour(azimuth));
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
    return String.format("%d %d' %.2f\"", hours, minutes, seconds);
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
    double h = Double.parseDouble(s.substring(0, s.indexOf('h')));
    Log.i(TAG, "ddddddddd=" + h);
    double min = Double.parseDouble(s.substring(s.indexOf('h') + 1, s.indexOf('m')));
    Log.i(TAG, "ddddddddd=" + min);
    double sec = Double.parseDouble(s.substring(s.indexOf('m') + 1));
    Log.i(TAG, "ddddddddd=" + sec);

    return convertHour2Dec(h, min, sec);
  }

  public static double getDecFromString(String s)
  {
    double d = Double.parseDouble(s.substring(0, s.indexOf('d')));
    Log.i(TAG, "ddddddddd=" + d);
    double min = Double.parseDouble(s.substring(s.indexOf('d') + 1, s.indexOf('\'')));
    Log.i(TAG, "ddddddddd=" + min);
    double sec = Double.parseDouble(s.substring(s.indexOf('\'') + 1, s.indexOf('\"')));
    Log.i(TAG, "ddddddddd=" + sec);

    return convertHour2Dec(d, min, sec);
  }

  public double getAzimuth()
  {
    return azimuth;
  }

  public double getHeight()
  {
    return height;
  }

  public void move(int vSteps, int hSteps)
  {
    azimuth += (vSteps * 360.0) / C.K;
    height += (hSteps * 360.0) / C.K;
  }
}
