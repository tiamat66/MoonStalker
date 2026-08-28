package si.vajnartech.moonstalker.processor;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.rest.RObjController;

public class CmdMoveEnd extends Controller<RObjController>
{
    public CmdMoveEnd(Processor queue)
    {
        super("moveend", queue);
    }

    @Override
    protected RObjController deserialize(BufferedReader br)
    {
        return gson.fromJson(br, RObjController.class);
    }

    @Override
    public RObjController backgroundFunc()
    {
        return callServer(null);
    }

    @Override
    protected void onPostExecute(RObjController rObjController)
    {

    }
}
