package si.vajnartech.moonstalker.processor;

import si.vajnartech.moonstalker.rest.RestBase;

public abstract class Controller<R> extends RestBase<String, R>
{
    public static String URL = "http://192.168.1.101:8001/";
    public static String PWD = "moonstalker";
    public static String USR = "moonstalker";

    public Controller(String cmd, Processor machine)
    {
        super(Controller.URL + "controller/" + cmd,
                Controller.USR, Controller.PWD,
                Controller.URL + "token/", machine);
    }

    protected String getParams(String val)
    {
        String[] res = val.split(" ");
        if (res.length == 2) return res[1];
        if (res.length == 3) return res[1] + " " + res[2];
        return "";
    }
}
