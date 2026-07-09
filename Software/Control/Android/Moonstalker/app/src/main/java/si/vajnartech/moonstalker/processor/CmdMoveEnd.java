package si.vajnartech.moonstalker.processor;

import static si.vajnartech.moonstalker.OpCodes.MSG_CONN_ERROR;
import static si.vajnartech.moonstalker.OpCodes.MSG_MVE_ACK;

import com.google.gson.Gson;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.rest.RObjController;

public class CmdMoveEnd extends Controller<RObjController>
{
    public CmdMoveEnd(Processor queue)
    {
        super("moveend", queue);
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

    }

//    @Override
//    protected void onPostExecute(String cmdResult)
//    {
//        if (cmdResult != null) {
//           if (cmdResult.equals("TIMEOUT")) {
//               machine.set(MSG_CONN_ERROR) ;
//            } else if (cmdResult.startsWith("MVE_ACK")) {
//               machine.set(MSG_MVE_ACK);
//            }
//        }
//    }


}
