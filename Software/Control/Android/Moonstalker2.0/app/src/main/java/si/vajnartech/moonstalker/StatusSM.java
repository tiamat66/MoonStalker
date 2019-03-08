package si.vajnartech.moonstalker;

import android.util.Log;

import static si.vajnartech.moonstalker.C.ST_CONNECTED;
import static si.vajnartech.moonstalker.C.ST_NOT_CAL;
import static si.vajnartech.moonstalker.C.ST_NOT_CONNECTED;

interface Nucleus
{
  void initTelescope();
  void calibrateTelescope();
}

public class StatusSM extends Thread
{
  private int     prevStatus;
  private boolean r;
  private Nucleus inf;

  StatusSM(Nucleus inf)
  {
    prevStatus = ST_NOT_CONNECTED;
    TelescopeStatus.set(ST_NOT_CONNECTED);
    this.inf = inf;
    r = true;
    start();
  }

  @Override
  public void run()
  {
    while (r) {
      try {
        sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      Log.i("STATUS", "[prev, current]=" + prevStatus + "," + TelescopeStatus.get());

      if (TelescopeStatus.get() == prevStatus)
        continue;
      if (prevStatus == ST_NOT_CONNECTED && TelescopeStatus.get() == ST_CONNECTED) {
        Log.i("STATUS", "init telescope");
        inf.initTelescope();
        prevStatus = ST_CONNECTED;
      }
      else if (prevStatus == ST_CONNECTED && TelescopeStatus.get() == ST_NOT_CAL) {
        Log.i("STATUS", "prompt to calibrate");
        inf.calibrateTelescope();
        prevStatus = ST_NOT_CAL;
      }
    }
  }
}