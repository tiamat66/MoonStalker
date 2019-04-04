package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import static si.vajnartech.moonstalker.C.ST_CALIBRATING;
import static si.vajnartech.moonstalker.C.ST_MOVING;
import static si.vajnartech.moonstalker.C.ST_MOVING_E;
import static si.vajnartech.moonstalker.C.ST_READY;
import static si.vajnartech.moonstalker.C.curObj;

public class ManualFragment extends MyFragment
{
  float dX, dY;
  D dK = new D();

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    View res = inflater.inflate(R.layout.frag_manual, container, false);
    res.findViewById(R.id.key_pad);
    act.findViewById(R.id.sky_object).setVisibility(View.GONE);
    act.findViewById(R.id.logo).setVisibility(View.GONE);
    if (TelescopeStatus.getMode() == ST_CALIBRATING) {
      act.terminal.setText(String.format(act.tx(R.string.to_calibrate), curObj.name));
      act.terminal.show();
    }
    else
      act.terminal.hide();

    res.setOnTouchListener(new View.OnTouchListener() {
      @Override public boolean onTouch(View view, MotionEvent event)
      {
        double rx, ry;
        // deactivate touch-screen while moving
        if(TelescopeStatus.get() == ST_MOVING ||
           TelescopeStatus.get() == ST_MOVING_E)
          return true;

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
          dX = view.getX() - event.getRawX();
          dY = view.getY() - event.getRawY();
          rx = event.getRawX();
          ry = event.getRawY();
          dK.up(new D(rx, ry));
          view.performClick();
          break;
        case MotionEvent.ACTION_MOVE:
          if (TelescopeStatus.get() == ST_READY) {
            rx = event.getRawX();
            ry = event.getRawY();
            dK.up(new D(rx, ry));
            dK.is(dK.mul(new D(1.0, -1.0))); // negate y part of point
            if (dK.getDirection() != C.Directions.NONE) {
              d = dK.getDirection();
              act.ctrl.moveStart(d);
            }
          }
          break;
        case MotionEvent.ACTION_UP:
          d = C.Directions.NONE;
          act.ctrl.moveStop();
          break;
        default:
          return false;
        }
        return true;
      }
    });
    return res;
  }

  C.Directions d = C.Directions.NONE;
}

class D
{
  private static final double thrs = 5;
  private double q_x1, q_x2;
  private double x1, x2;

  D()
  {
    x1 = x2 = q_x1 = q_x2 = 0.0;
  }

  D(double x1, double x2)
  {
    this.x1 = x1;
    this.x2 = x2;
    q_x1 = q_x2 = 0.0;
  }

  void up(D v)
  {
    D res = new D(v.x1 - q_x1, v.x2 - q_x2);
    q_x1 = v.x1;
    q_x2 = v.x2;
    is(res);
  }

  void is(D val)
  {
    x1 = val.x1;
    x2 = val.x2;
  }

  D mul(D val)
  {
    return new D(x1 * val.x1, x2 * val.x2);
  }

  C.Directions getDirection()
  {
    if (x2 > thrs) return C.Directions.UP;
    if (x2 < -thrs) return C.Directions.DOWN;
    if (x1 < -thrs) return C.Directions.LEFT;
    if (x1 > thrs) return C.Directions.RIGHT;
    return C.Directions.NONE;
  }
}


