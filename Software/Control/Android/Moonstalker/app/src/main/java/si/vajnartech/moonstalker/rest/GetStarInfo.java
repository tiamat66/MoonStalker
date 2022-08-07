package si.vajnartech.moonstalker.rest;


import si.vajnartech.moonstalker.SkyInterface;

import static si.vajnartech.moonstalker.PositionCalculus.getDecFromString;
import static si.vajnartech.moonstalker.PositionCalculus.getRaFromString;

@SuppressWarnings("SameParameterValue")
public class GetStarInfo extends GetSkyObjInfo<CharSequence>
{
  public GetStarInfo(String name, SkyInterface skyInterface)
  {
    super(name, "http://www.stellar-database.com/Scripts/search_star.exe?Name=", skyInterface);
  }

  @Override
  protected void process(String data)
  {
    String res;
    if (data == null)
      return;
    res = parse(data, "Right Ascension and Declination: ", " (epoch 2000.0)");

    if (res.isEmpty())
      return;

    // RightAscension and Declination:</B>2h31m48.704s,+89&deg;15'50.72"
    String a = "RightAscensionandDeclination:</B>";
    String b = "s,";
    String c = "s,";

    String ra  = res.substring(a.length(), res.indexOf(b));
    String dec = res.substring(res.indexOf(c) + 2).replaceAll("&deg;", "d");

    int q1 = parseLocal(data, "Proper names:");
    res = parseLocal(data, q1 + "</B>".length() + name.length(), "<BR>");
    String[] result = res.split(",");


    skyInterface.setObjectProps(name, renameConstellation(result), getRaFromString(ra), getDecFromString(dec));
  }

  private String renameConstellation(String[] result)
  {
    return result.length >= 3 ? result[2] : result[1];
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
    } else if (u.contains(s))
      return u.substring(u.indexOf(s));
    return "";
  }

  private int parseLocal(String txt, String start)
  {
    String u = txt.replaceAll("\\s+", "");
    String s = start.replaceAll("\\s+", "");
    return u.indexOf(s) + s.length();
  }

  private String parseLocal(String txt, int start, String end)
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
