package si.vajnartech.moonstalker;

import android.util.Log;

import static si.vajnartech.moonstalker.C.ST_CALIBRATING;
import static si.vajnartech.moonstalker.C.ST_CONNECTED;
import static si.vajnartech.moonstalker.C.ST_MANUAL;
import static si.vajnartech.moonstalker.C.ST_MOVE_TO_OBJECT;
import static si.vajnartech.moonstalker.C.ST_MOVING;
import static si.vajnartech.moonstalker.C.ST_MOVING_S;
import static si.vajnartech.moonstalker.C.ST_NOT_CAL;
import static si.vajnartech.moonstalker.C.ST_NOT_CONNECTED;
import static si.vajnartech.moonstalker.C.ST_READY;
import static si.vajnartech.moonstalker.C.ST_TRACING;

interface Nucleus
{
  void initTelescope();

  void updateStatus();

  void startProgress(MainActivity.ProgressType pt);

  void stopProgress();

  void move();

  void dump(String str);
}

public class StatusSM extends Thread
{
  private int     prevStatus;
  private int     prevMode;
  private boolean r;
  private Nucleus inf;

  StatusSM(Nucleus inf)
  {
    prevStatus = ST_NOT_CONNECTED;
    prevMode = ST_READY;
    TelescopeStatus.set(ST_NOT_CONNECTED);
    this.inf = inf;
    r = true;
    start();
    inf.updateStatus();
  }

  @Override
  public void run()
  {
    while (r) {
      try {
        sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      Log.i("STATUS", "[prev, current]=" + prevStatus + "," + TelescopeStatus.get());
      Log.i("STATUS", "[prev, current]=" + prevMode + "," + TelescopeStatus.getMode());
      Log.i("STATUS", "telescope lock =" + TelescopeStatus.locked());
      inf.dump("$ status=" + TelescopeStatus.get() + "\n");

      if (TelescopeStatus.get() == prevStatus &&
          TelescopeStatus.getMode() == prevMode &&
          TelescopeStatus.getMode() != ST_TRACING)
        continue;

      if (prevMode == ST_MOVE_TO_OBJECT && TelescopeStatus.getMode() == ST_MANUAL) {
        prevMode = TelescopeStatus.getMode();
        inf.updateStatus();
      }
      else if (prevMode == ST_TRACING && TelescopeStatus.getMode() == ST_MOVE_TO_OBJECT) {
        prevMode = TelescopeStatus.getMode();
        inf.updateStatus();
        continue;
      }
      else if ((prevMode == ST_CALIBRATING || prevMode == ST_MOVE_TO_OBJECT) && TelescopeStatus.getMode() == ST_TRACING) {
        prevMode = TelescopeStatus.getMode();
        inf.updateStatus();
        continue;
      }
      else if (prevMode == ST_MANUAL && TelescopeStatus.getMode() == ST_READY) {
        prevMode = TelescopeStatus.getMode();
        inf.updateStatus();
      }
      else if (prevMode == ST_CALIBRATING && TelescopeStatus.getMode() == ST_MOVE_TO_OBJECT) {
        prevMode = TelescopeStatus.getMode();
        inf.updateStatus();
      }
      else if (prevMode == ST_READY && TelescopeStatus.getMode() == ST_MANUAL) {
        prevMode = TelescopeStatus.getMode();
        inf.updateStatus();
      }
      else if (prevMode == ST_READY && TelescopeStatus.getMode() == ST_CALIBRATING) {
        prevMode = ST_CALIBRATING;
        inf.updateStatus();
      } else if (prevStatus == ST_NOT_CONNECTED && TelescopeStatus.get() == ST_CONNECTED) {
        inf.startProgress(MainActivity.ProgressType.INITIALIZING);
        inf.initTelescope();
        prevStatus = ST_CONNECTED;
        inf.updateStatus();
      } else if (prevStatus == ST_CONNECTED && TelescopeStatus.get() == ST_READY) {
        prevStatus = ST_NOT_CAL;
        inf.updateStatus();
        inf.stopProgress();
      } else if (prevStatus == ST_NOT_CAL && TelescopeStatus.get() == ST_READY) {
        prevStatus = ST_READY;
        inf.updateStatus();
      } else if (prevStatus == ST_READY && (TelescopeStatus.get() == ST_MOVING || TelescopeStatus.get() == ST_MOVING_S)) {
        prevStatus = TelescopeStatus.get();
        if (prevMode != ST_TRACING)
          inf.startProgress(MainActivity.ProgressType.MOVING);
      } else if ((prevStatus == ST_MOVING || prevStatus == ST_MOVING_S) && TelescopeStatus.get() == ST_READY) {
        prevStatus = TelescopeStatus.get();
        if (prevMode != ST_TRACING)
          inf.stopProgress();
        inf.updateStatus();
      } else if (prevStatus == ST_NOT_CAL && TelescopeStatus.get() == ST_CALIBRATING) {
        prevStatus = ST_CALIBRATING;
        inf.updateStatus();
      } else if (prevMode == ST_CALIBRATING && TelescopeStatus.get() == ST_READY) {
        prevStatus = TelescopeStatus.get();
        inf.updateStatus();
      }
      // ##
      if (prevMode == ST_TRACING)
        inf.move();
    }
  }
}