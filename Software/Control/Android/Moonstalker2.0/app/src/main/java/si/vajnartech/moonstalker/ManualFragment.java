package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class ManualFragment extends MyFragment implements View.OnTouchListener
{
  float dX, dY;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    View res = inflater.inflate(R.layout.frag_manual, container, false);
    res.findViewById(R.id.key_pad);

    return res;
  }

  @Override
  public boolean onTouch(View v, MotionEvent event)
  {
    double rx, ry;

    switch (event.getAction()) {
    case MotionEvent.ACTION_MOVE:
      rx = event.getRawX();
      ry = event.getRawY();
      break;
    case MotionEvent.ACTION_DOWN:
      dX = v.getX() - event.getRawX();
      dY = v.getY() - event.getRawY();
      rx = event.getRawX();
      ry = event.getRawY();
      //dK.up(new R2Double(rx, ry));
      v.performClick();
      break;
    case MotionEvent.ACTION_UP:
      rx = event.getRawX();
      ry = event.getRawY();
      // dK.up(new R2Double(rx, ry));
      // dK.is(dK.mul(new R2Double(1.0, -1.0))); // negate y part of point
    default:
      return false;
    }
    return true;
  }
}


