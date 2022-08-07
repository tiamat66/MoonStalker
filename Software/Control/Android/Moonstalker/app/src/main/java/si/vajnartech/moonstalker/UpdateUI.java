package si.vajnartech.moonstalker;


interface UpdateUI
{
  void updateMessage(String msg);

  void updateMessageColor(int color);

  void updateSideDrawer();

  void setPositionString(int color, SkyObject skyObject);

  void showFab(boolean show);
}
