package si.vajnartech.moonstalker.rest;

import static si.vajnartech.moonstalker.processor.Controller.PWD;
import static si.vajnartech.moonstalker.processor.Controller.USR;

import java.io.BufferedReader;

import si.vajnartech.moonstalker.processor.Processor;

public class Login extends RestBase<ObjLogin, RObjLogin>
{
    protected RestBase<?, ?> task;

    public Login(RestBase<?, ?> task, Processor machine)
    {
        super(machine);
        this.task = task;
    }

    @Override
    protected RObjLogin deserialize(BufferedReader br)
    {
        return gson.fromJson(br, RObjLogin.class);
    }

    @Override
    public RObjLogin backgroundFunc()
    {
        return callServer(new ObjLogin(USR, PWD));
    }

    @Override
    protected void onPostExecute(RObjLogin res)
    {
        if (res != null && res.success) {
            RestBase.setCachedToken(res.token);
            task.execute(res.token);
        }
    }
}
