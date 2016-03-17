package com.robic.zoran.moonstalker;

import android.location.GpsSatellite;
import android.util.Log;

/**
 * Created by zoran on 6.3.2016.
 */
public class Control {

    private static final String TAG = "control";
    private static final String RDY = "<RDY>";
    BlueToothService BTservice;
    private String outMessage;
    Telescope telescope;

    public Control(BlueToothService myBTservice, Telescope myTelescope) {

        BTservice = myBTservice;
        telescope = myTelescope;
        Log.d(TAG, "...Control created...");
    }

    public void move(double h, double v) {

        // send <MV H,V>
        outMessage = "<MV " + h + "," + v + ">";
        Log.d(TAG, outMessage);
        BTservice.write(outMessage);
    }

    public void processMsg(String msg) {

        Log.d(TAG, "...process message: " + msg + RDY + "...");
        if(msg.equals(RDY)) {

            Log.d(TAG, "Process RDY message from Arduino)");

            telescope.setReady();
        } else {

            Log.d(TAG, "Unknown message received from Arduino");
        }
    }
}


