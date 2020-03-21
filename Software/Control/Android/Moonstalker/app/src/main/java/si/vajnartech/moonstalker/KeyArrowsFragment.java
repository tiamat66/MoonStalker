package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import si.vajnartech.moonstalker.androidsvg.SVGImageView;


public class KeyArrowsFragment extends MyFragment implements View.OnClickListener
{
  static String upAColor = "#ff0000";
  private View view;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    view = inflater.inflate(R.layout.keypad_arrows, container, false);
    _updateArrows();
    return view;
  }

  private void _updateArrows()
  {
    if (view == null) return;

    SVGImageView iv  = view.findViewById(R.id.keypad_arrows);
    iv.setImageDrawable(new SVGDrawable(getResources(), R.raw.keyboard_arrows, 800, 800));
    iv.setOnClickListener(this);
  }

  @Override
  public void onClick(View v)
  {
    upAColor = "#00ff00";
    _updateArrows();
  }
}
