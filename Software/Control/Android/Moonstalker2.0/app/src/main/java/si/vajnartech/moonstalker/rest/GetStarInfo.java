package si.vajnartech.moonstalker.rest;

import android.util.Log;

import si.vajnartech.moonstalker.MainActivity;

import static si.vajnartech.moonstalker.C.TAG;

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
    res = parse(data, "Right Ascension and Declination: ", " (epoch 2000.0)");
    Log.i(TAG, "GetSkyObjInfo: " + data);
    if (res.isEmpty())
      return;
    Log.i(TAG, "GetSkyObjInfo: " + res);
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
        return u.substring(u.indexOf(s));
    }
    return "";
  }

  @Override
  protected String parse(String txt, String start)
  {
    return null;
  }
}
