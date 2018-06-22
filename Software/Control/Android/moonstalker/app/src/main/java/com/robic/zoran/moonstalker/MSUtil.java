package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;

import java.util.Calendar;
import java.util.GregorianCalendar;

class MSUtil
{
  @SuppressLint("DefaultLocale")
  static String convertDec2Hour(double num)
  {
    long hours = (long) num;
    double fPart = num - hours;
    fPart *= 60;
    long minutes = (long) fPart;
    double seconds = fPart - minutes;
    seconds *= 60;
    return String.format("%d %d\' %.2f\"", hours, minutes, seconds);
  }

  static double convertHour2Dec(double h, double min, double s)
  {
    return (h + (min / 60.0) + (s / 3600.0));
  }


//  Local Siderial Time
//  Suppose you have a sunny morning. Put a stick in the ground, and watch the shadow. The shadow will get shorter and shorter - and then start to get longer and longer. The time corresponding to the shortest shadow is your local noon. We reckon a Solar day as (roughly) the mean time between two local noons, and we call this 24 hours of time.
//
//  The stars keep a day which is about 4 minutes shorter than the Solar day. This is because during one day, the Earth moves in its orbit around the Sun, so the Sun has to travel a bit further to reach the next day's noon. The stars do not have to travel that bit further to catch up - so the siderial day is shorter.
//
//  We need to be able to tell time by the stars, and the siderial time can be calculated from a formula which involves the number of days from the epoch J2000. An approximate version of the formula is;
//
//  LST = 100.46 + 0.985647 * d + long + 15*UT
//
//  d    is the days from J2000, including the fraction of
//  a day
//  UT   is the universal time in decimal hours
//  long is your longitude in decimal degrees, East positive.
//
//  Add or subtract multiples of 360 to bring LST in range 0 to 360
//  degrees.
//      and this formula gives your local siderial time in degrees. You can divide by 15 to get your local siderial time in hours, but often we leave the figure in degrees. The approximation is within 0.3 seconds of time for dates within 100 years of J2000.
//  Worked Example for LST
//
//  Find the local siderial time for 2310 UT, 10th August 1998
//  at Birmingham UK (longitude 1 degree 55 minutes west).
//
//  I know that UT = 23.166667
//  d = -508.53472 (last section)
//  long = -1.9166667  (West counts as negative)
//
//  so
//
//      LST = 100.46 + 0.985647 * d + long + 15*UT
//    = 100.46 + 0.985647 * -508.53472 - 1.9166667 + 15 * 23.166667
//        = -55.192383 degrees
//    = 304.80762 degrees
//
//  note how we added 360 to LST to bring the number into the range
//  0 to 360 degrees.

  static double LST(double longitude)
  {
    double lst = 100.46 + 0.985647 * j2000() + longitude + 15 * UTC();
    long lstO = (long) (lst / 360);
    lst = lst - lstO * 360.0;
    if (lst < 0.0) lst += 360.0;
    return lst;
  }

  private static double UTC()
  {
    final int  timezone = -2;

    int hour   = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    int minute = Calendar.getInstance().get(Calendar.MINUTE);
    int second = Calendar.getInstance().get(Calendar.SECOND);

    return hour +
           timezone +
           minute / 60.0 +
           second / 3600.0;
  }

  private static double j2000()
  {
    double y2k = new GregorianCalendar(2000, Calendar.JANUARY, 1, 0, 0).getTimeInMillis();
    return (System.currentTimeMillis() - y2k) / 86400000;
  }
}
