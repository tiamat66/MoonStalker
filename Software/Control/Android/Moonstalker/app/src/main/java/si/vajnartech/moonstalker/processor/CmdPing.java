package si.vajnartech.moonstalker.processor;

import static si.vajnartech.moonstalker.C.ST_ERROR;
import static si.vajnartech.moonstalker.OpCodes.MSG_CONN_TIMEOUT;
import static si.vajnartech.moonstalker.OpCodes.MSG_INFO;
import static si.vajnartech.moonstalker.OpCodes.MSG_POSITION;
import static si.vajnartech.moonstalker.OpCodes.MSG_READY;
import static si.vajnartech.moonstalker.OpCodes.MSG_WARNING;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.util.Objects;

public class CmdPing extends Controller<String>
{
    public CmdPing(Processor machine)
    {
        super("ping", machine);
    }

    @Override
    protected void onPostExecute(String cmdResult)
    {
        if (cmdResult == null) return;
        
        String msg = getParams(cmdResult);

        if (Objects.equals(cmdResult, "RDY")) {
            machine.set(MSG_READY);
        } else if (Objects.equals(cmdResult, "TIMEOUT")) {
            machine.set(MSG_CONN_TIMEOUT);
        } else if (cmdResult.startsWith("ERROR")) {
            machine.set(ST_ERROR, msg);
        } else if (cmdResult.startsWith("WARNING")) {
            machine.set(MSG_WARNING);
        } else if (cmdResult.startsWith("INFO")) {
            machine.set(MSG_INFO, msg);
        } else if (cmdResult.startsWith("POS")) {
            machine.set(MSG_POSITION, msg);
        }
    }

    @Override
    protected String deserialize(BufferedReader br)
    {
        return new Gson().fromJson(br, String.class);
    }

    @Override
    public String backgroundFunc()
    {
        return callServer(null);
    }
}
