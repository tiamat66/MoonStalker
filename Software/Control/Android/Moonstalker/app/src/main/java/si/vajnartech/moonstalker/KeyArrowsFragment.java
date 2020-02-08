package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.caverock.androidsvg.SVGImageView;

public class KeyArrowsFragment extends MyFragment
{
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    View res = inflater.inflate(R.layout.keypad_arrows, container, false);
    SVGImageView iv = res.findViewById(R.id.keypad_arrows);
    iv.setImageDrawable(new SVGDrawable(getResources(), R.raw.keyboard_arrows, 800, 800));
    return res;
  }
}
