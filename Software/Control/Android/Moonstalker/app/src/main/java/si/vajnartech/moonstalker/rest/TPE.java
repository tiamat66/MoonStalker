package si.vajnartech.moonstalker.rest;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("NullableProblems")
class TPE
{
  public static class CallerDelayedPolicy implements RejectedExecutionHandler
  {

    public void rejectedExecution(Runnable r, ThreadPoolExecutor e)
    {
      if (!e.isShutdown()) {
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        new Thread(r).start();
      }
    }
  }

  private static final ThreadFactory sThreadFactory = new ThreadFactory()
  {
    private final AtomicInteger mCount = new AtomicInteger(1);

    public Thread newThread(Runnable r)
    {
      return new Thread(r, "HTTP Task #" + mCount.getAndIncrement());
    }
  };

  private static final BlockingQueue<Runnable> sPoolWorkQueue    = new LinkedBlockingQueue<>(128);
  private static final int                     CPU_COUNT         = Runtime.getRuntime().availableProcessors();
  private static final int                     CORE_POOL_SIZE    = CPU_COUNT + 1;
  private static final int                     MAXIMUM_POOL_SIZE = CPU_COUNT * 4 + 1;
  private static final int                     KEEP_ALIVE        = 1;

  static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
      CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
      sPoolWorkQueue, sThreadFactory, new CallerDelayedPolicy()
  );
}
