package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

@SuppressLint("ViewConstructor") class CursorKeysBar
{
  static final int dx = 5;

  MainActivity act;
  Telescope t;

  CursorKeysBar(MainActivity act, View v)
  {
    this.act = act;
    t = act.getTelescope();

    v.findViewById(R.id.arrow_u).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view)
      {
        t.move(dx, 0);
      }
    });
    v.findViewById(R.id.arrow_l).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view)
      {
        t.move(0, -dx);
      }
    });
    v.findViewById(R.id.arrow_r).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view)
      {
        t.move(0, dx);
      }
    });
    v.findViewById(R.id.arrow_ok).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view)
      {
        t.calibrate();
      }
    });
    v.findViewById(R.id.arrow_d).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view)
      {
        t.move(-dx, 0);
      }
    });
  }
}
