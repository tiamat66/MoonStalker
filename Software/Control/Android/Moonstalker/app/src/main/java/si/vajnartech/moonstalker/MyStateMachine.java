package si.vajnartech.moonstalker;

import android.os.Bundle;

import si.vajnartech.moonstalker.statemachine.StateMachine;

class MyStateMachine extends StateMachine
{

  MyStateMachine(MainActivity act)
  {
    super(act);
  }

  @Override
  public void initTelescope()
  {
    act.initControl();
  }

  @Override
  public void updateUI(int telescopeStatus, int telescopeMode)
  {
    act.userInterface.updateUI(act.fab, telescopeStatus, telescopeMode);
  }

  @Override
  public void move()
  {
    act.ctrl.move();
  }

  @Override
  public void st()
  {
    act.ctrl.st();
  }

  @Override
  public void dump(String str)
  {
    act.monitor.update(str);
  }

  @Override
  public void message(String msg)
  {
    act.myMessage(msg);
  }

  @Override
  public void disconnectBluetooth()
  {
    BlueTooth.disconnect();
  }

  @Override
  public void connect()
  {
    act.connect();
  }

  @Override
  public void setFragment(String tag, Class<? extends MyFragment> frag)
  {
    act.setFragment(tag, frag, new Bundle());
  }

  @Override
  public void calibrate()
  {
    act.ctrl.calibrate();
  }

  @Override
  protected void process()
  {
    if (state.statusChanged(C.ST_NOT_READY, C.ST_READY)) {
      TelescopeStatus.unlock();
      state.restore();
    } else if (state.statusChanged(C.ST_NOT_CONNECTED, C.ST_CONNECTING)) {
      connect();
    } else if (state.statusChanged(C.ST_CONNECTING, C.ST_CONNECTED)) {
      initTelescope();
    } else if (state.statusChanged(C.ST_CONNECTED, C.ST_INIT)) {
      st();
    } else if (state.statusChanged(C.ST_READY, C.ST_MOVING)) {
      SkyObject newObject = act.ctrl.getNewObject();
      if (newObject.get() != null)
        move();
    }
  }

  @Override
  protected void processMode()
  {
    if (state.modeChanged(C.ANY, C.ST_CALIBRATING)) {
      setFragment("manual", ManualFragment.class);
      message(act.tx(R.string.calibration_ntfy));
    } else if (state.modeChanged(C.ST_CALIBRATING, C.ST_CALIBRATED)) {
      calibrate();
      setFragment("main", MainFragment.class);
    } else if (state.modeChanged(C.ST_CALIBRATED, C.ST_MOVE_TO_OBJECT)) {
      setFragment("move", SelectFragment.class);
    }
  }
}
