package si.vajnartech.moonstalker;

import android.view.View;
import android.widget.TextView;

@SuppressWarnings("SameParameterValue")
final class TerminalWindow
{
  private final MainActivity act;

  TerminalWindow(MainActivity act)
  {
    this.act = act;
  }

  private TextView getTv()
  {
    return act.findViewById(R.id.msg_window);
  }

  @SuppressWarnings("unused")
  void setBackgroundColor(int color)
  {
    act.runOnUiThread(() -> {
      TextView tv = getTv();
      if (tv != null)
        tv.setBackgroundColor(color);
    });
  }

  void setText(String msg)
  {
    C.curMessage = msg;
    act.runOnUiThread(() -> {
      TextView tv = getTv();
      if (tv != null)
        tv.setText(msg);
    });
  }

  void writePosition(AstroObject obj)
  {
    setText(formatPositionString(obj, 0));
  }

  void show(boolean sh)
  {
    act.runOnUiThread(() -> {
      TextView tv = getTv();
      if (tv != null)
        tv.setVisibility(sh ? View.VISIBLE : View.GONE);
    });
  }

  private String formatPositionString(AstroObject obj, int mode)
  {
    switch (mode) {
      case 1:
        return String.format("%s | %s", obj.azimuth, obj.altitude);
      default:
        return String.format("%s (%s)\n%s | %s", obj.name, obj.constellation, obj.azimuth, obj.altitude);
    }
  }
}
