package si.vajnartech.moonstalker.processor;


import android.util.Log;

import java.io.BufferedReader;
import java.util.concurrent.atomic.AtomicInteger;

import si.vajnartech.moonstalker.OpCodes;
import si.vajnartech.moonstalker.rest.ObjController;
import si.vajnartech.moonstalker.rest.RObjController;

public class CmdPing extends Controller<RObjController>
{
    public CmdPing(Processor machine)
    {
        super("ping", machine);
    }

    @Override
    protected RObjController deserialize(BufferedReader br)
    {
        return gson.fromJson(br, RObjController.class);
    }

    @Override
    public RObjController backgroundFunc()
    {
        return callServer(null);
    }

    @Override
    protected void onPostExecute(RObjController res)
    {
           Log.i("CmdPing", res.state + " " + res.message + " " + res.error_data);
           switch(res.state) {
               case "ready":
                   if (machine.status.get() == OpCodes.READY) return;
                   machine.set(OpCodes.READY, new ObjController(res.message, res.error_data, ""), OpCodes.READY, null, null);
                   break;
               case "connected":
                   if (machine.status.get() == OpCodes.CONNECTED) return;
                   machine.set(OpCodes.CONNECTED, new ObjController(res.message, res.error_data, ""), OpCodes.CONNECTED, null, null);
                   break;
               case "error":
                   if (machine.status.get() == OpCodes.ERROR) return;
                   machine.set(OpCodes.ERROR, new ObjController(res.message, "", ""), OpCodes.ERROR, null, null);
                   break;
               case "moving":
                   if (machine.status.get() == OpCodes.MOVING) return;
                   machine.set(OpCodes.MOVING, new ObjController(res.message, res.error_data, ""), OpCodes.MOVING, null, null);
           }
        }
    }



