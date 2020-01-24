package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.util.Log;

import static si.vajnartech.moonstalker.C.*;

interface Nucleus
{
  void initTelescope();

  void updateStatus();

  void startProgress(MainActivity.ProgressType pt);

  void stopProgress();

  void move();

  void st();

  void dump(String str);

  void onNoAnswer();
}

@SuppressWarnings("FieldCanBeLocal")
public class StatusSM extends Thread
{
  private int timeout   = 2000;

  private int     prevStatus;
  private int     prevMode;
  private boolean r;
  private Nucleus inf;
  private Bundle  savedState = null;

  private Balls balls = new Balls();

  StatusSM(Nucleus inf)
  {
    reset();
    this.inf = inf;
    r = true;
    start();
    inf.updateStatus();
  }

  @Override
  public void run()
  {
    balls.add(new Ball(new Runnable() {
      @Override public void run()
      {
        inf.stopProgress();
        inf.onNoAnswer();
        BlueTooth.disconnect();
        reset();
      }
    }, ST_CONNECTED, 10));

    balls.add(new Ball(new Runnable() {
      @Override public void run()
      {
        Log.i("STATUS", "Pohendlaj stuck v ST_NOT_READY");
        TelescopeStatus.unlock();
        inf.st();
      }
    }, ST_NOT_READY, 5));

    while (r) {
      try {
        sleep(timeout);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      Log.i("STATUS", "[prev, current]=" + prevStatus + "," + TelescopeStatus.get());
      Log.i("STATUS", "[prev, current]=" + prevMode + "," + TelescopeStatus.getMode());
      Log.i("STATUS", "telescope lock =" + TelescopeStatus.locked());
      if (C.mStatus)
        inf.dump("$ status=" + TelescopeStatus.get() + "\n");
      if (TelescopeStatus.get() == prevStatus &&
          TelescopeStatus.get() != ST_READY &&
          TelescopeStatus.get() != ST_NOT_CONNECTED)
        balls.go(prevStatus);
//      if (TelescopeStatus.getMode() == prevMode &&
//          TelescopeStatus.getMode() != ST_TRACING)
//        continue;
      else {
        balls.reset();
        if (TelescopeStatus.get() != ST_NOT_READY) saveState();
        process();
      }
    }
  }

  private void reset()
  {
    prevStatus = ST_NOT_CONNECTED;
    prevMode = ST_READY;
    TelescopeStatus.set(ST_NOT_CONNECTED);
    TelescopeStatus.setMode(ST_READY);
  }

  private void saveState()
  {
    if (savedState == null) savedState = new Bundle();
    savedState.putInt("prev_status", prevStatus);
    savedState.putInt("prev_mode", prevMode);
    savedState.putInt("status", TelescopeStatus.get());
    savedState.putInt("mode", TelescopeStatus.getMode());
  }

  private void restoreState()
  {
    if (savedState == null) return;
    prevStatus = savedState.getInt("prev_status");
    prevMode = savedState.getInt("prev_mode");
    TelescopeStatus.set(savedState.getInt("status"));
    TelescopeStatus.setMode(savedState.getInt("mode"));
  }

  private void process()
  {
    if (prevStatus == ST_NOT_READY && TelescopeStatus.get() == ST_READY) {
      prevStatus = TelescopeStatus.get();
      TelescopeStatus.unlock();
      restoreState();
      inf.updateStatus();
      return;
    } else if (prevStatus != ST_NOT_READY && TelescopeStatus.get() == ST_NOT_READY) {
      prevStatus = TelescopeStatus.get();
      TelescopeStatus.lock();
      inf.updateStatus();
    } else if (prevMode == ST_MOVE_TO_OBJECT && TelescopeStatus.getMode() == ST_MANUAL) {
      prevMode = TelescopeStatus.getMode();
      inf.updateStatus();
    } else if (prevMode == ST_TRACING && TelescopeStatus.getMode() == ST_MOVE_TO_OBJECT) {
      prevMode = TelescopeStatus.getMode();
      inf.updateStatus();
      return;
    } else if ((prevMode == ST_CALIBRATING || prevMode == ST_MOVE_TO_OBJECT) && TelescopeStatus.getMode() == ST_TRACING) {
      prevMode = TelescopeStatus.getMode();
      inf.updateStatus();
      return;
    } else if (prevMode == ST_MANUAL && TelescopeStatus.getMode() == ST_READY) {
      prevMode = TelescopeStatus.getMode();
      inf.updateStatus();
    } else if (prevMode == ST_CALIBRATING && TelescopeStatus.getMode() == ST_MOVE_TO_OBJECT) {
      prevMode = TelescopeStatus.getMode();
      inf.updateStatus();
    } else if (prevMode == ST_READY && TelescopeStatus.getMode() == ST_MANUAL) {
      prevMode = TelescopeStatus.getMode();
      inf.updateStatus();
    } else if (prevMode == ST_READY && TelescopeStatus.getMode() == ST_CALIBRATING) {
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
      if (prevMode != ST_TRACING) {}
      //inf.startProgress(MainActivity.ProgressType.MOVING);
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