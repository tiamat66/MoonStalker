package si.vajnartech.moonstalker.rest;

import android.util.Log;
import android.widget.ArrayAdapter;

import static si.vajnartech.moonstalker.C.TAG;

public class GetConstellationInfo extends GetSkyObjInfo<CharSequence>
{
  public GetConstellationInfo(ArrayAdapter<CharSequence> adapter)
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

    while (lastIndex != -1) {

      lastIndex = txt.indexOf(start, lastIndex);

      if (lastIndex != -1) {
        lastIndex += start.length();
        enci = txt.substring(lastIndex);

        Log.i(TAG, enci.substring(enci.indexOf('>') + 1, enci.indexOf('<')));
        String j = enci.substring(enci.indexOf('>') + 1, enci.indexOf('<'));
        adapter.add(j.replaceAll("\\s+", ""));
      }
    }
    return null;
  }
}
