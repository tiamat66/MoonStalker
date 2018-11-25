package com.robic.zoran.moonstalker;

import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import com.robic.zoran.moonstalker.rest.GetStarInfo;

public abstract class VajnarFragment extends DialogFragment
{
  MainActivity act;

  StatusBar sb = null;

  public static <T extends VajnarFragment> T instantiate(Class<T> cls, MainActivity act)
  {
    T res = null;
    try {
      res = cls.newInstance();
      res.act = act;
    } catch (java.lang.InstantiationException | IllegalAccessException e) {
      e.printStackTrace();
    }
    return res;
  }

  protected void scanAstroLine(int position, Spinner sp)
  {
    if (sp == null) return;
    String name = sp.getItemAtPosition(position).toString();
    new GetStarInfo(name, act);
  }

  protected void initAstroObjDropDown()
  {
    sb.getSkyObjects().setAdapter(act.skyObjAdapter);
    sb.getSkyObjects().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
      {
        scanAstroLine(i, sb.getSkyObjects());
      }

      @Override public void onNothingSelected(AdapterView<?> adapterView)
      {
        // TODO
      }
    });
    int i = act.skyObjAdapter.getPosition(act.getResources().getString(R.string.cal_obj));
    sb.getSkyObjects().setSelection(i);
    sb.getSkyObjects().setEnabled(false);

    sb.getConstellations().setAdapter(act.constellationAdapter);
    sb.getConstellations().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
      {
        // TODO
      }

      @Override public void onNothingSelected(AdapterView<?> adapterView)
      {
        // TODO
      }
    });
    i = act.constellationAdapter.getPosition(act.getResources().getString(R.string.cal_cnstl));
    sb.getConstellations().setSelection(i);
    sb.getConstellations().setEnabled(false);

  }

  protected abstract void setStatus(int status);

  protected abstract void updateButtons();
}
