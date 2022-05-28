package si.vajnartech.moonstalker;


import android.os.Bundle;


interface UpdateUI
{
  void update(String title);

  void update(Integer title);

  void update(Integer title, Bundle modes);
}
