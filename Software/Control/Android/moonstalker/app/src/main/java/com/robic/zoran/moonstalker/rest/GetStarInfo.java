package com.robic.zoran.moonstalker.rest;

import android.util.Log;

import com.robic.zoran.moonstalker.AstroObject;
import com.robic.zoran.moonstalker.MSUtil;
import com.robic.zoran.moonstalker.MainActivity;

import static com.robic.zoran.moonstalker.rest.REST.TAG;

public class GetStarInfo extends GetSkyObjInfo
{
  public GetStarInfo(String name, MainActivity act)
  {
    super(name, act, "http://www.stellar-database.com/Scripts/search_star.exe?Name=");
  }

  @Override
  protected void process(String data)
  {
    String res;
    if (data == null)
      return;
    res = parse(data, "Right Ascension and Declination: ",
                     " (epoch 2000.0)");
    Log.i(TAG, "GetSkyObjInfo: " + data);

    if (res.isEmpty())
      return;

    // parse ra and dec from string
    // RightAscensionandDeclination:</B>2h31m48.704s,+89&deg;15'50.72"
    String a = "RightAscensionandDeclination:</B>";
    String b = "s,";
    String c = "s,";
    Log.i(TAG, "GetSkyObjInfo: " + res);


    String j1 = res.substring(a.length(), res.indexOf(b));
    String j2 = res.substring(res.indexOf(c) + 2,
                              res.length()).replaceAll("&deg;", "d");
    Double i1 = MSUtil.getRaFromString(j1);
    Double i2 = MSUtil.getDecFromString(j2);
    act.curObj = new AstroObject(name, i1, i2, j1, j2);

    int q1 = parse1(data, "Proper names:");
    Log.i("IZAA", "OBBBBJJJJJEEEEE=" + act.curObj);
    res = parse1(data, q1 + "</B>".length() + name.length(), "<BR>");
    Log.i("IZAA", "picka ti matrna " + res);
  }

  @Override
  protected String parse(String txt, String start, String end)
  {
    String u = txt.replaceAll("\\s+", "");
    String s = start.replaceAll("\\s+", "");

    if (end != null) {
      String e = end.replaceAll("\\s+", "");
      if (u.contains(s) && u.contains(e)) {
        return u.substring(u.indexOf(s), u.indexOf(e));
      }
    } else {
      if (u.contains(s))
        return u.substring(u.indexOf(s), u.length());
    }
    return "";
  }

  private int parse1(String txt, String start)
  {
    String u = txt.replaceAll("\\s+", "");
    String s = start.replaceAll("\\s+", "");
    return u.indexOf(s) + s.length();
  }

  private String parse1(String txt, int start, String end)
  {
    String u = txt.replaceAll("\\s+", "");
    String e = end.replaceAll("\\s+", "");
    return u.substring(start, u.indexOf(e));
  }

  @Override
  protected String parse(String txt, String start)
  {
    return null;
  }
}
