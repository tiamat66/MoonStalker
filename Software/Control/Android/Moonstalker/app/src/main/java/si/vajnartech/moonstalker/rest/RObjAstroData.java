package si.vajnartech.moonstalker.rest;

import java.util.HashMap;

import si.vajnartech.moonstalker.processor.CelestialObj;

public class RObjAstroData
{
    public HashMap<String, CelestialObj>  data;
    
    public RObjAstroData(RObjAstroData val)
    {
        if (val != null && val.data != null)
            data = new HashMap<>(val.data);
    }
}

