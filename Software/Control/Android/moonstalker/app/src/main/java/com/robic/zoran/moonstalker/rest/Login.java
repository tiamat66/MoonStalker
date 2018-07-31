package com.robic.zoran.moonstalker.rest;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.robic.zoran.moonstalker.MainActivity;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Login extends AsyncTask<String, Void, Integer>
{
  String url;
  @SuppressLint("StaticFieldLeak")
  MainActivity act;
  String token;
  REST task;

  BluetoothSocket socket;

  static final int LOGIN_OK = 0;
  static final int LOGIN_ERR = -1;

  Login(String url, MainActivity act)
  {
    super();
    this.url = url;
    this.act = act;
  }

  void errorExit()
  {
    act.finish();
    System.exit(0);
  }

  @Override
  protected Integer doInBackground(String... params)
  {
    return connect();
  }

  public abstract Integer connect();

  // THREAD_POOL_EXECUTOR
  protected static class TPE
  {
    public static class CallerDelayedPolicy implements RejectedExecutionHandler
    {
      /**
       * Creates a {@code CallerRunsPolicy}.
       */
      public CallerDelayedPolicy() { }

      /**
       * Executes tásk r in the caller's thread, unless the executor
       * has been shut down, in which case the tásk is discarded.
       *
       * @param r the runnable task requested to be executed
       * @param e the executor attempting to execute this task
       */
      public void rejectedExecution(Runnable r, ThreadPoolExecutor e)
      {
        if (!e.isShutdown()) {
          try { Thread.sleep(500); } catch (InterruptedException ignored) {}
          new Thread(r).start();
        }
      }
    }
    private static final ThreadFactory           sThreadFactory    = new ThreadFactory()
    {
      private final AtomicInteger mCount = new AtomicInteger(1);

      public Thread newThread(Runnable r)
      {
        return new Thread(r, "HTTP Task #" + mCount.getAndIncrement());
      }
    };
    private static final   BlockingQueue<Runnable> sPoolWorkQueue    = new LinkedBlockingQueue<Runnable>(128);
    private static final   int                     CPU_COUNT         = Runtime.getRuntime().availableProcessors();
    private static final   int                     CORE_POOL_SIZE    = CPU_COUNT + 1;
    private static final   int                     MAXIMUM_POOL_SIZE = CPU_COUNT * 4 + 1;
    private static final int                     KEEP_ALIVE           = 1;
    public static final  Executor                THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
        CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS,
        sPoolWorkQueue, sThreadFactory, new CallerDelayedPolicy()
    );
  }
}
