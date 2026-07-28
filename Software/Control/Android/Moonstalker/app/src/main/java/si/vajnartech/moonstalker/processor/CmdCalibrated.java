package si.vajnartech.moonstalker.processor;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.rest.ObjController;
import si.vajnartech.moonstalker.rest.RObjController;

public class CmdCalibrated extends Controller<RObjController>
{
    protected ObjController object;

    public CmdCalibrated(Processor machine, ObjController object)
    {
        super("calibrated", machine);
        this.object = object;
    }

    @Override
    protected RObjController deserialize(BufferedReader br)
    {
        return gson.fromJson(br, RObjController.class);
    }

    @Override
    public RObjController backgroundFunc()
    {
        return callServer(object);
    }

    @Override
    protected void onPostExecute(RObjController rObjController) {

    }


//    @Override
//    public Void backgroundFunc()
//    {
//        return callServer(object);
//    }
}
