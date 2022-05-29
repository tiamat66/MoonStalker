package si.vajnartech.moonstalker.statemachine;

import si.vajnartech.moonstalker.ProgressIndicator;

public interface StateMachineActions
{
  void initTelescope();

  void updateStatus();

  void startProgress(ProgressIndicator.ProgressType pt);

  void stopProgress();

  void move();

  void st();

  void dump(String str);

  void onNoAnswer();

  void disconnectBluetooth();

  void connect();

  void notification(String msg);
}
