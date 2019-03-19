package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Comparator;

import si.vajnartech.moonstalker.rest.GetConstellationInfo;
import si.vajnartech.moonstalker.rest.GetSkyObjInfo;
import si.vajnartech.moonstalker.rest.GetStarInfo;

import static si.vajnartech.moonstalker.C.TAG;
import static si.vajnartech.moonstalker.C.calObj;
import static si.vajnartech.moonstalker.C.curObj;

public class MoveFragment extends MyFragment
{
  private  Spinner                  skyObjects;
  private  Spinner                  constellations;

  static private ArrayAdapter<CharSequence> skyObjAdapter;
  static private ArrayAdapter<CharSequence> constellationAdapter;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    View res = inflater.inflate(R.layout.frag_move, container, false);
    skyObjects = res.findViewById(R.id.spinner1);
    constellations = res.findViewById(R.id.spinner2);
    initAstroObjDropDown();
    setPositionString();
    return res;
  }

  private void scanAstroLine(int position, Spinner sp)
  {
    if (sp == null) return;
    String name = sp.getItemAtPosition(position).toString();
    new GetStarInfo(name, new GetSkyObjInfo.SkyInterface() {
      @Override
      public void updateConstellation()
      {
        getCFromStar();
      }
    });
  }

  private void initAstroObjDropDown()
  {
    skyObjects.setAdapter(skyObjAdapter);
    constellations.setAdapter(constellationAdapter);

    skyObjects.setSelection(skyObjAdapter.getPosition(calObj));
    constellations.setSelection(getCFromStar());

    skyObjects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
      {
        scanAstroLine(i, skyObjects);
      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView)
      {}
    });
    constellations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
      {}

      @Override
      public void onNothingSelected(AdapterView<?> adapterView)
      {}
    });

  }

  static int getCFromStar()
  {
    Log.i(TAG, "......." + C.curConstellation);
    for (int i=0; i<constellationAdapter.getCount(); i++)
    {
      String str = constellationAdapter.getItem(i).toString().toLowerCase();
      if (C.curConstellation.toLowerCase().contains(str))
        return i;
    }
    return 0;
  }

  static void initAstroObjDatabase(MainActivity ctx)
  {
    skyObjAdapter = ArrayAdapter.createFromResource(ctx, R.array.sky_objects, android.R.layout.simple_spinner_item);
    skyObjAdapter.sort(new Comparator<CharSequence>() {
      @Override public int compare(CharSequence charSequence, CharSequence t1)
      {
        return (charSequence.toString().charAt(0) - t1.toString().charAt(0));
      }
    });

    constellationAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_item);
    constellationAdapter.sort(new Comparator<CharSequence>() {
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

    return String.format("%s\n%s | %s", curObj.name, az, h);
  }

  private void setPositionString()
  {
    act.terminal.setText(formatPositionString(act.ctrl.az, act.ctrl.h));
  }
}
