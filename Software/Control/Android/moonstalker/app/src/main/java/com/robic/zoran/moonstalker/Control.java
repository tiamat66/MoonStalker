package com.robic.zoran.moonstalker;

import android.location.GpsSatellite;

/**
 * Created by zoran on 6.3.2016.
 */
public class Control {

    BlueToothService BTservice;

    public Control(BlueToothService myBTservice) {

        BTservice = myBTservice;
    }

    public void Track() {

    }

    private class Messages {

        private String message;

        public void move(double h, double v) {

            // <MV H,V>
            message = "<MV" + h + "," + v + ">";
        }

        private void sendMessage(String msg) {

            BTservice.sendMsg(message);
        }

        private void waitForMsg(String msg) {

            String rcvdMessage;

            BTservice.waitForMsg(); //...thread waits for message from Arduino
            rcvdMessage = BTservice.getRcvdMsg();

            /* TODO
            if (rcvdMessage == msg)
            Log.d(TAG, "...RDY received...");
             */

        }
    }

}
