package si.vajnartech.moonstalker.processor;

import static si.vajnartech.moonstalker.OpCodes.MSG_BATTERY_RES;
import static si.vajnartech.moonstalker.OpCodes.MSG_CONN_ERROR;

import com.google.gson.Gson;

import java.io.BufferedReader;

public class CmdBattery extends Controller<String>
{
    public CmdBattery(QueueUI queue) {
        super("battery", queue);
    }

    @Override
    protected void onPostExecute(String cmdResult)
    {
        String msg = getParams(cmdResult);

        if (cmdResult.startsWith("BTRY")) {
            queue.obtainMessage(MSG_BATTERY_RES, msg).sendToTarget();
        }  else if (cmdResult.equals("TIMEOUT")) {
            queue.obtainMessage(MSG_CONN_ERROR).sendToTarget();
        }
    }

    @Override
    protected String deserialize(BufferedReader br)
    {
        return new Gson().fromJson(br, String.class);
    }

    @Override
    public String backgroundFunc() {
        return callServer(null);
    }
}
