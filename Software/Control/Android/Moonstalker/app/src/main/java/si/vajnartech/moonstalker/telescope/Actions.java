package si.vajnartech.moonstalker.telescope;

import java.util.ArrayList;

import si.vajnartech.moonstalker.AstroObject;

public interface Actions
{
    void updateStatus(int val);
    void updateStatus(int val, String msg);

    void updateStatus(int val, Object msg);
}
