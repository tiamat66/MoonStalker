package si.vajnartech.moonstalker.processor;

public class Ping
{
    private static final int PING_INTERVAL = 7000;

    public Ping(Processor machine)
    {
        Runnable pingRunnable = new Runnable() {
            @Override
            public void run() {
                new CmdPing(machine);
                machine.getIoQueue().postDelayed(this, PING_INTERVAL);
            }
        };
        machine.getIoQueue().postDelayed(pingRunnable, PING_INTERVAL);
    }
}
