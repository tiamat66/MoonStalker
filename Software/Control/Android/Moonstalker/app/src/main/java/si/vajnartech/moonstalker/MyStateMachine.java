package si.vajnartech.moonstalker;

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
  public void onNoAnswer()
  {
    act.myMessage(act.tx(R.string.msg_no_answer));
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
}
