package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import static com.robic.zoran.moonstalker.Telescope.ST_MOVING;
import static com.robic.zoran.moonstalker.Telescope.ST_NOT_CAL;
import static com.robic.zoran.moonstalker.Telescope.ST_TRACING;

class Control
{
  private static final String TAG = "IZAA";

  // Messages
  // TODO: Naredi list msg+stevilka
  static final String MSG_READY   = "RDY";    // RDY
  static final String MSG_BATTERY = "BTRY";   // BTRY,11.7
  static final String MSG_ERROR   = "ERROR";  // ERROR,1     [1=battery low]
  static final String MSG_INIT    = "INIT";   // INIT

  static final int    READY   = 1;
  static final int    BATTERY = 3;
  static final int    ERROR   = 5;
  static final int    INIT    = 10;

  // Commands
  static final String CMD_MOVE    = "MV";    // MV,7,-12
  static final String CMD_STATUS  = "ST";    // ST
  static final String CMD_BATTERY = "BTRY";  // BTRY

  static final int    MOVE        = 1;
  static final int    GET_STATUS  = 2;
  static final int    GET_BATTERY = 3;

  private InMessageHandler  inMessageHandler;
  private OutMessageHandler outMessageHandler;
  private MainActivity act;
  private Telescope t;

  Control(Telescope t, MainActivity act)
  {
    this.act = act;
    this.t = t;
    inMessageHandler =  new InMessageHandler();
    outMessageHandler = new OutMessageHandler();
  }

  private void postCommand(String outMessage)
  {
    Log.i(TAG, "Sending to Server: " + outMessage);
    if (DeviceIO.EMULATED)
      act.getDevice().write(outMessage, DeviceIO.EMULATED);
    else
      act.getDevice().write(outMessage);
  }

  void outMessageProcess(int msg, Bundle bundle)
  {
    switch (msg) {
      case GET_BATTERY:
        outMessageHandler.obtainMessage(msg).sendToTarget();
        break;
      case GET_STATUS:
        outMessageHandler.obtainMessage(msg).sendToTarget();
        break;
      case MOVE:
        outMessageHandler.obtainMessage(msg, bundle).sendToTarget();
        break;
    }
  }

  void inMsgProcess(int msg, Bundle bundle)
  {
    switch (msg) {
      case READY:
        inMessageHandler.obtainMessage(msg).sendToTarget();
        break;
      case BATTERY:
        inMessageHandler.obtainMessage(msg, bundle).sendToTarget();
        break;
      case ERROR:
        inMessageHandler.obtainMessage(msg, bundle).sendToTarget();
        break;
      case INIT:
        inMessageHandler.obtainMessage(msg).sendToTarget();
        break;
    }
  }

  @SuppressLint("HandlerLeak")
  class OutMessageHandler extends Handler
  {
    @Override
    public void handleMessage(Message message)
    {
      String outMessage = "";
      switch (message.what)
      {
        case MOVE:
          Bundle b = (Bundle) message.obj;
          outMessage = CMD_MOVE + "," + b.getInt("arg1")
            + "," + b.getInt("arg2");
          if (t.p.getStatus() != ST_TRACING) t.p.setStatus(ST_MOVING);
          act.statusBar.setPosition(t.getPos());
          break;
        case GET_BATTERY:
          outMessage = CMD_BATTERY;
          break;
        case GET_STATUS:
          outMessage = CMD_STATUS;
          break;
      }
      postCommand(outMessage);
    }
  }

  @SuppressLint("HandlerLeak")
  class InMessageHandler extends Handler
  {
    @Override
    public void handleMessage(Message message)
    {
      String inMessage = "";
      Bundle b = (Bundle) message.obj;
      switch (message.what) {
        case READY:
          inMessage = MSG_READY;
          processReady();
          break;
        case BATTERY:
          inMessage = MSG_BATTERY + "," + b.getFloat("arg1");
          t.batteryVoltage(b.getFloat("arg1"));
          break;
        case ERROR:
          inMessage = MSG_ERROR + "," + b.getString("arg1");
          t.p.setError(b.getString("arg1"));
          break;
        case INIT:
          inMessage = MSG_INIT;
          outMessageProcess(GET_STATUS, null);
          outMessageProcess(GET_BATTERY, null);
          break;

        default:
          return;
      }
      Log.i(Telescope.TAG, "Get message and process it from Server: " + inMessage);
    }
  }

  private void processReady()
  {
    switch (t.p.getStatus()) {
      case ST_NOT_CAL:
        act.messagePrompt(
          "Calibration",
          "Manually move the telescope to object Polaris then click CALIBRATED"
        );
        break;
      case ST_MOVING:
        t.p.setStatus(READY);
        break;
      case ST_TRACING:
        t.p.setStatus(ST_TRACING);
    }
  }
}


