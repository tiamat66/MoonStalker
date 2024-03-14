package si.vajnartech.moonstalker;

import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;


class Monitor extends PopupWindow
{
  private final TextView tv;

  private final ArrayList<String> content = new ArrayList<>();

  Monitor(View ctxView)
  {
    super(ctxView, 800, 700);
    tv = ctxView.findViewById(R.id.sys_monitor);
  }

  void update(String el)
  {
    content.add(el + "\n");
    StringBuilder res = new StringBuilder();
    for (String str: content) {
      res.append(str);
    }
    tv.setText(res.toString());
  }
}
