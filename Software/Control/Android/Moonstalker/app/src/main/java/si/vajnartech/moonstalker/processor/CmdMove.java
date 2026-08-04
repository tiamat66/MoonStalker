package si.vajnartech.moonstalker.processor;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.rest.ObjController;

public class CmdMove extends Controller<ObjController>
{
    protected ObjController object;
    public CmdMove(Processor machine, ObjController object)
    {
        super("move", machine);
        this.object = object;
    }

    @Override
    protected ObjController deserialize(BufferedReader br)
    {
        return gson.fromJson(br, ObjController.class);
    }

    @Override
    public ObjController backgroundFunc()
    {
        return callServer(object);
    }

    @Override
    protected void onPostExecute(ObjController objController)
    {
    }
}

