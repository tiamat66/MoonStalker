package si.vajnartech.moonstalker.processor;

import android.os.Handler;

public class Ping
{
    private static final int PING_INTERVAL = 5000;
    private final Processor machine;
    private boolean running = false;

    private final Runnable pingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            new CmdPing(machine);
            Handler queue = machine.getIoQueue();
            if (queue != null) {
                queue.postDelayed(this, PING_INTERVAL);
            }
        }
    };

    public Ping(Processor machine)
    {
        this.machine = machine;
    }

    public synchronized void start()
    {
        if (running) return;
        running = true;
        Handler queue = machine.getIoQueue();
        if (queue != null) {
            queue.postDelayed(pingRunnable, PING_INTERVAL);
        }
    }

    public synchronized void stop()
    {
        running = false;
        Handler queue = machine.getIoQueue();
        if (queue != null) {
            queue.removeCallbacks(pingRunnable);
        }
    }
}
