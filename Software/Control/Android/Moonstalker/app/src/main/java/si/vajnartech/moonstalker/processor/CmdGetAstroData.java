package si.vajnartech.moonstalker.processor;

import static si.vajnartech.moonstalker.OpCodes.MSG_CONN_ERROR;
import static si.vajnartech.moonstalker.OpCodes.MSG_GOT_ASTRO_DATA;

import com.google.gson.Gson;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.rest.RObjController;

public class CmdGetAstroData extends Controller<DataAstroObj>
{
    public CmdGetAstroData(Processor machine)
    {
        super("get_astro_data", machine);
    }

    @Override
    protected DataAstroObj deserialize(BufferedReader br)
    {
        return gson.fromJson(br, DataAstroObj.class);
    }

    @Override
    public DataAstroObj backgroundFunc()
    {
        return callServer(null);
    }

    @Override
    protected void onPostExecute(DataAstroObj dataAstroObj)
    {

    }


//    @Override
//    protected void onPostExecute(DataAstroObj astroData)
//    {
//        if (astroData == null) {
//            machine.set(MSG_CONN_ERROR);
//        } else {
//            machine.set(MSG_GOT_ASTRO_DATA, astroData);
//        }
//    }


}
