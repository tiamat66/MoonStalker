package si.vajnartech.moonstalker;

class MyBlueTooth extends BlueTooth
{
  MyBlueTooth(String url, MainActivity act)
  {
    super(url, act);
  }

  @Override
  public void onOk()
  {
    TelescopeStatus.set(C.ST_CONNECTED);
  }

  @Override
  public void onError(String msg)
  {
    act.get().myMessage(msg);
    TelescopeStatus.set(C.ST_NOT_CONNECTED);
  }
}
