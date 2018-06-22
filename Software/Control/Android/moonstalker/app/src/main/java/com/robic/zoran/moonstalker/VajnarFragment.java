package com.robic.zoran.moonstalker;

import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import java.util.Scanner;

import static com.robic.zoran.moonstalker.Telescope.ST_READY;

public abstract class VajnarFragment extends DialogFragment implements AdapterView.OnItemSelectedListener
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
    String buf = sp.getItemAtPosition(position).toString();

    Scanner sc   = new Scanner(buf);
    String  name = sc.next();
    //ra h min sec
    String ra = sc.next() + " ";
    ra += sc.next() + " ";
    ra += sc.next() + " ";
    //dec h min sec
    String dec = sc.next() + " ";
    dec += sc.next() + " ";
    dec += sc.next() + " ";
    act.curObj.set(name, ra, dec);
  }

  protected void initAstroObjDropDown()
  {
    ArrayAdapter<CharSequence> skyObjAdapter =
        ArrayAdapter.createFromResource(act, R.array.stars, android.R.layout.simple_spinner_item);
    sb.getSkyObjects().setAdapter(skyObjAdapter);
    sb.getSkyObjects().setOnItemSelectedListener(this);
    sb.getSkyObjects().setEnabled(true);
  }

  protected abstract void setStatus(int status);

  protected abstract void updateButtons();
}
