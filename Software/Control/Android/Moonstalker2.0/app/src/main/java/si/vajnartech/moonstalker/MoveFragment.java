package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.support.annotation.NonNull;
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
import si.vajnartech.moonstalker.rest.GetStarInfo;

import static si.vajnartech.moonstalker.C.calConstellation;
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
    setPositionString((TextView) container.findViewById(R.id.position));
    return res;
  }

  private void scanAstroLine(int position, Spinner sp)
  {
    if (sp == null) return;
    String name = sp.getItemAtPosition(position).toString();
    new GetStarInfo(name);
  }

  private void initAstroObjDropDown()
  {
    skyObjects.setAdapter(skyObjAdapter);
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
    skyObjects.setSelection(skyObjAdapter.getPosition(calObj));

    constellations.setAdapter(constellationAdapter);
    constellations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
      {}

      @Override
      public void onNothingSelected(AdapterView<?> adapterView)
      {}
    });
    constellationAdapter.getPosition(calConstellation);
    constellations.setSelection(constellationAdapter.getPosition(calConstellation));
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

  private void setPositionString(TextView txt)
  {
    txt.setText(formatPositionString(act.ctrl.az, act.ctrl.h));
  }
}
