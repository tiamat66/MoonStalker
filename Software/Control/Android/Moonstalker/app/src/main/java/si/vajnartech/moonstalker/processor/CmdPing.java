package si.vajnartech.moonstalker.processor;

import static si.vajnartech.moonstalker.OpCodes.MSG_CONN_ERROR;
import static si.vajnartech.moonstalker.OpCodes.MSG_ERROR;
import static si.vajnartech.moonstalker.OpCodes.MSG_INFO;
import static si.vajnartech.moonstalker.OpCodes.MSG_READY;
import static si.vajnartech.moonstalker.OpCodes.MSG_WARNING;

import com.google.gson.Gson;

import java.io.BufferedReader;

public class CmdPing extends Controller<String>
{
    public CmdPing(QueueUI queue)
    {
        super("ping", queue);
    }

    @Override
    protected void onPostExecute(String cmdResult)
    {
        String msg = getParams(cmdResult);

        if (cmdResult.equals("RDY")) {
            queue.obtainMessage(MSG_READY).sendToTarget();
        } else if (cmdResult.equals("TIMEOUT")) {
            queue.obtainMessage(MSG_CONN_ERROR).sendToTarget();
        } else if (cmdResult.startsWith("ERROR")) {
            queue.obtainMessage(MSG_ERROR, msg).sendToTarget();
        }  else if (cmdResult.startsWith("WARNING")) {
            queue.obtainMessage(MSG_WARNING, msg).sendToTarget();
        } else if (cmdResult.startsWith("INFO")) {
            queue.obtainMessage(MSG_INFO, msg).sendToTarget();
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
