package com.robic.zoran.moonstalker;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import static com.robic.zoran.moonstalker.Telescope.ST_BTRY_LOW;
import static com.robic.zoran.moonstalker.Telescope.ST_ERROR;
import static com.robic.zoran.moonstalker.Telescope.ST_MOVING;
import static com.robic.zoran.moonstalker.Telescope.ST_NOT_CAL;
import static com.robic.zoran.moonstalker.Telescope.ST_READY;
import static com.robic.zoran.moonstalker.Telescope.ST_TRACING;


public class StatusBar
{
  MainActivity act;

  private ImageView status;
  private ImageView gps;
  private ImageView btry;
  private Spinner   skyObjects;
  private TextView  msgBox;
  private TextView  msgBox2;
  private TextView  msgBox3;

  StatusBar(MainActivity act, View v)
  {
    this.act = act;

    skyObjects = (Spinner) v.findViewById(R.id.spinner1);
    status = (ImageView) v.findViewById(R.id.status);
    gps = (ImageView) v.findViewById(R.id.gps);
    btry = (ImageView) v.findViewById(R.id.btry);
    msgBox = (TextView) v.findViewById(R.id.msg_box);
    msgBox2 = (TextView) v.findViewById(R.id.tx_gps);
    msgBox3 = msgBox2;

    setStatus(act.getTelescope().p.getStatus());
    setGps();
  }

  Spinner getSkyObjects()
  {
    return skyObjects;
  }

  void setStatus(int s)
  {
    switch (s) {
    case ST_NOT_CAL:
      status.setImageResource(R.drawable.ic_cal_s);
      break;
    case ST_READY:
      status.setImageResource(R.drawable.ic_ok_s);
      setMessage(act.getTelescope().formatPositionString());
      msgBox.setTextColor(act.getResources().getColor(R.color.colorOk));
      break;
    case ST_TRACING:
      status.setImageResource(R.drawable.ic_tr_s);
      break;
    case ST_MOVING:
      status.setImageResource(R.drawable.ic_mv_s);
      break;
    case ST_ERROR:
    case ST_BTRY_LOW:
      status.setImageResource(R.drawable.ic_error_s);
    }
    if (s == ST_BTRY_LOW)
      btry.setImageResource(R.drawable.ic_error_s);
    else
      btry.setImageResource(R.drawable.ic_ok_s);
  }

  void setGps()
  {
    if (act.getGps().isGotLocation())
      gps.setImageResource(R.drawable.ic_ok_s);
    else
      gps.setImageResource(R.drawable.ic_error_s);

    msgBox2.setText(act.getTelescope().formatLocationString());
  }

  void setError(Bundle b)
  {
    if (b != null)
      msgBox3.setText(b.getString("arg1"));
  }

  void setMessage(String msg)
  {
    msgBox.setText(msg);
  }
}
