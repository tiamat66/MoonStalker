package si.vajnartech.moonstalker.rest;

import android.util.Log;
import android.widget.ArrayAdapter;

import si.vajnartech.moonstalker.MainActivity;

import static si.vajnartech.moonstalker.C.TAG;

public class GetConstellationInfo extends GetSkyObjInfo
{
  public GetConstellationInfo(Object adapter)
  {
    super("http://www.astro.wisc.edu/~dolan/constellations/constellation_list.html");
    this.adapter = adapter;
  }

  @Override
  protected void process(String data)
  {
    parse(data, "href=\"constellations/");
  }

  @Override
  protected String parse(String txt, String start, String end)
  {
    return null;
  }

  @Override
  protected String parse(String txt, String start)
  {
    int                        lastIndex = 0;
    String                     enci;
    ArrayAdapter<CharSequence> a         = (ArrayAdapter<CharSequence>) adapter;

    while (lastIndex != -1) {

      lastIndex = txt.indexOf(start, lastIndex);

      if (lastIndex != -1) {
        lastIndex += start.length();
        enci = txt.substring(lastIndex);

        Log.i(TAG, enci.substring(enci.indexOf('>') + 1, enci.indexOf('<')));
        String j = enci.substring(enci.indexOf('>') + 1, enci.indexOf('<'));
        a.add(j.replaceAll("\\s+", ""));
      }
    }
    return null;
  }
}
