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
  public void updateStatus()
  {
    userInterface.updateStatus(act.fab);
  }

  @Override
  public void startProgress(ProgressIndicator.ProgressType type)
  {
    act.progressIndicator.progressOn(type);
  }

  @Override
  public void stopProgress()
  {
    act.progressIndicator.progressStop();
  }

  @Override
  public void move()
  {

  }

  @Override
  public void st()
  {
    act.ctrl.st();
  }

  @Override
  public void dump(String str)
  {
    act.runOnUiThread(() -> act.monitor.update(str));
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
  public void notification(String msg)
  {
    act.showNotification(msg);
  }

  @Override
  public void setFragment(String tag, Class<? extends MyFragment> frag)
  {
    act.setFragment(tag,  frag, new Bundle());
  }
}
