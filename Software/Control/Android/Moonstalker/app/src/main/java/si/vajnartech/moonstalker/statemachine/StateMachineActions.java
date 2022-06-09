package si.vajnartech.moonstalker.statemachine;

import si.vajnartech.moonstalker.MyFragment;
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

  void message(String msg);

  void disconnectBluetooth();

  void connect();

  void notification(String msg);

  void setFragment(String tag, Class<? extends MyFragment> frag);
}
