package si.vajnartech.moonstalker.telescope;

import java.util.concurrent.atomic.AtomicInteger;

import si.vajnartech.moonstalker.OpCodes;
import si.vajnartech.moonstalker.rest.ObjController;
import si.vajnartech.moonstalker.rest.RObjAstroData;


public final class Status
{
    private final AtomicInteger value = new AtomicInteger(OpCodes.NOT_READY);
    public volatile ObjController message = null;
    public volatile RObjAstroData data = null;
    public volatile AtomicInteger mode  = new AtomicInteger(OpCodes.NOT_READY);
    public volatile String battery = "";
    public volatile boolean alarm = false;


    public void set(int val)
    {
        value.set(val);
    }

    public int get()
    {
        return value.get();
    }
}
