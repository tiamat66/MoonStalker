package si.vajnartech.moonstalker;

import android.view.View;
import android.widget.TextView;

final class TerminalWindow
{
  private TextView tv;

  TerminalWindow(MainActivity act)
  {
    tv = act.findViewById(R.id.msg_window);
    hide();
    act.findViewById(R.id.sky_object).setVisibility(View.GONE);
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
