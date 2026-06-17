package si.vajnartech.moonstalker.processor;

import si.vajnartech.moonstalker.rest.ObjController;
import si.vajnartech.moonstalker.rest.RestBase;

public abstract class Controller<R> extends RestBase<ObjController, R>
{
    public static String URL = "http://192.168.7.75:5000/";
    public static String PWD = "password123";
    public static String USR = "android";

    public Controller(String cmd, Processor machine)
    {
        super(URL + "command/" + cmd, machine);
    }
}
