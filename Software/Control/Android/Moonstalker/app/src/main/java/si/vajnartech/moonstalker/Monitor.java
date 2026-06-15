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
    setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
    setOutsideTouchable(true);
    setFocusable(true);
    setElevation(10.0f);
  }

  void update(String el)
  {
    content.add(el + "\n");
    StringBuilder res = new StringBuilder();
    for (String str: content) {
      res.append(str);
    }
    tv.setText(res.toString());
    
    // Auto-scroll to bottom
    View parent = (View) tv.getParent();
    if (parent instanceof android.widget.ScrollView) {
        parent.post(() -> ((android.widget.ScrollView) parent).fullScroll(View.FOCUS_DOWN));
    }
  }
}
