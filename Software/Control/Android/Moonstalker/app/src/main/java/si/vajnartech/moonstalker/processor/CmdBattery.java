package si.vajnartech.moonstalker.processor;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.rest.RObjController;

public class CmdBattery extends Controller<RObjController>
{
    public CmdBattery(Processor queue) {
        super("battery", queue);
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
    protected void onPostExecute(RObjController rObjController)
    {
        // TODO
    }

//    @Override
//    protected void onPostExecute(String cmdResult)
//    {
//        String msg = getParams(cmdResult);
//
//        if (cmdResult.startsWith("BTRY")) {
//            machine.set(MSG_BATTERY_RES, msg);
//        }  else if (cmdResult.equals("TIMEOUT")) {
//            machine.set(MSG_CONN_ERROR);
//        }
//    }


}
