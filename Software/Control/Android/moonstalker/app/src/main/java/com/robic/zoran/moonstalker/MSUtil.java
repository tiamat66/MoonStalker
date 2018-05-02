package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;

public class MSUtil
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
}
