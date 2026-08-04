package si.vajnartech.moonstalker.processor;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.OpCodes;
import si.vajnartech.moonstalker.rest.ObjController;
import si.vajnartech.moonstalker.rest.RObjController;

public class CmdPosition extends Controller<RObjController>
{
    public CmdPosition(Processor machine)
    {
        super("position", machine);
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
        if (res == null || !res.success) return;

        String[] s = res.message.split(" ");
        machine.set(OpCodes.POS_UPDATE, new ObjController(s[0], s[1], s[2]));
    }
}
