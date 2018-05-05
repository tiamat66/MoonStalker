package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;

@SuppressLint("ViewConstructor")
public class MsStatusBar extends LinearLayout
{
  MainActivity act;
  TextView     stTitle;
  @SuppressLint("InflateParams")
  public MsStatusBar(final MainActivity act)
  {
    super(act);

    this.act = act;

    LayoutInflater inflater = (LayoutInflater) act.getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    if (inflater != null)
      addView(inflater.inflate(R.layout.ms_status_bar, null));

    stTitle = (TextView) findViewById(R.id.tx_status);

    stTitle.setOnClickListener(new OnClickListener()
    {
      @Override public void onClick(View view)
      {
        switch (act.getTelescope().p.getStatus()) {
        case Telescope.ST_NOT_CAL:
          act.getTelescope().calibrate();
          act.getTelescope().p.setStatus(Telescope.ST_READY);
          break;
        }
      }
    });
  }

  public void setStatus(int s)
  {
    Drawable ok = getResources().getDrawable(R.drawable.ic_ok);
    Drawable notCal = getResources().getDrawable(R.drawable.ic_not_cal);
    Drawable trac = getResources().getDrawable(R.drawable.ic_tracing);
    Drawable mov = getResources().getDrawable(R.drawable.ic_moving);
    Drawable error = getResources().getDrawable(R.drawable.ic_error);
    switch (s) {
    case Telescope.ST_READY:
      stTitle.setCompoundDrawablesWithIntrinsicBounds(ok, null, null, null);
      stTitle.setText(R.string.st_ready);
      break;
    case Telescope.ST_NOT_CAL:
      stTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(notCal, null, null, null);
      stTitle.setText(R.string.st_calibrate);
      break;
    case Telescope.ST_TRACING:
      stTitle.setCompoundDrawablesWithIntrinsicBounds(trac, null, null, null);
      stTitle.setText(R.string.st_tracing);
      break;
    case Telescope.ST_MOVING:
      stTitle.setCompoundDrawablesWithIntrinsicBounds(mov, null, null, null);
      stTitle.setText(R.string.st_moving);
      break;
    default:
      stTitle.setCompoundDrawables(error, null, null, null);
      stTitle.setText(R.string.st_error);
      break;
    }

    Log.i("IZAA", "Status SET: " + s);
  }
}
