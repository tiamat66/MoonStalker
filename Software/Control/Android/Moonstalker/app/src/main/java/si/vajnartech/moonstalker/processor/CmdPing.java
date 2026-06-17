package si.vajnartech.moonstalker.processor;

import static si.vajnartech.moonstalker.C.ST_ERROR;
import static si.vajnartech.moonstalker.OpCodes.MSG_CONN_TIMEOUT;
import static si.vajnartech.moonstalker.OpCodes.MSG_INFO;
import static si.vajnartech.moonstalker.OpCodes.MSG_POSITION;
import static si.vajnartech.moonstalker.OpCodes.MSG_READY;
import static si.vajnartech.moonstalker.OpCodes.MSG_WARNING;

import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.util.Objects;

import si.vajnartech.moonstalker.rest.ObjController;

public class CmdPing extends Controller<ObjController>
{
    public CmdPing(Processor machine)
    {
        super("response", machine);
    }

    @Override
    protected ObjController deserialize(BufferedReader br)
    {
        return gson.fromJson(br, ObjController.class);
    }

    @Override
    public ObjController backgroundFunc()
    {
        return callServer(null);
    }

    @Override
    protected void onPostExecute(ObjController res)
    {
        Log.i("Tatatat", "Frtolin");
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

}
