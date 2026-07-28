package si.vajnartech.moonstalker.rest;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Params so parametri ki jih potrebujemo da vzpostavimo povezavo npr. token
public abstract class AsyncTaskExecutor<Params, Progress, Result>
{
    private static final ExecutorService SHARED_EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    private volatile Handler handler;

    private volatile Result result;

    protected AsyncTaskExecutor()
    {
    }

    public Handler getHandler()
    {
        Handler h = handler;
        if (h == null) {
            synchronized(AsyncTaskExecutor.class) {
                h = handler;
                if (h == null) {
                    h = handler = new Handler(Looper.getMainLooper());
                }
            }
        }
        return h;
    }

    public void execute()
    {
        execute(null);
    }

    public void execute(Params params)
    {
        onPreExecute();
        SHARED_EXECUTOR.execute(() -> {
            result = doInBackground(params);
            getHandler().post(() -> onPostExecute(result));
        });
    }

    public void cancel(@SuppressWarnings("unused") boolean mayInterruptIfRunning)
    {
        onCancelled();
    }

    public boolean isCancelled()
    {
        return false;
    }

    protected void onPreExecute()
    {
        // Override this method whereever you want to perform task before background execution get started
    }

    @SuppressWarnings("unused")
    public void publishProgress(Progress value)
    {
        getHandler().post(() -> onProgressUpdate(value));
    }

    protected void onProgressUpdate(@SuppressWarnings("unused") Progress value)
    {
        // Override this method whereever you want update a progress result
    }

    protected abstract Result doInBackground(Params params);

    protected abstract
    void onPostExecute(Result result);

    protected void onCancelled() {}
}
