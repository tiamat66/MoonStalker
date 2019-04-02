package si.vajnartech.moonstalker;

import android.util.Log;

import static si.vajnartech.moonstalker.C.ST_CALIBRATING;
import static si.vajnartech.moonstalker.C.ST_CONNECTED;
import static si.vajnartech.moonstalker.C.ST_MOVING;
import static si.vajnartech.moonstalker.C.ST_MOVING_S;
import static si.vajnartech.moonstalker.C.ST_NOT_CAL;
import static si.vajnartech.moonstalker.C.ST_NOT_CONNECTED;
import static si.vajnartech.moonstalker.C.ST_READY;

interface Nucleus
{
  void initTelescope();

  void updateStatus();

  void startProgress(MainActivity.ProgressType pt);

  void stopProgress();
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

      if (TelescopeStatus.get() == prevStatus && TelescopeStatus.getMode() == prevMode)
        continue;

      if (prevMode == ST_READY && TelescopeStatus.getMode() == ST_CALIBRATING) {
        prevMode = ST_CALIBRATING;
        inf.updateStatus();
      } else if (prevStatus == ST_NOT_CONNECTED && TelescopeStatus.get() == ST_CONNECTED) {
        Log.i("STATUS", "init telescope");
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
        inf.startProgress(MainActivity.ProgressType.MOVING);
      } else if ((prevStatus == ST_MOVING || prevStatus == ST_MOVING_S) && TelescopeStatus.get() == ST_READY) {
        prevStatus = TelescopeStatus.get();
        inf.stopProgress();
        inf.updateStatus();
      } else if (prevStatus == ST_NOT_CAL && TelescopeStatus.get() == ST_CALIBRATING) {
        prevStatus = ST_CALIBRATING;
        inf.updateStatus();
      } else if (prevStatus == ST_CALIBRATING && TelescopeStatus.get() == ST_READY) {
        prevStatus = ST_READY;
        inf.updateStatus();
      }
    }
  }
}