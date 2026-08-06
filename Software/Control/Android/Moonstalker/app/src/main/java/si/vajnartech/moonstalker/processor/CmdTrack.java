package si.vajnartech.moonstalker.processor;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.rest.ObjController;
import si.vajnartech.moonstalker.rest.RObjController;

public class CmdTrack extends Controller<RObjController>
{
    protected ObjController data;

    public CmdTrack(Processor machine, ObjController data)
    {
        super("track", machine);
        this.data = data;
    }

    @Override
    protected RObjController deserialize(BufferedReader br)
    {
        return gson.fromJson(br, RObjController.class);
    }

    @Override
    public RObjController backgroundFunc()
    {
        return callServer(data);
    }

    @Override
    protected void onPostExecute(RObjController rObjController)
    {
        // zaenkrat je komanda posredovana dobimo message ki je sent in neki text
        // vse ostalo je na pingu
    }
}
