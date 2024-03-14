package si.vajnartech.moonstalker.telescope;

import static si.vajnartech.moonstalker.C.ST_ASTRO_DATA;
import static si.vajnartech.moonstalker.C.ST_CALIBRATED;
import static si.vajnartech.moonstalker.C.ST_CALIBRATING;
import static si.vajnartech.moonstalker.C.ST_CONNECTION_ERROR;
import static si.vajnartech.moonstalker.C.ST_NOT_CONNECTED;
import static si.vajnartech.moonstalker.C.ST_NOT_READY;
import static si.vajnartech.moonstalker.C.ST_READY;
import static si.vajnartech.moonstalker.C.ST_WAITING;

import android.os.Bundle;

import java.util.ArrayList;

import si.vajnartech.moonstalker.AstroObject;
import si.vajnartech.moonstalker.ControlFragment;
import si.vajnartech.moonstalker.MainActivity;
import si.vajnartech.moonstalker.MainFragment;
import si.vajnartech.moonstalker.ManualFragment;
import si.vajnartech.moonstalker.R;
import si.vajnartech.moonstalker.processor.AstroObj;
import si.vajnartech.moonstalker.processor.DataAstroObj;

public class StateMachine extends Thread
{
    protected boolean running;
    public Status status = new Status();
    public Status mode = new Status();
    private int curStatus, curMode;

    protected MainActivity act;

    public StateMachine(MainActivity act)
    {
        this.act = act;
        running = true;
        mode.set(ST_NOT_READY);
        start();
    }

    /** @noinspection BusyWait*/
    @Override
    public void run()
    {
       while(running) {
           try {
               sleep(1000);
           } catch (InterruptedException e) {
               throw new RuntimeException(e);
           }

           if (curStatus != status.get()) {
               curStatus = status.get();
               if (status.get() == ST_WAITING) {
                   act.setInfoMessage(R.string.waiting);
               } else if (status.get() == ST_NOT_CONNECTED) {
                   act.setInfoMessage(R.string.not_connected);
                   act.updateFab(R.color.colorError);
                   act.logMessage("...not connected");
               } else if (status.get() == ST_READY) {
                   act.setInfoMessage(R.string.ready);
                   act.updateFab(R.color.colorOk2);
                   act.updateMenu(true, true, false, false);
               } else if (status.get() == ST_CONNECTION_ERROR) {
                   act.setInfoMessage(R.string.connection_failed);
                   act.updateFab(R.color.colorError);
                   act.logMessage("...connection error");
               } else if (status.get() == ST_ASTRO_DATA) {
                   act.astroData = new DataAstroObj(status.data);
                   act.setInfoMessage(R.string.ready);
                   act.updateMenu(true, true, false, false);
                   act.updateFab(R.color.colorOk2);
               }

           } else if (curMode != mode.get()) {
               curMode = mode.get();
               if (mode.get() == ST_CALIBRATING) {
                   act.setFragment("manual", ManualFragment.class, new Bundle());
                   act.promptToCalibration();
                   act.setInfoMessage(R.string.calibrating);
               } else if (mode.get() == ST_CALIBRATED) {
                   // potrdi current objekt
                   act.setFragment("control", ControlFragment.class, new Bundle());
                   act.updateMenu(false, true, true, true);
                   act.setInfoMessage(R.string.ready);
               }
           }
       }
    }
}
