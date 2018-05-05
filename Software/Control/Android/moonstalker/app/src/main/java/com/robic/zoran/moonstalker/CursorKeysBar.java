package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;

@SuppressLint("ViewConstructor")
public class CursorKeysBar extends LinearLayout
{
  ImageView arrow;
  @SuppressLint("InflateParams") public CursorKeysBar(MainActivity act, int id)
  {
    super(act);

    LayoutInflater inflater = (LayoutInflater) act.getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    if (inflater != null)
      addView(inflater.inflate(R.layout.ms_cursor_keys, null));

    arrow = (ImageView) findViewById(R.id.sc_arrow);
    arrow.setImageResource(id);
  }
}
