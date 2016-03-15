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
    private String inMessage;
    private String outMessage;
    Thread thread;
    Telescope telescope;

    public Control(BlueToothService myBTservice, Telescope myTelescope) {

        BTservice = myBTservice;
        telescope = myTelescope;
        thread = new Thread(new Runnable() {
            public void run(){

                waitForMsg();
            }
        });
        thread.start();

        Log.d(TAG, "...Control created...");
    }

    public void move(double h, double v) {

        // send <MV H,V>
        outMessage = "<MV " + h + "," + v + ">";
        Log.d(TAG, outMessage);
        sendMessage(outMessage);
    }

    private void sendMessage(String msg) {

        BTservice.write(msg);
    }

    private void waitForMsg() {

        while (true) {

            try {
                inMessage = BTservice.getRcvdMsg();

                // TODO: Do different actions for different messages
                if(inMessage == RDY) {
                    telescope.setReady();
                }

                thread.sleep(1000);
            } catch (InterruptedException e) {

                break;
            }
        }
    }
}


