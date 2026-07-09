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
           Log.i("CmdPing", res.state + " " + res.message + " " + res.error_data);
           switch(res.state) {
               case "ready":
                   machine.set(OpCodes.READY, new ObjController(res.message, res.error_data, ""));
                   break;
               case "connected":
                   machine.set(OpCodes.CONNECTED, new ObjController(res.message, res.error_data, ""));
                   break;
               case "error":
                   machine.set(OpCodes.ERROR, new ObjController(res.message, "", ""));
                   break;
               case "moving":
                   machine.set(OpCodes.MOVING, new ObjController(res.message, res.error_data, ""));
           }
        }
    }

//    @Override
//    protected void onPostExecute(String cmdResult)
//    {
//        if (cmdResult == null) return;
//
//        String msg = getParams(cmdResult);
//
//        if (Objects.equals(cmdResult, "RDY")) {
//            machine.set(MSG_READY);
//        } else if (Objects.equals(cmdResult, "TIMEOUT")) {
//            machine.set(MSG_CONN_TIMEOUT);
//        } else if (cmdResult.startsWith("ERROR")) {
//            machine.set(ST_ERROR, msg);
//        } else if (cmdResult.startsWith("WARNING")) {
//            machine.set(MSG_WARNING);
//        } else if (cmdResult.startsWith("INFO")) {
//            machine.set(MSG_INFO, msg);
//        } else if (cmdResult.startsWith("POS")) {
//            machine.set(MSG_POSITION, msg);
//        }
//    }


