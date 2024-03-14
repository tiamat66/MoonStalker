package si.vajnartech.moonstalker.processor;

import static si.vajnartech.moonstalker.OpCodes.MSG_CONN_ERROR;
import static si.vajnartech.moonstalker.OpCodes.MSG_GET_ASTRO_DATA;

import com.google.gson.Gson;

import java.io.BufferedReader;

public class CmdGetAstroData extends Controller<DataAstroObj>
{
    public CmdGetAstroData(QueueUI queue)
    {
        super("get_astro_data", queue);
    }

    @Override
    protected void onPostExecute(DataAstroObj astroData)
    {
        if (astroData == null) {
            queue.obtainMessage(MSG_CONN_ERROR).sendToTarget();
        } else {
            queue.obtainMessage(MSG_GET_ASTRO_DATA, astroData).sendToTarget();
        }
    }

    @Override
    protected DataAstroObj deserialize(BufferedReader br)
    {
        return new Gson().fromJson(br, DataAstroObj.class);
    }

    @Override
    public DataAstroObj backgroundFunc()
    {
        return callServer(null);
    }
}
