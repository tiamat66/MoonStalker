package si.vajnartech.moonstalker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import si.vajnartech.moonstalker.androidsvg.SVGImageView;

import static si.vajnartech.moonstalker.C.E;
import static si.vajnartech.moonstalker.C.N;
import static si.vajnartech.moonstalker.C.NONE;
import static si.vajnartech.moonstalker.C.S;
import static si.vajnartech.moonstalker.C.W;

public class ManualFragment extends MyFragment implements View.OnTouchListener
{
  private View view;

  private final Differential d = new Differential();

  private final AtomicBoolean fingerOnScreen = new AtomicBoolean(false);

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    view = inflater.inflate(R.layout.keypad_arrows, container, false);
    updateArrows();

    return view;
  }

  void updateArrows()
  {
    if (view == null) return;
    SVGImageView iv = view.findViewById(R.id.keypad_arrows);
    iv.setImageDrawable(new SVGDrawable(getResources(), R.raw.keyboard_arrows, 800, 800));
    iv.setOnTouchListener(this);
  }

  @Override
  public boolean onTouch(View v, MotionEvent event)
  {
    double rx, ry;

    v.performClick();
    switch (event.getAction()) {
    case MotionEvent.ACTION_DOWN:
      rx = event.getRawX();
      ry = event.getRawY();
      d.up(new Differential(rx, ry));
      break;
    case MotionEvent.ACTION_MOVE:
      if (!fingerOnScreen.get()) {
        rx = event.getRawX();
        ry = event.getRawY();
        d.up(new Differential(rx, ry));
        d.is(d.mul(new Differential(1.0, -1.0))); // negate y part of point
        if (!d.getDirection().equals(NONE)) {
          String direction = d.getDirection();
          fingerOnScreen.set(true);
          TelescopeStatus.setMisc(direction);
          updateArrows();
          act.ctrl.moveStart(direction);
        }
      }
      break;
    case MotionEvent.ACTION_UP:
      fingerOnScreen.set(false);
      TelescopeStatus.setMisc(NONE);
      act.ctrl.moveStop();
      break;
    default:
      return false;
    }
    return true;
  }
}

@SuppressWarnings("NullableProblems")
class Differential
{
  private static final double thrs = 5.0;

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


