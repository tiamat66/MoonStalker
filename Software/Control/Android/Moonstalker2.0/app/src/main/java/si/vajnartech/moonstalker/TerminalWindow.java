package si.vajnartech.moonstalker;

import android.view.View;
import android.widget.TextView;

@SuppressWarnings("SameParameterValue")
final class TerminalWindow
{
  private TextView tv;
  private MainActivity act;

  TerminalWindow(MainActivity act)
  {
    this.act = act;
    tv = act.findViewById(R.id.msg_window);
  }

  void setBackgroundColor(int color)
  {
    tv.setBackgroundColor(color);
  }

  void setText(String msg)
  {
    C.curMessage = msg;
    act.refreshCurrentFragment();
    tv.setText(msg);
  }
}
