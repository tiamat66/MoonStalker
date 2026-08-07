package si.vajnartech.moonstalker.processor;


import android.util.Log;

import java.io.BufferedReader;

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
           if (res == null) return;

           if (res.battery != null) machine.status.battery = res.battery;
           if (res.alarm != null) machine.status.alarm = res.alarm;

           machine.getUxQueue().post(machine.getAct()::updateIndicators);

           Log.i("CmdPing", res.state + " " + res.message + " " + res.error_data);
//           processUnsoliciedAction(res);

           switch(res.state) {
               case "ready":
                   if (machine.status.get() == OpCodes.READY) return;
                   machine.set(OpCodes.READY, new ObjController(res.message, res.error_data, ""), OpCodes.NOT_READY, null);
                   break;
               case "connected":
                   if (machine.status.get() == OpCodes.CONNECTED) return;
                   if (machine.status.get() == OpCodes.TRACK)  {
                       machine.set(OpCodes.POSITION);
                       return;
                   }
                   machine.set(OpCodes.CONNECTED, new ObjController(res.message, res.error_data, ""));
                   break;
               case "error":
                   if (machine.status.get() == OpCodes.ERROR) return;
                   machine.set(OpCodes.ERROR, new ObjController(res.message, "", ""));
                   break;
               case "moving":
                   if (machine.status.get() == OpCodes.MOVING ||
                           machine.status.get() == OpCodes.TRACK) return;

                   machine.set(OpCodes.MOVING, new ObjController(res.message, res.error_data, ""));
           }
        }

        // BATTERY, INFO, WARNING, ERROR from Telescope
//        private void processUnsoliciedAction(RObjController res)
//        {
//            if (res.battery != null && res,)
//            machine.set(OpCodes.GOT_BATTERY, new ObjController(res.battery, "", ""));
//        }
    }



