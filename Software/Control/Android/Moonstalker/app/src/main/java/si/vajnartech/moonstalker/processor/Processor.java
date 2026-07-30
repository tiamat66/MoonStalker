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

import java.util.HashMap;
import java.util.Objects;

import si.vajnartech.moonstalker.ControlFragment;
import si.vajnartech.moonstalker.MainActivity;
import si.vajnartech.moonstalker.ManualMoveFragment;
import si.vajnartech.moonstalker.OpCodes;
import si.vajnartech.moonstalker.R;
import si.vajnartech.moonstalker.rest.ObjController;
import si.vajnartech.moonstalker.rest.RObjAstroData;
import si.vajnartech.moonstalker.telescope.Status;

public class Processor
{
    public final Status status = new Status();
    protected MainActivity act;
    protected Handler ioQueue;
    protected Handler uxQueue;
    protected HashMap<Integer, Ball> actions = new HashMap<>();

    protected HandlerThread ioThread;

    public Processor(MainActivity act)
    {
        this.act = act;
        uxQueue = new Handler(Looper.getMainLooper());
        
        ioThread = new HandlerThread("ProcessorIO");
        ioThread.start();
        ioQueue = new Handler(ioThread.getLooper());

        initTable();
    }

    public void quit()
    {
        ioThread.quitSafely();
    }

//    public void set(int id)
//    {
//        Ball ball = actions.get(id);
//        if (ball != null) {
//            if (ball.ioAction != null) {
//                ioQueue.post(ball.ioAction);
//            }
//            if (ball.uxAction != null) {
//                uxQueue.post(ball.uxAction);
//            }
//        }
//    }

    public void set(int id, RObjAstroData data)
    {
        set(id, null, null, null, data);
    }

    public void set(int id, Integer newState, Integer newMode)
    {
        set(id, null, newState, newMode, null);
    }
    public void set(int id, ObjController message)
    {
        set(id, message, null, null, null);
    }

    public void set(int id)
    {
        set(id, null, null, null, null);
    }

    public void set(int id, final ObjController message, Integer newState, Integer newMode, RObjAstroData data)
    {
        Ball ball = actions.get(id);
        if (ball != null) {
            if (ball.ioAction != null) {
                ioQueue.post(() -> {
                    synchronized (status) {
                        if (newMode != null)
                            status.mode.set(newMode);
                        if (data != null)
                            status.data = data;
                        if (message != null)
                            status.message = message;
                        ball.ioAction.run();
                        // prejsnje stanje se uporablja v odlocitvah
                        if (newState != null)
                            status.set(newState);
                    }
                });
            }
            if (ball.uxAction != null) {
                uxQueue.post(() -> {
                    synchronized (status) {
                        if (newMode != null)
                            status.mode.set(newMode);
                        if (data != null)
                            status.data = data;
                        if (message != null)
                            status.message = message;
                        ball.uxAction.run();
                        if (newState != null)
                            status.set(newState);
                    }
                });
            }
        }
    }

    public Handler getIoQueue()
    {
        return ioQueue;
    }
//
//    public void set(int id, RObjAstroData data)
//    {
//        status.data = data;
//        set(id);
//    }

//    public void set(int id, ObjController message)
//    {
//        status.message = message;
//        set(id);
//    }

