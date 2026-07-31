package si.vajnartech.moonstalker.processor;

import static si.vajnartech.moonstalker.C.CALIBRATOR;
import static si.vajnartech.moonstalker.OpCodes.CALIBRATED;
import static si.vajnartech.moonstalker.OpCodes.CALIBRATING;
import static si.vajnartech.moonstalker.OpCodes.CONNECT;
import static si.vajnartech.moonstalker.OpCodes.CONN_ERROR;
import static si.vajnartech.moonstalker.OpCodes.GOT_ASTRO_DATA;
import static si.vajnartech.moonstalker.OpCodes.MOVE_END;
import static si.vajnartech.moonstalker.OpCodes.MSG_BATTERY;
import static si.vajnartech.moonstalker.OpCodes.MSG_BATTERY_RES;
import static si.vajnartech.moonstalker.OpCodes.MSG_CONN_TIMEOUT;
import static si.vajnartech.moonstalker.OpCodes.MSG_INFO;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import si.vajnartech.moonstalker.ControlFragment;
import si.vajnartech.moonstalker.MainActivity;
import si.vajnartech.moonstalker.ManualMoveFragment;
import si.vajnartech.moonstalker.OpCodes;
import si.vajnartech.moonstalker.R;
import si.vajnartech.moonstalker.rest.ObjController;
import si.vajnartech.moonstalker.rest.RObjAstroData;
import si.vajnartech.moonstalker.telescope.Status;

/**
 * Processor handles the telescope control logic by dispatching commands and UI updates
 * based on operational codes (OpCodes). It manages a background IO thread and main UI queue.
 */
public class Processor {
    public final Status status = new Status();
    private final MainActivity act;
    private final Handler ioQueue;
    private final Handler uxQueue;
    private final Map<Integer, Ball> actions = new HashMap<>();
    private final HandlerThread ioThread;

    public Processor(MainActivity act) {
        this.act = act;
        this.uxQueue = new Handler(Looper.getMainLooper());

        this.ioThread = new HandlerThread("ProcessorIO");
        this.ioThread.start();
        this.ioQueue = new Handler(ioThread.getLooper());

        initTable();
    }

    /**
     * Safely shuts down the processor's background thread.
     */
    public void quit() {
        ioThread.quitSafely();
    }

    // --- State setters ---

    public void set(int id) {
        set(id, null, null, null, null);
    }

    public void set(int id, RObjAstroData data) {
        set(id, null, null, null, data);
    }

    public void set(int id, Integer newState, Integer newMode) {
        set(id, null, newState, newMode, null);
    }

    public void set(int id, ObjController message) {
        set(id, message, null, null, null);
    }

    /**
     * Core dispatch method. It schedules actions on IO and UX queues.
     * State updates are synchronized on the status object to ensure consistency.
     */
    public void set(int id, final ObjController message, final Integer newState, final Integer newMode, final RObjAstroData data) {
        Ball ball = actions.get(id);
        if (ball == null) return;

        if (ball.ioAction != null) {
            ioQueue.post(() -> execute(ball.ioAction, message, newState, newMode, data));
        }
        if (ball.uxAction != null) {
            uxQueue.post(() -> execute(ball.uxAction, message, newState, newMode, data));
        }
    }

    private void execute(Runnable action, ObjController message, Integer newState, Integer newMode, RObjAstroData data) {
        synchronized (status) {
            // Apply new state parameters
            if (newMode != null) status.mode.set(newMode);
            if (data != null) status.data = data;
            if (message != null) status.message = message;

            // Execute the action mapped to the opcode
            action.run();
        }
    }

    public Handler getIoQueue() {
        return ioQueue;
    }

