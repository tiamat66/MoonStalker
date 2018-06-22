package com.robic.zoran.moonstalker;

import java.util.Scanner;

class AstroObject
{
  private String name;
  private String ra;
  private String dec;

  AstroObject(String name, String ra, String dec)
  {
    set(name, ra, dec);
  }

  AstroObject()
  {}

  void set(String name, String ra, String dec)
  {
    this.name = name;
    this.ra = ra;
    this.dec = dec;
  }

  double getRa()
  {
    Scanner sc = new Scanner(ra);
    return MSUtil.convertHour2Dec(
        Double.valueOf(sc.next()),
        Double.valueOf(sc.next()),
        Double.valueOf(sc.next()));
  }

  double getDec()
  {
    Scanner sc = new Scanner(dec);
    return MSUtil.convertHour2Dec(
        Double.valueOf(sc.next()),
        Double.valueOf(sc.next()),
        Double.valueOf(sc.next()));
  }
}
