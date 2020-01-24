package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static si.vajnartech.moonstalker.C.*;

public class ManualFragment extends MyFragment
{
  private Differential differential = new Differential();

  private AtomicBoolean fingerOnScreen = new AtomicBoolean(false);

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    View res = inflater.inflate(R.layout.frag_manual, container, false);
    View kp  = res.findViewById(R.id.key_pad);

    kp.setOnTouchListener(new View.OnTouchListener()
    {
      @Override public boolean onTouch(View view, MotionEvent event)
      {
        double rx, ry;
//        // deactivate touch-screen while moving
//        if (TelescopeStatus.get() == ST_MOVING ||
//            TelescopeStatus.get() == ST_MOVING_E)
//          return true;

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
          rx = event.getRawX();
          ry = event.getRawY();
          differential.up(new Differential(rx, ry));
          view.performClick();
          break;
        case MotionEvent.ACTION_MOVE:
          if (!fingerOnScreen.get()) {
            rx = event.getRawX();
            ry = event.getRawY();
            differential.up(new Differential(rx, ry));
            differential.is(differential.mul(new Differential(1.0, -1.0))); // negate y part of point
            String direction = differential.getDirection();
            if (!direction.equals(NONE)) {
              fingerOnScreen.set(true);
              act.ctrl.moveStart(direction);
            }
          }
          break;
        case MotionEvent.ACTION_UP:
          fingerOnScreen.set(false);
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
}

@SuppressWarnings("NullableProblems")
class Differential
{
  private static final double thrs = 3.0;

  private double q_x1, q_x2;
  private double x1, x2;

  Differential()
  {
    x1 = x2 = q_x1 = q_x2 = 0.0;
  }

  Differential(double x1, double x2)
  {
    this.x1 = x1;
    this.x2 = x2;
    q_x1 = q_x2 = 0.0;
  }

  void up(Differential v)
  {
    Differential res = new Differential(v.x1 - q_x1, v.x2 - q_x2);
    q_x1 = v.x1;
    q_x2 = v.x2;
    is(res);
  }

  void is(Differential val)
  {
    x1 = val.x1;
    x2 = val.x2;
  }

  Differential mul(Differential val)
  {
    return new Differential(x1 * val.x1, x2 * val.x2);
  }

  String getDirection()
  {
    if (x2 > thrs) return N;
    if (x2 < -thrs) return S;
    if (x1 < -thrs) return W;
    if (x1 > thrs) return E;
    return NONE;
  }

  @Override
  public String toString()
  {
    return String.format(Locale.GERMAN, "x1=%f, x2=%f", x1, x2);
  }
}


