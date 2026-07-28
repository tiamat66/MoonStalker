package si.vajnartech.moonstalker.telescope;

import static si.vajnartech.moonstalker.C.ST_NOT_READY;

import java.util.concurrent.atomic.AtomicInteger;

import si.vajnartech.moonstalker.rest.ObjController;
import si.vajnartech.moonstalker.rest.RObjAstroData;


public final class Status
{
    private final AtomicInteger value = new AtomicInteger(ST_NOT_READY);
    public volatile ObjController message = null;
    public volatile RObjAstroData data = null;

    public void set(int val)
    {
        value.set(val);
    }

    public int get()
    {
        return value.get();
    }
}
