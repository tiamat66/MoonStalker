package si.vajnartech.moonstalker.processor;

import android.util.Log;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.rest.RObjAstroData;

public class CmdGetAstroData extends Controller<RObjAstroData>
{
    public CmdGetAstroData(Processor machine)
    {
        super("getastrodata", machine);
    }

    @Override
    protected RObjAstroData deserialize(BufferedReader br)
    {
        return gson.fromJson(br, RObjAstroData.class);
    }

    @Override
    public RObjAstroData backgroundFunc()
    {
        return callServer(null);
    }

    @Override
    protected void onPostExecute(RObjAstroData rObjAstroData)
    {
        Log.i("Pepe", "Tjorba kavcic");
    }
}
