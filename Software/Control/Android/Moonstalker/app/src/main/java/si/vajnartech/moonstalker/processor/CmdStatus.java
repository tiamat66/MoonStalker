package si.vajnartech.moonstalker.processor;

import static si.vajnartech.moonstalker.OpCodes.MSG_CONN_ERROR;
import static si.vajnartech.moonstalker.OpCodes.MSG_GET_ASTRO_DATA;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.rest.RObjController;

public class CmdStatus extends Controller<RObjController>
{
    public CmdStatus(Processor machine)
    {
        super("get_status", machine);
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

//    /** @noinspection IfCanBeSwitch*/
//    @Override
//    protected void onPostExecute(String cmdResult)
//    {
//        if (cmdResult != null) {
//            if (Objects.equals(cmdResult, "RDY")) {
//                machine.set(MSG_GET_ASTRO_DATA);
//            }
//            else if (Objects.equals(cmdResult, "NOT_RDY")) {
//                machine.set(MSG_NOT_READY);
//            } else if (Objects.equals(cmdResult, "TIMEOUT")) {
//                machine.set(MSG_CONN_ERROR);
//            }
//        }
//    }


    @Override
    protected void onPostExecute(RObjController res)
    {
        // TODO
        if (res.success) {
            machine.set(MSG_GET_ASTRO_DATA);
        } else {
            machine.set(MSG_CONN_ERROR);
        }
    }
}
