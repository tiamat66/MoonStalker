package si.vajnartech.moonstalker.processor;

public class Ping extends Thread
{
    protected boolean running;
    protected QueueUI queue;

    public Ping(QueueUI queue)
    {
        this.queue = queue;
        running = true;
        start();
    }

    /** @noinspection BusyWait*/
    @Override
    public void run()
    {
        while(running) {
            try {
                sleep(7000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            new CmdPing(queue);
        }
    }
}
