package si.vajnartech.moonstalker.telescope;

import static si.vajnartech.moonstalker.C.ST_CALIBRATING;
import static si.vajnartech.moonstalker.C.ST_NOT_CONNECTED;
import static si.vajnartech.moonstalker.C.ST_NOT_READY;
import static si.vajnartech.moonstalker.C.ST_READY;
import static si.vajnartech.moonstalker.C.ST_WAITING;

import android.os.Bundle;

import si.vajnartech.moonstalker.MainActivity;
import si.vajnartech.moonstalker.ManualFragment;
import si.vajnartech.moonstalker.R;

public class StateMachine extends Thread
{
    protected boolean running;
    public Status status = new Status();
    public Status mode = new Status();
    private int curStatus;

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

           if (curStatus == status.get())
               continue;
           curStatus = status.get();
           if (status.get() == ST_WAITING) {
               act.setInfoMessage(R.string.waiting);
           } else if (status.get() == ST_NOT_CONNECTED) {
               act.setInfoMessage(R.string.not_connected);
               act.updateFab(R.color.colorError);
           } else if (status.get() == ST_READY) {
               act.setInfoMessage(R.string.ready);
               act.updateFab(R.color.colorOk2);
               act.updateMenu(true, true, false, false);
           }
       }
    }
}
