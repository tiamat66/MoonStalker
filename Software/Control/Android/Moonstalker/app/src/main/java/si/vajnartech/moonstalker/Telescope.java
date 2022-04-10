package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.util.Log;

import static si.vajnartech.moonstalker.C.K;
import static si.vajnartech.moonstalker.C.ST_READY;
import static si.vajnartech.moonstalker.C.TAG;
import static si.vajnartech.moonstalker.C.calObj;
import static si.vajnartech.moonstalker.C.curObj;
import static si.vajnartech.moonstalker.OpCodes.*;

@SuppressWarnings({"SameParameterValue", "unused"})
public abstract class Telescope extends PositionCalculus
{
  private double hSteps = 0;
  private double vSteps = 0;

  private static final int PRECISION  = 1;

  Telescope(MainActivity act)
  {
    super(act);
  }

  void calibrate()
  {
    setPosition(curObj);
    raDec2AltAz();
    TelescopeStatus.set(ST_READY);
    curObj = new AstroObject(calObj, 0.0, 0.0, "", "");
    Log.i(TAG, "Calibration object is " + curObj);
  }

  private void move(int hSteps, int vSteps)
  {
    az += (vSteps * 360.0) / K;
    h += (hSteps * 360.0) / K;
    Log.i(TAG, "move");
    mv(hSteps, vSteps, 500); // TODO kako dolociti max speed
  }

  void move(AstroObject obj)
  {
    setPosition(obj);
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

    if (TelescopeStatus.locked()) {
      Log.i(TAG, "cannot execute move, telescope is locked");
      return;
    }

    azimuth_tmp = az;
    height_tmp = h;

    raDec2AltAz();
    dif_az = az - azimuth_tmp;
    dif_hi = h - height_tmp;

    hSteps += (dif_az * K) / 360.0;
    vSteps += (dif_hi * K) / 360.0;

    cur_h_steps = (int) hSteps;
    cur_v_steps = (int) vSteps;
    if (Math.abs(cur_h_steps) >= PRECISION || Math.abs(cur_v_steps) >= PRECISION) {
      hSteps -= cur_h_steps;
      vSteps -= cur_v_steps;
      mv(cur_h_steps, cur_v_steps, 500);
    }
  }

  void init()
  {
    inMsgProcess(INIT, new Bundle());
  }

  public abstract void inMsgProcess(String msg, Bundle bundle);

  abstract void mv(int hSteps, int vSteps, int speed);

  abstract void st();
}
