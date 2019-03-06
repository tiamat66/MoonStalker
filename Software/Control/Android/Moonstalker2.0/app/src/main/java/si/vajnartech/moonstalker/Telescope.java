package si.vajnartech.moonstalker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.text.DecimalFormat;

import static si.vajnartech.moonstalker.C.K;
import static si.vajnartech.moonstalker.C.ST_BTRY_LOW;
import static si.vajnartech.moonstalker.C.ST_ERROR;
import static si.vajnartech.moonstalker.C.ST_NOT_CONNECTED;
import static si.vajnartech.moonstalker.C.ST_READY;
import static si.vajnartech.moonstalker.C.ST_TRACING;
import static si.vajnartech.moonstalker.C.TAG;
import static si.vajnartech.moonstalker.C.curObj;
import static si.vajnartech.moonstalker.Control.MOVE;

public abstract class Telescope extends PositionCalculus
{
  private double hSteps = 0;
  private double vSteps = 0;

  private TraceHandler traceHandler;

  private static final int   H_NEGATIVE  = 0;
  private static final float TRSHLD_BTRY = 11.0f;
  private static final int   PRECISION   = 1;

  float btryVoltage;
  Bundle status;

  Telescope(MainActivity act)
  {
    super(act);

    status = new Bundle();
    setStatus(ST_NOT_CONNECTED);
    traceHandler = new TraceHandler();
  }

  void calibrate()
  {
    // Calibration object is now the first item from sky objects list
    set(curObj);
    // setPosition();
    setStatus(ST_READY);
    // act.curentFragment.sb.getConstellations().setEnabled(true);
    // act.curentFragment.sb.getSkyObjects().setEnabled(true);
  }

  private void move(int hSteps, int vSteps)
  {
    az += (vSteps * 360.0) / K;
    h += (hSteps * 360.0) / K;
    Log.i(TAG, "move");
    mv(hSteps, vSteps);
  }

  private void move(double ra, double dec)
  {
    set(ra, dec);
    move();
  }

  private void move()
  {
    double dif_az;
    double dif_hi;
    double azimuth_tmp;
    double height_tmp;
    int    cur_h_steps;
    int    cur_v_steps;
    Bundle bundle = new Bundle();

    azimuth_tmp = az;
    height_tmp = h;


    dif_az = az - azimuth_tmp;
    dif_hi = h - height_tmp;

    hSteps += (dif_az * K) / 360.0;
    vSteps += (dif_hi * K) / 360.0;

    cur_h_steps = (int) hSteps;
    cur_v_steps = (int) vSteps;
    if (Math.abs(cur_h_steps) >= PRECISION || Math.abs(cur_v_steps) >= PRECISION) {
      hSteps -= cur_h_steps;
      vSteps -= cur_v_steps;
      if (h <= H_NEGATIVE) {
        bundle.putString("arg1", "Negative altitude");
        inMsgProcess(Control.ERROR, bundle);
      } else
        mv(cur_h_steps, cur_v_steps);
    }
  }

  void setBatteryVoltage(float voltage)
  {
    btryVoltage = voltage;
    if (voltage < TRSHLD_BTRY)
      setStatus(ST_BTRY_LOW);
  }

  void startTrace()
  {
    setStatus(ST_TRACING);
    new TraceThread();
  }

  String formatLocationString()
  {
    DecimalFormat df  = new DecimalFormat("###.##");
    String        lon = "LO:" + df.format(curLocation.getLongitude());
    String        lat = "LA:" + df.format(curLocation.getLatitude());

    return (lon + "|" + lat);
  }

  String formatPositionString()
  {
    DecimalFormat df = new DecimalFormat("###.##");
    String        az = "A:" + df.format(this.az);
    String        h  = "H:" + df.format(this.h);

    return (az + "|" + h);
  }

  public void init()
  {
    inMsgProcess(Control.INIT, null);
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
      while (getStatus() == ST_TRACING) {
        if (isLocked()) continue;
        traceHandler.obtainMessage(MOVE).sendToTarget();
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  void setStatus(int st)
  {
    status.putInt("st", st);
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
  }

  abstract boolean isLocked();

  public abstract void inMsgProcess(int msg, Bundle bundle);

  abstract void mv(int hSteps, int vSteps);
}
