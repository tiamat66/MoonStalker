package com.robic.zoran.moonstalker;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import static com.robic.zoran.moonstalker.Telescope.ST_READY;

public class MainFragment extends VajnarFragment
{
  LinearLayout res;
  Button b1;
  Button b2;
  Button b3;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    res = (LinearLayout) inflater.inflate(R.layout.frag_main, container, false);

    b1 = (Button) res.findViewById(R.id.bt_auto);
    b1.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view)
      {
        act.setFragment("auto", AutoFragment.class, new Bundle());
      }
    });
    b2 = (Button) res.findViewById(R.id.bt_cal);
    b2.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view)
      {
        act.setFragment("calibration", ManualFragment.class, new Bundle());
      }
    });
    b3 = (Button) res.findViewById(R.id.bt_man);
    b3.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view)
      {
        act.setFragment("manual", ManualFragment.class, new Bundle());
      }
    });

    updateButtons();
    setStatus(act.getTelescope().p.getStatus());
    return res;
  }

  @Override
  protected void setStatus(int status)
  {
    ImageView st = (ImageView) res.findViewById(R.id.imageView1);
    if (act.getTelescope().p.getStatus() == ST_READY)
      st.setImageResource(R.drawable.ic_ok_s);
    else
      st.setImageResource(R.drawable.ic_error_s);
  }

  @Override
  protected void updateButtons()
  {
    if (act.getTelescope().p.getStatus() == ST_READY) {
      b1.setVisibility(View.VISIBLE);
      b3.setVisibility(View.VISIBLE);
    } else {
      b1.setVisibility(View.GONE);
      b3.setVisibility(View.GONE);
    }
  }

  @Override
  public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
  {
    scanAstroLine(i, sb.getSkyObjects());
  }

  @Override public void onNothingSelected(AdapterView<?> adapterView)
  {}
}