    private void initTable() {
        // Error handling
        actions.put(OpCodes.ERROR, new Ball(null, () -> {
            if (status.message != null) {
                act.logMessage(status.message.p2);
                if (!"NOT_RDY".equals(status.message.p1)) {
                    act.setInfoMessage(R.string.error);
                    act.updateFab(R.color.colorError);
                }
            }
            status.set(OpCodes.ERROR);
        }));

        // Connection sequence
        actions.put(OpCodes.CONNECTING, new Ball(null, () -> {
            act.setInfoMessage(R.string.connecting);
            status.set(OpCodes.CONNECTING);
        }));

        actions.put(OpCodes.POS_UPDATE, new Ball(null, () -> {
            try {
                if (status.message != null) {
                    double el = Double.parseDouble(status.message.p2);
                    double az = Double.parseDouble(status.message.p3);
                    act.setPosMessage(el, az);
                }
            } catch (NumberFormatException ignored) {}
        }));

        actions.put(OpCodes.POSITION, new Ball(() -> new CmdPosition(this), null));
        actions.put(OpCodes.GET_ASTRO_DATA, new Ball(() -> new CmdGetAstroData(this), null));

        actions.put(OpCodes.CONNECTED, new Ball(
                () -> {
                    Log.i("PEPE", "CONNECTED " + status.get());
                    if (status.get() == OpCodes.MOVING) set(OpCodes.POSITION);
                    else set(OpCodes.GET_ASTRO_DATA);
                    status.set(OpCodes.CONNECTED);
                },
                () -> {
                    act.setInfoMessage(R.string.connected);
                    act.updateMenu(true, true, false, false);
                }
        ));

        actions.put(CONNECT, new Ball(
                () -> {
                    new CmdStatus(this);
                    set(OpCodes.CONNECTING, OpCodes.CONNECTING, null);
                },
                null
        ));

        actions.put(CONN_ERROR, new Ball(null, () -> {
            act.setInfoMessage(R.string.connection_failed);
            act.updateFab(R.color.colorError);
            act.logMessage("...connection error");
            status.set(CONN_ERROR);
        }));

        // Data synchronization
        actions.put(GOT_ASTRO_DATA, new Ball(null, () -> act.objectsDatabase = status.data));

        // Movement control
        actions.put(OpCodes.MOVE_START, new Ball(() -> new CmdMoveStart(this, status.message), null));

        actions.put(OpCodes.MOVING, new Ball(null, () -> {
            act.setInfoMessage(R.string.moving);
            status.set(OpCodes.MOVING);
        }));

        actions.put(MSG_CONN_TIMEOUT, new Ball(null, () -> act.setInfoMessage(R.string.timeout)));

        // Calibration sequence
        actions.put(CALIBRATING, new Ball(null, () -> {
            act.setFragment("manual", ManualMoveFragment.class, new Bundle());
            act.promptToCalibration();
            act.setInfoMessage(R.string.calibrating);
            act.updateFab(R.color.colorOk);
        }));

        actions.put(CALIBRATED, new Ball(
                () -> new CmdCalibrated(this),
                () -> {
                    act.curObjName = CALIBRATOR;
                    if (act.objectsDatabase != null && act.objectsDatabase.data != null) {
                        act.curObject = act.objectsDatabase.data.get(CALIBRATOR);
                    }
                    act.setFragment("control", ControlFragment.class, new Bundle());
                    act.updateMenu(false, true, true, true);
                    act.setInfoMessage(R.string.calibrated);
                }
        ));

        // Status updates
        actions.put(OpCodes.READY, new Ball(null, () -> {
            act.setInfoMessage(R.string.ready);
            act.updateFab(R.color.colorOk);
            if (status.message != null) {
                act.logMessage(status.message.p2);
            }
            status.set(OpCodes.READY);
        }));

        actions.put(MSG_INFO, new Ball(null, () -> {
            if (status.message != null) {
                act.logMessage("..." + status.message);
            }
        }));

        actions.put(MSG_BATTERY, new Ball(() -> new CmdBattery(this), null));

        actions.put(OpCodes.MOVE, new Ball(() -> new CmdMove(this, status.message),
                () -> act.setInfoMessage(R.string.moving)));

        actions.put(MSG_BATTERY_RES, new Ball(null, () -> act.setInfoMessage(R.string.btry_voltage)));

        actions.put(MOVE_END, new Ball(() -> new CmdMoveEnd(this), null));
    }
}
