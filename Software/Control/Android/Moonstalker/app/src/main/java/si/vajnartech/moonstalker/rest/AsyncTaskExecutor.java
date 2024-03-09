package si.vajnartech.moonstalker.rest;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AsyncTaskExecutor<Params, Progress, Result>
{
    private final ExecutorService executor;

    private Handler handler;

    private volatile Result result;

    protected AsyncTaskExecutor()
    {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

    public ExecutorService getExecutor()
    {
        return executor;
    }

    public Handler getHandler()
    {
        if (handler == null) {
            synchronized(AsyncTaskExecutor.class) {
                handler = new Handler(Looper.getMainLooper());
            }
        }
        return handler;
    }

    public void execute()
    {
        execute(null);
    }

    public void execute(Params params)
    {
        onPreExecute();
        executor.execute(() -> {
            result = doInBackground(params);
            getHandler().post(() -> onPostExecute(result));
        });
    }

    public void cancel(boolean mayInterruptIfRunning)
    {
        if (executor != null) {
            executor.shutdownNow();
            onCancelled();
        }
    }

    public boolean isCancelled()
    {
        return executor == null || executor.isTerminated() || executor.isShutdown();
    }

    protected void onPreExecute()
    {
        // Override this method whereever you want to perform task before background execution get started
    }

    // used for push progress resport to UI
    public void publishProgress(Progress value)
    {
        getHandler().post(() -> onProgressUpdate(value));
    }

    protected void onProgressUpdate(Progress value)
    {
        // Override this method whereever you want update a progress result
    }

    protected abstract Result doInBackground(Params params);

    protected abstract
    void onPostExecute(Result result);

    protected void onCancelled() {}
}



