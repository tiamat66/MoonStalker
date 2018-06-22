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
import static com.robic.zoran.moonstalker.Telescope.ST_TRACING;

public class AutoFragment extends VajnarFragment
{
  View res;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    res = inflater.inflate(R.layout.frag_auto, container, false);
    LinearLayout l1 = (LinearLayout) res.findViewById(R.id.layout1);
    sb = new StatusBar(act, res);

    l1.addView(act.view3D);
    initAstroObjDropDown();
    setStatus(act.getTelescope().p.getStatus());
    updateButtons();
    return res;
  }

  @Override
  public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
  {
    scanAstroLine(i, sb.getSkyObjects());
  }

  @Override
  public void onNothingSelected(AdapterView<?> adapterView)
  {}

  @Override
  protected void setStatus(int status)
  {
    ImageView st = (ImageView) res.findViewById(R.id.imageView7);
    if (act.getTelescope().p.getStatus() == ST_READY)
      st.setImageResource(R.drawable.ic_ok_s);
    else
      st.setImageResource(R.drawable.ic_error_s);
  }

  @Override protected void updateButtons()
  {
    Button b1 = (Button) res.findViewById(R.id.bt_trace);
    Button b2 = (Button) res.findViewById(R.id.bt_move);

    b1.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view)
      {
        switch (act.getTelescope().p.getStatus()) {
        case ST_TRACING:
          act.getTelescope().p.setStatus(ST_READY);
          updateButtons();
          break;
        default:
          act.getTelescope().startTrace();
          updateButtons();
        }
      }
    });
    b2.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view)
      {
        act.getTelescope().move(act.curObj.getRa(), act.curObj.getDec());
      }
    });

    switch (act.getTelescope().p.getStatus()) {
    case ST_TRACING:
      b2.setVisibility(View.GONE);
      b1.setText(R.string.stop);
      break;
    default:
      b2.setVisibility(View.VISIBLE);
      b1.setText(R.string.trace_button);
    }
  }
}
