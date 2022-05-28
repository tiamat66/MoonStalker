package si.vajnartech.moonstalker.statemachine;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Ball je objekt vezan na STANJE stroja katero se ob zadnji periodi NI spremenilo, torej se zatakne v nekem stanju
// ima se timeout ki pove po koliko periodah se stanje resetira

final class Ball
{
   private static final HashMap<Integer, Ball> balls = new HashMap<>();

   public static void addBall(Integer status, Ball ball)
   {
      balls.put(status, ball);
   }

   public static void executeBall(Integer status)
   {
      Ball b = balls.get(status);
      if (b != null)
         b.update();
   }

   private final Runnable postExecute;

   private final int executeTimeout;

   private final AtomicInteger stucked = new AtomicInteger(0);

   Ball(Runnable ex, int timeout)
   {
      postExecute = ex;
      executeTimeout = timeout;
   }

   void update()
   {
      if (stucked.incrementAndGet() > executeTimeout) {
         if (postExecute != null) postExecute.run();
         stucked.set(0);
      }
   }

   public static void reset()
   {
      for (Ball b: balls.values()) b.stucked.set(0);
   }
}