    private void initTable()
    {
        actions.put(OpCodes.ERROR, new Ball(null, () -> {
            act.logMessage(status.message.p2);
            if (Objects.equals(status.message.p1, "NOT_RDY"))
                return;
            act.setInfoMessage(R.string.error);
            act.updateFab(R.color.colorError);
        }));

        actions.put(OpCodes.CONNECTING, new Ball(null,
                () -> {
            act.setInfoMessage(R.string.connecting);
        }));
        actions.put(OpCodes.POS_UPDATE, new Ball(null,
                () -> act.setPosMessage(Double.parseDouble(status.message.p2), Double.parseDouble(status.message.p3)))
        );
        actions.put(OpCodes.POSITION, new Ball(() -> new CmdPosition(this), null));
        actions.put(OpCodes.GET_ASTRO_DATA, new Ball(() -> new CmdGetAstroData(this), null));

        actions.put(OpCodes.CONNECTED, new Ball(() -> {
            // MOVING -> CONNECTED
            if (status.get() == OpCodes.MOVING)
                set(OpCodes.POSITION);
            else
                set(OpCodes.GET_ASTRO_DATA);
            }, () -> {
                    act.setInfoMessage(R.string.connected);
                    act.updateMenu(true, true, false, false);
                }));

        actions.put(CONNECT, new Ball(() -> {
            new CmdStatus(this);
            set(OpCodes.CONNECTING, OpCodes.CONNECTING, null);
        }, null));

        actions.put(CONN_ERROR, new Ball(null, () -> {
            act.setInfoMessage(R.string.connection_failed);
            act.updateFab(R.color.colorError);
            act.logMessage("...connection error");
        }));

        actions.put(GOT_ASTRO_DATA, new Ball(null, () -> act.objectsDatabase = status.data));

        actions.put(OpCodes.MOVE_START, new Ball(
                () -> {
                    new CmdMoveStart(this, status.message);
                }, null
        ));
        actions.put(OpCodes.MOVING, new Ball(null,
                () -> {
                    act.setInfoMessage(R.string.moving);
                }));
        // MSG_CONN_TIMEOUT
        actions.put(MSG_CONN_TIMEOUT, new Ball(null,
                () -> act.setInfoMessage(R.string.timeout)));
        // MSG_CALIBRATING
        actions.put(CALIBRATING, new Ball(null,
                () -> {
                    act.setFragment("manual", ManualMoveFragment.class, new Bundle());
                    act.promptToCalibration();
                    act.setInfoMessage(R.string.calibrating);
                    act.updateFab(R.color.colorOk);
                    act.setInfoMessage(R.string.calibrating);
                }));
        // MSG_CALIBRATED
        actions.put(CALIBRATED, new Ball(
                () -> new CmdCalibrated(this),
                () -> {
                    act.curObjName = CALIBRATOR;
                    act.toObjName = CALIBRATOR;
                    act.curObject = act.objectsDatabase.data.get(CALIBRATOR);
                    act.setFragment("control", ControlFragment.class, new Bundle());
                    act.updateMenu(false, true, true, true);
                    act.setInfoMessage(R.string.calibrated);
                }
        ));
        // MSG_READY
        actions.put(OpCodes.READY, new Ball(null,
                () -> {
                    act.setInfoMessage(R.string.ready);
                    act.updateFab(R.color.colorOk);
                    act.logMessage(status.message.p2);
                }
                ));

//        actions.put(MSG_ERROR, new Ball(null,
//                () -> {
//                    if (Objects.equals(status.message, "END_LIMIT_SW_TRIG"))
//                        act.setInfoMessage(R.string.end_limit_sw_trig);
//                    status.set(ST_ERROR);
//                }
//                ));
        // MSG_WARNING
//        actions.put(MSG_WARNING, new Ball(null,
//                () -> {
//                    if (Objects.equals(status.message, "BTRY_LOW"))
//                        act.setInfoMessage(R.string.btry_low);
//                }
//                ));
        // MSG_INFO
        actions.put(MSG_INFO, new Ball(null,
                () -> act.logMessage(String.format("...%s", status.message))));
        // MSG_POSITION
//        actions.put(MSG_POSITION, new Ball(null,
//                () -> {
//                    String[] res = status.message.p1.split(" "); // TODO
//                    act.curObject.setPosition(res[0], res[1]);
//                    act.setPosMessage();
//                }));
        // MSG_BATTERY
        actions.put(MSG_BATTERY, new Ball(() -> new CmdBattery(this),
                null
                ));
        // MSG_MOVE
        actions.put(OpCodes.MOVE, new Ball(() -> new CmdMove(this, status.message),
                null
                ));

        // MSG_BATTERY_RES
        actions.put(MSG_BATTERY_RES, new Ball(null,
                () -> act.setInfoMessage(R.string.btry_voltage)));
        // MSG_MOVE_END
        actions.put(MOVE_END, new Ball(() -> new CmdMoveEnd(this),
                null));

    }
}
