package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

@SuppressLint("ViewConstructor")
public class Buttons extends LinearLayout
{
  MainActivity act;
  @SuppressLint("InflateParams")
  public Buttons(final MainActivity act)
  {
    super(act);

    this.act = act;
    LayoutInflater inflater = (LayoutInflater) act.getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    if (inflater != null)
      addView(inflater.inflate(R.layout.status, null));
//
//    act.skyObjDropDown = (Spinner)findViewById(R.id.sp_sky_obj);
//
//    buttons();

  }

  private void buttons()
  {
//    ImageView move = (ImageView)findViewById(R.id.bt_move);
//    ImageView trace = (ImageView)findViewById(R.id.bt_trace);
//    ImageView err = (ImageView)findViewById(R.id.bt_exit);
//
//    move.setOnClickListener(new OnClickListener() {
//      @Override public void onClick(View view)
//      {
//        act.getTelescope().move(act.curObj.getRa(), act.curObj.getDec());
//      }
//    });
//
//    trace.setOnClickListener(new OnClickListener() {
//      @Override public void onClick(View view)
//      {
//        act.getTelescope().move(act.curObj.getRa(), act.curObj.getDec());
//        act.getTelescope().startTrace();
//      }
//    });
//
//    err.setOnClickListener(new OnClickListener() {
//      @Override public void onClick(View view)
//      {
//        act.errorExit();
//      }
//    });
  }
}
