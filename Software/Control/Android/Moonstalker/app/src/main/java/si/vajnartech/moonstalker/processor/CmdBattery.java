package si.vajnartech.moonstalker.processor;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.rest.RObjController;

public class CmdBattery extends Controller<RObjController>
{
    public CmdBattery(Processor queue) {
        super("battery", queue);
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
        if (res == null) return;
        if (res.battery != null) machine.status.battery = res.battery;
        if (res.alarm != null) machine.status.alarm = res.alarm;
    }
}
