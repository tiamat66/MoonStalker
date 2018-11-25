package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.text.DecimalFormat;

import static com.robic.zoran.moonstalker.Control.MOVE;

class Telescope
{
  static final String TAG = "IZAA";

  // Telescope status values, TODO: imena statusov
  static final int ST_READY =    1;
  static final int ST_ERROR =    2;
  static final int ST_TRACING =  5;
  static final int ST_MOVING =   6;
  static final int ST_BTRY_LOW = 7;
  static final int ST_NOT_CAL =  9;
  static final int ST_NOT_CONNECTED = 10;
  static final int ST_CONNECTING    = 11;
  static final int ST_CONNECTED     = 12;

  // Mechanical characteristics
  private static final double MOTOR_STEPS_NUM      = 200.0;
  private static final double REDUCTOR_TRANSLATION = 30.0;
  private static final double BELT_TRANSLATION     = 48.0 / 14.0;
  private static final double K =
    MOTOR_STEPS_NUM * REDUCTOR_TRANSLATION * BELT_TRANSLATION;
  //================================================================================================

  Parameters p;
  private Position pos;
  private MainActivity act;
  private double hSteps = 0;
  private double vSteps = 0;

  private TraceHandler traceHandler;

  private static final int   H_NEGATIVE = 0;
  private static final float TRSHLD_BTRY = 11.0f;
  private static final int   PRECISION  = 1;

  Telescope(GPSService gps, MainActivity act)
  {
    p = new Parameters(null);
    pos = new Position(gps);
    this.act = act;
    traceHandler = new TraceHandler();
  }

  void calibrate()
  {
    // Calibration object is now the first item from sky objects list
    pos.set(act.curObj.ra, act.curObj.dec);
    pos.raDec2AltAz();
    setPosition();
    p.setStatus(ST_READY);
    act.curentFragment.sb.getConstellations().setEnabled(true);
    act.curentFragment.sb.getSkyObjects().setEnabled(true);
  }

  void move(int hSteps, int vSteps)
  {
    pos.az += (vSteps * 360.0) / K;
    pos.h  += (hSteps * 360.0) / K;
    Log.i(TAG, "move");
    act.getCtr().move(hSteps, vSteps);
  }

  void move(double ra, double dec)
  {
    pos.set(ra, dec);
    move();
  }

  private void move()
  {
    double dif_az;
    double dif_hi;
    double azimuth_tmp;
    double height_tmp;
    int cur_h_steps;
    int cur_v_steps;
    Bundle bundle = new Bundle();
    Control c = act.getCtr();

    azimuth_tmp = pos.az;
    height_tmp = pos.h;

    pos.raDec2AltAz();
    dif_az = pos.az - azimuth_tmp;
    dif_hi = pos.h - height_tmp;

    hSteps += (dif_az * K) / 360.0;
    vSteps += (dif_hi * K) / 360.0;

    cur_h_steps = (int) hSteps;
    cur_v_steps = (int) vSteps;
    if (Math.abs(cur_h_steps) >= PRECISION || Math.abs(cur_v_steps) >= PRECISION) {
      hSteps -= cur_h_steps;
      vSteps -= cur_v_steps;
      if (pos.h <= H_NEGATIVE) {
        bundle.putString("arg1",
          act.getResources().getString(R.string.e_alt_neg));
        act.getCtr().inMsgProcess(Control.ERROR, bundle);
      } else
        c.move(cur_h_steps, cur_v_steps);
    }
  }

  void batteryVoltage(float voltage)
  {
    p.btryVoltage = voltage;
    if (voltage < TRSHLD_BTRY)
      p.setStatus(ST_BTRY_LOW);
  }

  void startTrace()
  {
    p.setStatus(ST_TRACING);
    new TraceThread();
  }

  String formatLocationString()
  {
    DecimalFormat df = new DecimalFormat("###.##");
    String lon = "LO:" + df.format(act.getGps().getLongitude());
    String lat = "LA:" + df.format(act.getGps().getLatitude());

    return (lon + "|" + lat);
  }

  String formatPositionString()
  {
    DecimalFormat df = new DecimalFormat("###.##");
    String        az = "A:" + df.format(pos.az);
    String        h  = "H:" + df.format(pos.h);

    return (az + "|" + h);
  }

  void setPosition()
  {
    if (act.curentFragment.sb != null)
      act.curentFragment.sb.setMessage(formatPositionString());
  }

  @SuppressLint("HandlerLeak")
  class TraceHandler extends Handler
  {
    @Override
    public void handleMessage(Message message)
    {
      switch (message.what) {
        case MOVE:
          move();
          break;
      }
    }
  }

  //================================================================================================

  private class TraceThread extends Thread
  {
    TraceThread()
    {
      Log.i(TAG, "Start tracing.");
      this.start();
    }

    @Override
    public void run()
    {
      while (p.getStatus() == ST_TRACING) {
        if (act.getCtr().isLocked()) continue;
        Log.i("IZAA", "Mudlja=" + act.getCtr().isLocked());
        traceHandler.obtainMessage(MOVE).sendToTarget();
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  class Parameters
  {
    Bundle status;
    float  btryVoltage;

    Parameters(Bundle b)
    {
      status = new Bundle();
      status.putInt("st", ST_NOT_CONNECTED);
      btryVoltage = 0.0f;
    }

    void setStatus(int st)
    {
      status.putInt("st", st);
      if (act.curentFragment.sb != null)
        act.curentFragment.sb.setStatus(st);
    }

    void setStatus(Bundle b)
    {
      status = b;
    }

    int getStatus()
    {
      return status.getInt("st");
    }

    void setError(String e)
    {
      Bundle b = new Bundle();
      b.putInt("st", ST_ERROR);
      b.putString("arg1", e);
      act.curentFragment.sb.setError(b);
    }
  }
}

