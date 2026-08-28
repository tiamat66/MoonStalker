package si.vajnartech.moonstalker.processor;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.rest.ObjController;

public class CmdReset extends Controller<ObjController>
{
    public CmdReset(Processor machine)
    {
        super("reset", machine);
    }

    @Override
    protected ObjController deserialize(BufferedReader br)
    {
        return gson.fromJson(br, ObjController.class);
    }

    @Override
    public ObjController backgroundFunc()
    {
        return callServer(null);
    }

    @Override
    protected void onPostExecute(ObjController objController)
    {
    }
}
