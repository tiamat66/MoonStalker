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
}


