package si.vajnartech.moonstalker.processor;

import java.util.ArrayList;

public class DataAstroObj
{
    public ArrayList<AstroObj> data;

    public DataAstroObj(DataAstroObj val)
    {
        data = new ArrayList<>();
        data.addAll(val.data);
    }
}
