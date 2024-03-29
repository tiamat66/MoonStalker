package si.vajnartech.moonstalker;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

final class Ball
{
  private final Runnable postExecute;

  private final int status;
  private final int threshold;

  AtomicInteger stucked = new AtomicInteger(0);

  Ball(Runnable ex, int st, int tr)
  {
    postExecute = ex;
    status = st;
    threshold = tr;
  }

  void update(int prevStatus)
  {
    if (prevStatus == status && stucked.incrementAndGet() > threshold) {
      if (postExecute != null) postExecute.run();
      stucked.set(0);
    }
  }
}

class Balls extends ArrayList<Ball>
{
  void go(int status)
  {
    for (Ball b: this) b.update(status);
  }

  void reset()
  {
    for (Ball b: this) b.stucked.set(0);
  }
}
