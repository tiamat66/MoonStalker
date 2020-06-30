package si.vajnartech.moonstalker.rest;

import android.util.Log;

import si.vajnartech.moonstalker.AstroObject;
import si.vajnartech.moonstalker.C;

import static si.vajnartech.moonstalker.PositionCalculus.getDecFromString;
import static si.vajnartech.moonstalker.PositionCalculus.getRaFromString;

@SuppressWarnings("SameParameterValue")
public class GetStarInfo extends GetSkyObjInfo<CharSequence>
{
  private SkyInterface skyInterface;

  public GetStarInfo(String name, SkyInterface i)
  {
    super(name, "http://www.stellar-database.com/Scripts/search_star.exe?Name=");
    skyInterface = i;
  }

  @Override
  protected void process(String data)
  {
    String res;
    if (data == null)
      return;
    res = parse(data, "Right Ascension and Declination: ", " (epoch 2000.0)");
    Log.i(C.TAG, "GetSkyObjInfo: " + data);

    if (res.isEmpty())
      return;

    // RightAscension and Declination:</B>2h31m48.704s,+89&deg;15'50.72"
    String a = "RightAscensionandDeclination:</B>";
    String b = "s,";
    String c = "s,";
    Log.i(C.TAG, "GetSkyObjInfo: " + res);


    String j1 = res.substring(a.length(), res.indexOf(b));
    String j2 = res.substring(res.indexOf(c) + 2).replaceAll("&deg;", "d");
    double i1 = getRaFromString(j1);
    double i2 = getDecFromString(j2);
    C.curObj = new AstroObject(name, i1, i2, j1, j2);

    int q1 = parse1(data, "Proper names:");
    Log.i(C.TAG, "obj: " + C.curObj);
    res = parse1(data, q1 + "</B>".length() + name.length(), "<BR>");
    String[] result = res.split(",");
    Log.i(C.TAG, "result: " + res);

    C.curConstellation = result.length >= 3 ? result[2] : result[1];
    Log.i(C.TAG, "current constalation = " + C.curConstellation);
    Log.i(C.TAG, "current object = " + C.curObj);
    if (skyInterface != null)
      skyInterface.updateConstellation();
  }

  @Override
  protected String parse(String txt, String start, String end)
  {
    String u = txt.replaceAll("\\s+", "");
    String s = start.replaceAll("\\s+", "");

    if (end != null) {
      String e = end.replaceAll("\\s+", "");
      if (u.contains(s) && u.contains(e))
        return u.substring(u.indexOf(s), u.indexOf(e));
    } else
      if (u.contains(s))
        return u.substring(u.indexOf(s));
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
