package si.vajnartech.moonstalker;

import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.LinkedList;


class Monitor extends PopupWindow
{
  private TextView           tv;
  private LinkedList<String> content = new LinkedList<>();

  Monitor(View ctxView)
  {
    super(ctxView, 600, 300);
    tv = ctxView.findViewById(R.id.sys_monitor);
  }

  void update(String el)
  {
    content.add(el);
    int           size = content.size();
    StringBuilder p    = new StringBuilder();
    for (int i=7; i>0; i-- )
      p.append((size > i) ? content.get(size - (i + 1)) : "\n");
    p.append(content.isEmpty() ? "" : content.getLast());
    tv.setText(p.toString());
  }
}
