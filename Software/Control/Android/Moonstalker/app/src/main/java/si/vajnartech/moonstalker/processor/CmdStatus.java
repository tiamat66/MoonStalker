package si.vajnartech.moonstalker.processor;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.rest.RObjController;

public class CmdStatus extends Controller<RObjController>
{
    public CmdStatus(Processor machine)
    {
        super("connect", machine);
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
    protected void onPostExecute(RObjController res)
    {
        // zaenkrat v tej komandi ni handlinga, vse se nanasa na ping
    }
}
