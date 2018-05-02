package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;

public class MsStatusBar extends LinearLayout
{
  MainActivity act;
  ImageView    status;
  TextView     stTitle;
  @SuppressLint("InflateParams")
  public MsStatusBar(final MainActivity act)
  {
    super(act);

    this.act = act;

    setOrientation(HORIZONTAL);
    LayoutInflater inflater = (LayoutInflater) act.getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    if (inflater != null)
      addView(inflater.inflate(R.layout.ms_status_bar, null));

    status = (ImageView) findViewById(R.id.sc_status);
    stTitle = (TextView) findViewById(R.id.tx_status);

    status.setOnClickListener(new OnClickListener()
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
    switch (s) {
    case Telescope.ST_READY:
      status.setImageResource(R.drawable.ic_ok);
      stTitle.setText(R.string.st_ready);
      break;
    case Telescope.ST_NOT_CAL:
      status.setImageResource(R.drawable.ic_not_cal);
      stTitle.setText(R.string.st_calibrate);
      break;
    case Telescope.ST_TRACING:
      status.setImageResource(R.drawable.ic_tracing);
      stTitle.setText(R.string.st_tracing);
      break;
    case Telescope.ST_MOVING:
      status.setImageResource(R.drawable.ic_moving);
      stTitle.setText(R.string.st_moving);
      break;
    default:
      status.setImageResource(R.drawable.ic_error);
      stTitle.setText(R.string.st_error);
      break;
    }
    status.invalidate();

    Log.i("IZAA", "Status SET: " + s);
  }

  public void setPosition(Position p)
  {
//    DecimalFormat df     = new DecimalFormat("#.####");
//    String        az     = "AZIMUTH=" + df.format(p.az);
//    String        h      = "HEIGH=" + df.format(p.h);
//    TextView      pos    = (TextView) findViewById(R.id.tx_location);
//    String        output = az + " " + h;
//    pos.setText(output);
  }
}
