package com.robic.zoran.moonstalker;

import android.location.GpsSatellite;
import android.util.Log;

/**
 * Created by zoran on 6.3.2016.
 */
public class Control {

    // Delimiters
    private static final String SM = "<";
    private static final String EM = ">";
    private static final String TAG = "control";

    //Messages
    //IN
    private static final String RDY =           "RDY";
    private static final String NOT_RDY =       "NOT_RDY";
    private static final String BTRY_LOW =      "BTRY_LOW";
    private static final String FATAL_ERROR =   "FATAL_ERROR";
    private static final String BTRY_RESULT =   "BTRY";

    //OUT
    private static final String MOVE =  "MV";
    private static final String ST =    "ST?";
    private static final String BTRY =  "BTRY?";


    BlueToothService BTservice;
    private String outMessage;
    Telescope telescope;

    public Control(BlueToothService myBTservice, Telescope myTelescope) {

        BTservice = myBTservice;
        telescope = myTelescope;
        Log.d(TAG, "...Control created...");
    }

    /* OUT MESSAGES */
    public void btry() {

        //<BTRY?
        outMessage = SM +
                BTRY +
                EM;

        Log.d(TAG, outMessage);
        BTservice.write(outMessage);

    }
    public void st() {

        // <ST?>
        outMessage = SM +
                ST +
                EM;

        Log.d(TAG, outMessage);
        BTservice.write(outMessage);

    }
    public void move(double h, double v) {

        // send <MV H,V>
        outMessage = SM +
                MOVE + " " + (int)h + "," + (int)v +
                EM;
        Log.d(TAG, outMessage);
        BTservice.write(outMessage);
    }

    public void processMsg(String msg) {

        if(chkMsg(msg, RDY)) {

            Log.d(TAG, "Process RDY message from Arduino");

            telescope.setReady();

            return;
        }

        if(chkMsg(msg, NOT_RDY)) {

            Log.d(TAG, "Process NOT_RDY message from Arduino");

            telescope.clearReady();
            return;
        }

        if(chkMsg(msg, BTRY_RESULT)) {

            Log.d(TAG, "Process BTRY_RESULT message from Arduino");
            btryVoltage(msg, BTRY_RESULT);
            return;
        }

        if(chkMsg(msg, FATAL_ERROR)) {

            Log.d(TAG, "Process FATAL_ERROR message from Arduino");
            telescope.clearReady();
            return;
        }

        if(chkMsg(msg, BTRY_LOW)) {

            Log.d(TAG, "Process BTRY_LOW message from Arduino");
            telescope.clearReady();
            return;
        }

        Log.d(TAG, "Unknown message received from Arduino");
    }

    private boolean chkMsg(String recMsg, String expMsg)
    {
        recMsg = recMsg.substring(1, 1+expMsg.length());
        return(recMsg.equals(expMsg));
    }

    private void btryVoltage(String recMsg, String expMsg) {

        String btryVoltage;
        btryVoltage = recMsg.substring(2+expMsg.length(), recMsg.length() -2);
        telescope.setBtryVoltage(Double.valueOf(btryVoltage));
        telescope.mainActivity.updateStatus();
    }
}


