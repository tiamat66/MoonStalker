package si.vajnartech.moonstalker.processor;

import static si.vajnartech.moonstalker.OpCodes.MSG_CONN_ERROR;
import static si.vajnartech.moonstalker.OpCodes.MSG_NOT_READY;

import com.google.gson.Gson;

import java.io.BufferedReader;

public class CmdStatus extends Controller<String>
{
    public CmdStatus(QueueUI queue)
    {
        super("get_status", queue);
    }

    @Override
    protected void onPostExecute(String cmdResult)
    {
        if (cmdResult != null) {
            if (cmdResult.equals("RDY")) {
                new CmdGetAstroData(queue);
            } else if (cmdResult.equals("NOT_RDY")) {
                queue.obtainMessage(MSG_NOT_READY).sendToTarget();
            }  else if (cmdResult.equals("TIMEOUT")) {
                queue.obtainMessage(MSG_CONN_ERROR).sendToTarget();
            }
        }
    }

    @Override
    protected
    String deserialize(BufferedReader br)
    {
        return new Gson().fromJson(br, String.class);
    }

    @Override
    public String backgroundFunc()
    {
        return callServer(null);
    }
}
