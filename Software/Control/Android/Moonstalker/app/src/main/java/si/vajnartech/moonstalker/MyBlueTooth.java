package si.vajnartech.moonstalker;

import android.widget.Toast;


class MyBlueTooth extends BlueTooth
{
  MyBlueTooth(String url, MainActivity act)
  {
    super(url, act);
  }

  @Override
  public void exit(String msg)
  {
    act.get().myMessage(msg);
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    act.get().finish();
  }

  @Override
  public void progressOn()
  {
    act.get().progressIndicator.progressOn(ProgressIndicator.ProgressType.CONNECTING);
  }

  @Override
  public void progressOff()
  {
    act.get().progressIndicator.progressStop();
  }

  @Override
  public void onOk()
  {
    MainActivity activity = act.get();
    activity.runOnUiThread(() -> Toast.makeText(activity, activity.tx(R.string.connected), Toast.LENGTH_SHORT).show());
    TelescopeStatus.set(C.ST_CONNECTED);
  }

  @Override
  public void onError()
  {
    MainActivity activity = act.get();
    activity.myMessage(activity.tx(R.string.connection_failed));
    TelescopeStatus.set(C.ST_NOT_CONNECTED);
  }
}
