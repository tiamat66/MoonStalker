package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import static com.robic.zoran.moonstalker.Telescope.ERROR;
import static com.robic.zoran.moonstalker.Telescope.OK;

class Control
{
    static final int timeout = 1000;
    private static final String TAG = "Control";

    // Messages
    private static final String MSG_READY =        "RDY";
    private static final String MSG_NOT_READY =    "NOT_RDY";
    private static final String MSG_BATTERY_LOW =  "BTRY_LOW";
    static final         String MSG_BATTERY_RES =  "BTRY";
    private static final String MSG_FATAL_ERROR =  "FATAL_ERROR";
    private static final String MSG_ALT_NEGATIVE = "ALT_NEGATIVE";
    private static final String MSG_ALT_POSITIVE = "ALT_POSITIVE";
    private static final String MSG_BUSY =         "BUSY";
    private static final String MSG_IDLE =         "IDLE";
    private static final String MSG_INIT =         "INIT";
    private static final int READY  = 1;
    private static final int NOT_READY = 2;
    private static final int BATTERY_RES = 3;
    private static final int BATTERY_LOW = 4;
    private static final int FATAL_ERROR = 5;
    static final int         ALT_NEGATIVE = 6;
    static final int         ALT_POSITIVE = 7;
    static private final int BUSY = 8;
    static private final int IDLE = 9;
    static final int         INIT = 10;

    // Commands
    private static final String CMD_MOVE =    "MV";
    private static final String CMD_STATUS =  "<ST?>";
    private static final String CMD_BATTERY = "<BTRY?>";
    static final int         MOVE    = 1;
    private static final int STATUS  = 2;
    private static final int BATTERY = 3;

    private InMessageHandler inMessageHandler;
    private MainActivity act;
    private Telescope t;

    Control(Telescope t, MainActivity act)
    {
        this.act = act;
        this.t = t;
        inMessageHandler = new InMessageHandler();
    }

    private void postCommand(String outMessage)
    {
        Log.d(TAG, outMessage);
        act.getDevice().write(outMessage);
        sleep(timeout);
    }

    private void sleep(int ms)
    {
        try {Thread.sleep(ms);} catch (InterruptedException e) {e.printStackTrace();}
    }

    void outMessage(int msg, String p1, String p2)
    {
        String outMessage;
        switch (msg) {
            case MOVE:    outMessage = "<" + CMD_MOVE + " " + p1 + "," + p2 + ">"; break;
            default: return;
        }
        postCommand(outMessage);
    }

    private void outMessage(int msg)
    {
        String outMessage;
        switch (msg) {
            case BATTERY: outMessage = CMD_BATTERY; break;
            case STATUS:  outMessage = CMD_STATUS; break;
            default: return;
        }
        postCommand(outMessage);
    }

    void inMsgProcess(int msg)
    {
        String message;
        switch (msg)
        {
            case INIT:
                outMessage(STATUS);
                if (!t.calibrated)
                    act.messagePrompt(
                            "Calibration",
                            "Manually move the telescope to object Polaris" +
                                    " and then click CALIBRATED");
                outMessage(BATTERY);
                message = MSG_INIT;
                break;
            case ALT_POSITIVE:
                message = MSG_ALT_POSITIVE;
                t.setReady(ERROR);
                t.setHNegative(false);
                break;
            case BUSY:
                message = MSG_BUSY;
                t.setReady(Telescope.BUSY);
                break;
            case IDLE:
                message = MSG_IDLE;
                t.setReady(OK);
                break;
            default:
                message = "Unknown message received: " + msg;
        }
        Log.i(Telescope.TAG, message);
    }

    void inMsgProcess(String msg, int bytes, byte[] buffer)
    {
        Log.i(TAG, msg);
        if (chkMsg(msg, MSG_READY))
            inMessageHandler.obtainMessage(READY).sendToTarget();
        else
        if (chkMsg(msg, MSG_NOT_READY))
            inMessageHandler.obtainMessage(NOT_READY).sendToTarget();
        else
        if (chkMsg(msg, MSG_BATTERY_RES))
            inMessageHandler.obtainMessage(BATTERY_RES, bytes, -1, buffer).sendToTarget();
        else
        if (chkMsg(msg, MSG_BATTERY_LOW))
            inMessageHandler.obtainMessage(BATTERY_LOW);
        else
            Log.d(TAG, "Unknown message received from Telescope Control.");
    }

    private boolean chkMsg(String recMsg, String expMsg)
    {
        recMsg = recMsg.substring(1, 1 + expMsg.length());
        return (recMsg.equals(expMsg));
    }

    @SuppressLint("HandlerLeak")
    class InMessageHandler extends Handler
    {
        @Override
        public void handleMessage(Message message)
        {
            String inMessage;
            switch (message.what)
            {
                case READY:
                    inMessage = MSG_READY;
                    t.setReady(OK);
                    break;
                case NOT_READY: inMessage = MSG_NOT_READY;
                    t.setReady(ERROR);
                    break;
                case BATTERY_RES:
                    byte[] readBuf = (byte[]) message.obj;
                    String rcvdMsg = new String(readBuf, 0, message.arg1);
                    inMessage = rcvdMsg;
                    t.batteryVoltage(rcvdMsg);
                    break;
                case BATTERY_LOW:
                    inMessage = MSG_BATTERY_LOW;
                    t.setBatteryOk(false);
                    t.setReady(ERROR);
                    break;
                case ALT_NEGATIVE:
                    inMessage = MSG_ALT_NEGATIVE;
                    t.setReady(ERROR);
                    t.setHNegative(true);
                    break;
                default:return;
            }
            Log.d(Telescope.TAG, inMessage);
            sleep(timeout);
        }
    }
}


