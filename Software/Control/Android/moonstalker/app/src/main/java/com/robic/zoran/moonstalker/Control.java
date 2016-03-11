package com.robic.zoran.moonstalker;

import android.location.GpsSatellite;
import android.util.Log;

/**
 * Created by zoran on 6.3.2016.
 */
public class Control {

    private static final String TAG = "control";
    BlueToothService BTservice;
    private String inMessage;
    private String outMessage;

    public Control(BlueToothService myBTservice) {

        BTservice = myBTservice;
        Log.d(TAG, "...Control created...");
    }

    public void Track() {

    }

    public void move(double h, double v) {

        // send <MV H,V>
        outMessage = "<MV " + h + "," + v + ">";
        Log.d(TAG, outMessage);
                sendMessage(outMessage);
        // wait for <RDY>
        inMessage = "<RDY>";
        Log.d(TAG, "...waiting for " + inMessage + "...");
        waitForMsg(inMessage);
    }

    private void sendMessage(String msg) {

        BTservice.sendMsg(msg);
    }

    private void waitForMsg(String msg) {

        String rcvdMessage;

        BTservice.waitForMsg(); //...thread waits for message from Arduino
        rcvdMessage = BTservice.getRcvdMsg();

        if (rcvdMessage == msg) {
            Log.d(TAG, "..." + msg + " received...");
        }
     }

}


