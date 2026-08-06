package si.vajnartech.moonstalker.processor;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.OpCodes;
import si.vajnartech.moonstalker.rest.RObjController;

public class CmdCalibrated extends Controller<RObjController>
{

    public CmdCalibrated(Processor machine)
    {
        super("calibrated", machine);
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
        if (rObjController != null && rObjController.success) {
            machine.set(OpCodes.POSITION, OpCodes.CALIBRATED);
        }
    }
}
