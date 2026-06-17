package si.vajnartech.moonstalker.processor;

import static si.vajnartech.moonstalker.OpCodes.MSG_CONN_ERROR;
import static si.vajnartech.moonstalker.OpCodes.MSG_MV_ACK;
import static si.vajnartech.moonstalker.OpCodes.MSG_NOT_READY;

import com.google.gson.Gson;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.rest.ObjController;

public class CmdMove extends Controller<ObjController>
{
    protected ObjController object;
    public CmdMove(Processor machine, ObjController object)
    {
        super("move", machine);
        this.object = object;
    }

    @Override
    protected ObjController deserialize(BufferedReader br)
    {
        return gson.fromJson(br, ObjController.class);
    }

    @Override
    public ObjController backgroundFunc()
    {
        return callServer(object);
    }

    @Override
    protected void onPostExecute(ObjController objController) {

    }

//    @Override
//    protected void onPostExecute(String cmdResult)
//    {
//        if (cmdResult != null) {
//            if (cmdResult.equals("NOT_RDY")) {
//                machine.set(MSG_NOT_READY);
//            } else if (cmdResult.equals("TIMEOUT")) {
//                machine.set(MSG_CONN_ERROR);
//            } else if (cmdResult.startsWith("MV_ACK")) {
//                machine.set(MSG_MV_ACK);
//            }
//        }
//    }

}

