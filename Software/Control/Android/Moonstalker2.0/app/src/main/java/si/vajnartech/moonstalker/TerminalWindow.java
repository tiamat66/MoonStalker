package si.vajnartech.moonstalker;

import android.view.View;
import android.widget.TextView;

@SuppressWarnings("SameParameterValue")
final class TerminalWindow
{
  private TextView tv;

  TerminalWindow(MainActivity act)
  {
    tv = act.findViewById(R.id.msg_window);
    hide();
  }

  void setBackgroundColor(int color)
  {
    tv.setBackgroundColor(color);
  }

  void setText(String msg)
  {
    tv.setText(msg);
  }

  void show()
  {
    tv.setVisibility(View.VISIBLE);
  }

  void hide()
  {
    tv.setVisibility(View.GONE);
  }
}
