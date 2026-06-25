package si.vajnartech.moonstalker.processor;

import static si.vajnartech.moonstalker.OpCodes.MSG_CONN_TIMEOUT;
import static si.vajnartech.moonstalker.OpCodes.MSG_MVS_ACK;
import static si.vajnartech.moonstalker.OpCodes.MSG_NOT_READY;

import com.google.gson.Gson;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.rest.ObjController;
import si.vajnartech.moonstalker.rest.RObjController;

public class CmdMoveStart extends Controller<RObjController>
{
    protected ObjController direction;

    public CmdMoveStart(Processor machine, ObjController direction)
    {
        super("move", machine);
        this.direction = direction;
    }

    @Override
    protected RObjController deserialize(BufferedReader br)
    {
        return gson.fromJson(br, RObjController.class);
    }

    @Override
    public RObjController backgroundFunc()
    {
        return callServer(direction);
    }

    @Override
    protected void onPostExecute(RObjController rObjController)
    {

    }

//    @Override
//    protected void onPostExecute(String cmdResult)
//    {
//        if (cmdResult != null) {
//            if (cmdResult.equals("NOT_RDY")) {
//                machine.set(MSG_NOT_READY);
//            } else if (cmdResult.equals("TIMEOUT")) {
//                machine.set(MSG_CONN_TIMEOUT);
//            } else if (cmdResult.startsWith("MVS_ACK")) {
//                machine.set(MSG_MVS_ACK);
//            }
//        }
//    }


}
