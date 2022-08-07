package si.vajnartech.moonstalker.statemachine;

import si.vajnartech.moonstalker.MyFragment;

public interface StateMachineActions
{
  void initTelescope();

  void updateUI(int status, int mode);

  void move();

  void st();

  void dump(String str);

  void message(String msg);

  void disconnectBluetooth();

  void connect();

  void setFragment(String tag, Class<? extends MyFragment> frag);

  void calibrate();
}
