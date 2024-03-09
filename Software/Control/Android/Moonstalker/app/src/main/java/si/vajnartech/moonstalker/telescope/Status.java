package si.vajnartech.moonstalker.telescope;

import static si.vajnartech.moonstalker.C.ST_NOT_CONNECTED;

import java.util.concurrent.atomic.AtomicInteger;

public final class Status
{
    private final AtomicInteger value = new AtomicInteger(ST_NOT_CONNECTED);
    public volatile String message = "";
    public void set(int val)
    {
        value.set(val);
    }

    public int get()
    {
        return value.get();
    }
}
