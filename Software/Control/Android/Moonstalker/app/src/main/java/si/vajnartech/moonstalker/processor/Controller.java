package si.vajnartech.moonstalker.processor;

import si.vajnartech.moonstalker.rest.RestBase;

public abstract class Controller<R> extends RestBase<Command, R>
{
    public static String URL = "http://192.168.1.10:8001/";
    public static String PWD = "AldebaraN7";
    public static String USR = "vajnar";

    public Controller(String cmd, QueueUI queue)
    {
        super(Controller.URL + "controller/" + cmd,
                Controller.USR, Controller.PWD,
                Controller.URL + "token/", queue);
    }

    protected String getParams(String val)
    {
        String[] res = val.split(" ");
        if (res.length > 1) return res[1];
        return "";
    }
}
