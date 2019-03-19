package si.vajnartech.moonstalker;

import android.widget.TextView;

final class TerminalWindow
{
  private TextView tv;

  TerminalWindow(MainActivity act)
  {
    tv = act.findViewById(R.id.msg_window);
  }

  void setBackgroundColor(int color)
  {
    tv.setBackgroundColor(color);
  }

  void setText(String msg)
  {
    tv.setText(msg);
  }
}
