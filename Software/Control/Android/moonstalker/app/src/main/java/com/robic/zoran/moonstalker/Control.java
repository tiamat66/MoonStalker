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
    //For debugging msg processing we have simple Arduino emulator
    ArduinoDebug arduinoDebug;

    public Control(BlueToothService myBTservice) {

        BTservice = myBTservice;
        arduinoDebug = new ArduinoDebug();

        Log.d(TAG, "...Control created...");
    }

    public void move(double h, double v) {

        // send <MV H,V>
        outMessage = "<MV " + h + "," + v + ">";
        Log.d(TAG, outMessage);

        //TODO following line is just for Arduino emulator
        outMessage = "<MV>";

        sendMessage(outMessage);

        // wait for <RDY>
        inMessage = "<RDY>";
        Log.d(TAG, "...waiting for " + inMessage + "...");
        waitForMsg(inMessage);
    }

    private void sendMessage(String msg) {

        //TODO: Test the BlueTooth
        //BTservice.sendMsg(msg);

        arduinoDebug.sendMsg(msg);
    }

    private void waitForMsg(String msg) {

        String rcvdMessage = "";

        //TODO: Test the BlueTooth
        //BTservice.waitForMsg(); //...thread waits for message from Arduino
        //rcvdMessage = BTservice.getRcvdMsg();

        arduinoDebug.waitForMsg();
        rcvdMessage = arduinoDebug.getRcvdMsg();

        if (rcvdMessage == msg) {
            Log.d(TAG, "..." + msg + " received...");
        }
     }

}


