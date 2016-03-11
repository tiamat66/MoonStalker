package com.robic.zoran.moonstalker;

/**
 * Created by zoran on 11.3.2016.
 */
public class ArduinoDebug {

    String crntProcessingMsg = "";
    String rcvdMsg = "";

    public void sendMsg(String msg) {

        if(msg == "<MV>") {
            rcvdMsg = "<RDY>";
        }
    }

    public String getRcvdMsg() {
        return rcvdMsg;
    }

    public void waitForMsg() {

    }
}
