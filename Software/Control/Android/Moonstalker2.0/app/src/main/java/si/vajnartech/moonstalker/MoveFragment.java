package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.text.DecimalFormat;
import java.util.Comparator;

import si.vajnartech.moonstalker.rest.GetConstellationInfo;
import si.vajnartech.moonstalker.rest.GetSkyObjInfo;
import si.vajnartech.moonstalker.rest.GetStarInfo;

import static si.vajnartech.moonstalker.C.curObj;

public class MoveFragment extends MyFragment
{
  private Spinner skyObjects;

  static private ArrayAdapter<CharSequence> skyObjAdapter;
  static private ArrayAdapter<CharSequence> constellationAdapter;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    View res = inflater.inflate(R.layout.frag_move, container, false);
    skyObjects = act.findViewById(R.id.sky_object);
    skyObjects.setVisibility(View.VISIBLE);
    act.terminal.show();
    act.terminal.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
    act.findViewById(R.id.logo).setVisibility(View.VISIBLE);
    initAstroObjDropDown();
    setPositionString();
    return res;
  }

  private void scanAstroLine(int position, Spinner sp)
  {
    if (sp == null) return;
    String name = sp.getItemAtPosition(position).toString();
    new GetStarInfo(name, new GetSkyObjInfo.SkyInterface()
    {
      @SuppressWarnings("ConstantConditions")
      @Override
      public void updateConstellation()
      {
        C.calConstellation = getCFromStar();
        setPositionString();
        if (!curObj.name.equals(C.calObj))
          act.terminal.setBackgroundColor(getResources().getColor(R.color.colorAccent));
      }
    });
  }

  private void initAstroObjDropDown()
  {
    skyObjects.setAdapter(skyObjAdapter);

    skyObjects.setSelection(skyObjAdapter.getPosition(curObj.name));

    skyObjects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
    {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
      {
        scanAstroLine(i, skyObjects);
      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView)
      {}
    });
  }

  @SuppressWarnings("ConstantConditions")
  static String getCFromStar()
  {
    int i;
    String buf = C.curConstellation.toLowerCase();
    if (buf.equals("1UrsaeMinoris".toLowerCase()))
      return("Ursa Minor");
    if (buf.contains("16Bo".toLowerCase()))
      return("Bootes");

    for (i = 0; i < constellationAdapter.getCount(); i++) {
      String str = constellationAdapter.getItem(i).toString().toLowerCase();
      if (buf.contains(str)) break;
    }
    if (i == constellationAdapter.getCount())
      return "Error";
    return constellationAdapter.getItem(i).toString();
  }

  static void initAstroObjDatabase(MainActivity ctx)
  {
    skyObjAdapter = ArrayAdapter.createFromResource(ctx, R.array.sky_objects, android.R.layout.simple_spinner_item);
    skyObjAdapter.sort(new Comparator<CharSequence>()
    {
      @Override public int compare(CharSequence charSequence, CharSequence t1)
      {
        return (charSequence.toString().charAt(0) - t1.toString().charAt(0));
      }
    });

    constellationAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item);
    constellationAdapter.sort(new Comparator<CharSequence>()
    {
      @Override public int compare(CharSequence charSequence, CharSequence t1)
      {
        return (charSequence.toString().charAt(0) - t1.toString().charAt(0));
      }
    });
    new GetConstellationInfo(constellationAdapter);
  }

  private String formatPositionString(double azimuth, double height)
  {
    DecimalFormat df = new DecimalFormat("###.##");
    String        az = "A:" + df.format(azimuth);
    String        h  = "H:" + df.format(height);

    return String.format("%s (%s)\n%s | %s", curObj.name, C.calConstellation, az, h);
  }

  public void setPositionString()
  {
    act.terminal.setText(formatPositionString(act.ctrl.az, act.ctrl.h));
  }
}
