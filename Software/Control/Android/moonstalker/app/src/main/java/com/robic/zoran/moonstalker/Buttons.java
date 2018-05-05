package com.robic.zoran.moonstalker;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

public class Buttons extends LinearLayout
{
  public Buttons(MainActivity act)
  {
    super(act);

    LayoutInflater inflater = (LayoutInflater) act.getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    if (inflater != null)
      addView(inflater.inflate(R.layout.ms_buttons, null));
  }
}
