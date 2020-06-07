package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.util.Log;

import static si.vajnartech.moonstalker.C.*;
import static si.vajnartech.moonstalker.OpCodes.MVE_ACK;
import static si.vajnartech.moonstalker.OpCodes.MVS_ACK;

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
  private int timeout   = 1000;

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

  private void addBalls()
  {
    balls.add(new Ball(new Runnable() {
      @Override public void run()
      {
        inf.stopProgress();
        inf.onNoAnswer();
        BlueTooth.disconnect();
        reset();
      }
    }, ST_CONNECTED, 7));

    balls.add(new Ball(new Runnable() {
      @Override public void run()
      {
        TelescopeStatus.unlock();
        inf.st();
      }
    }, ST_NOT_READY, 5));

    balls.add(new Ball(new Runnable() {
      @Override public void run()
      {
        if (TelescopeStatus.getAck().isEmpty()) return;
        if (TelescopeStatus.getAck().equals(MVS_ACK) ||
            TelescopeStatus.getAck().equals(MVE_ACK)) {
          if (MVE_ACK.equals(TelescopeStatus.getAck())) {
            TelescopeStatus.set(ST_READY);
            TelescopeStatus.setMisc(NONE);
          }
          else
            TelescopeStatus.set(ST_MOVING);
          TelescopeStatus.setAck(CLEAR);
        }
        else {
          inf.onNoAnswer();
          TelescopeStatus.setAck(CLEAR);
          restoreState();
        }

      }
    }, ST_WAITING_ACK, 3));
  }

  @Override
  public void run()
  {
    addBalls();

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
      else {
        balls.reset();
        if (TelescopeStatus.get() != ST_NOT_READY &&
            TelescopeStatus.get() != ST_MOVING &&
            TelescopeStatus.get() != ST_WAITING_ACK)
          saveState();
        process();
        if (TelescopeStatus.get() != prevStatus || TelescopeStatus.getMode() != prevMode)
          inf.updateStatus();
        prevStatus = TelescopeStatus.get();
        prevMode = TelescopeStatus.getMode();
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
      TelescopeStatus.unlock();
      restoreState();
    } else if (prevStatus != ST_NOT_READY && TelescopeStatus.get() == ST_NOT_READY) {
      TelescopeStatus.lock();
    } else if (prevStatus == ST_NOT_CONNECTED && TelescopeStatus.get() == ST_CONNECTED) {
      inf.startProgress(MainActivity.ProgressType.INITIALIZING);
      inf.initTelescope();
    } else if (prevStatus == ST_CONNECTED && TelescopeStatus.get() == ST_READY)
      inf.stopProgress();
  }
}