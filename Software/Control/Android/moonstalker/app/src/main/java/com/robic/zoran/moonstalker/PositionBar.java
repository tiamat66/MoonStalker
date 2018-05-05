package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;

@SuppressLint("ViewConstructor")
public class PositionBar extends LinearLayout
{
  TextView azimuth;
  TextView height;

  @SuppressLint("InflateParams") public PositionBar(MainActivity act)
  {
    super(act);

    LayoutInflater inflater = (LayoutInflater) act.getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    if (inflater != null)
      addView(inflater.inflate(R.layout.ms_position, null));

    azimuth = (TextView) findViewById(R.id.tx_azimuth);
    height = (TextView) findViewById(R.id.tx_height);
  }

  public void setPosition(Position p)
  {
    DecimalFormat df  = new DecimalFormat("#.####");
    String        az  = "AZIMUTH=" + df.format(p.az);
    String        h   = "HEIGHT=" + df.format(p.h);
    azimuth.setText(az);
    height.setText(h);
  }
}
