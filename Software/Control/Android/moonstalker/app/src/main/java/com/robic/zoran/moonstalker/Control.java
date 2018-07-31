package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.robic.zoran.moonstalker.rest.Drive;
import com.robic.zoran.moonstalker.rest.Instruction;

import java.util.LinkedList;

import static com.robic.zoran.moonstalker.Telescope.ST_MOVING;
import static com.robic.zoran.moonstalker.Telescope.ST_NOT_CAL;
import static com.robic.zoran.moonstalker.Telescope.ST_READY;
import static com.robic.zoran.moonstalker.Telescope.ST_TRACING;

public class Control
{
  private static final String TAG = "IZAA";
  private boolean isSocketFree;

  // Messages
  // TODO: Naredi list msg+stevilka
  static final String MSG_READY   = "RDY";    // RDY
  static final String MSG_BATTERY = "BTRY";   // BTRY,11.7
  static final String MSG_ERROR   = "ERROR";  // ERROR,1     [1=battery low]
  static final String MSG_INIT    = "INIT";   // INIT

  static final int READY   = 2;
  static final int BATTERY = 3;
  static final int ERROR   = 5;
  static final int INIT    = 10;

  // Commands
  static final String CMD_MOVE    = "MV";    // MV,7,-12
  static final String CMD_STATUS  = "ST";    // ST
  static final String CMD_BATTERY = "BTRY";  // BTRY

  static final int MOVE        = 7;
  static final int GET_STATUS  = 1;
  static final int GET_BATTERY = 3;

  private InMessageHandler  inMessageHandler;
//  private OutMessageHandler outMessageHandler;
  private MainActivity      act;
  private Telescope         t;
  private CommandProcessor  proc;

  Control(Telescope t, MainActivity act)
  {
    this.act = act;
    this.t = t;
    inMessageHandler = new InMessageHandler();
//    outMessageHandler = new OutMessageHandler(this);
    isSocketFree = true;
    proc = new CommandProcessor(this);
  }

  public void move(int hSteps, int vSteps)
  {
    Bundle b = new Bundle();
    b.putInt("arg1", hSteps);
    b.putInt("arg2", vSteps);
    act.getCtr().outMessageProcess(Control.MOVE, b);
  }

  private void postCommand(String outMessage)
  {
    Log.i(TAG, "Sending to Server: " + outMessage);
    if (DeviceIO.EMULATED)
      act.getDevice().write(outMessage, DeviceIO.EMULATED);
    else
      act.getDevice().write(outMessage);
  }

  private void outMessageProcess(int msg)
  {
    switch (msg) {
    case GET_STATUS:
      proc.add(new Instruction(Drive.OpCodes.ST));
      break;
    case GET_BATTERY:
      proc.add(new Instruction(Drive.OpCodes.BTRY));
      break;
    }
  }

  private void outMessageProcess(int msg, Bundle bundle)
  {
//    switch (msg) {
//    case GET_BATTERY:
//      outMessageHandler.obtainMessage(msg).sendToTarget();
//      break;
//    case GET_STATUS:
//      outMessageHandler.obtainMessage(msg).sendToTarget();
//      break;
//    case MOVE:
//      outMessageHandler.obtainMessage(msg, bundle).sendToTarget();
//      break;
//    }
  }

  public void inMsgProcess(int msg, Bundle bundle)
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
  class InMessageHandler extends Handler
  {
    @Override
    public void handleMessage(Message message)
    {
      String inMessage = "";
      Bundle b         = (Bundle) message.obj;
      switch (message.what) {
      case READY:
        inMessage = MSG_READY;
        processReady();
        break;
      case BATTERY:
        inMessage = MSG_BATTERY + "," + b.getFloat("p1");
        t.batteryVoltage(b.getFloat("p1"));
        break;
      case ERROR:
        inMessage = MSG_ERROR + "," + b.getString("p1");
        t.p.setError(b.getString("p1"));
        break;
      case INIT:
        inMessage = MSG_INIT;
        outMessageProcess(GET_STATUS);
        outMessageProcess(GET_BATTERY);
        break;
      default:
        return;
      }
      Log.i(Telescope.TAG, "Get message and process it from Server: " + inMessage);
    }
  }

  class CommandProcessor
  {
    LinkedList<Instruction> l = new LinkedList<>();
    boolean r;
    Control ctrl;

    CommandProcessor(Control ctrl)
    {
      this.ctrl = ctrl;
      r = true;
      processorStart();
    }

    void processorStart()
    {
      final String URL = "Xperia L1";
      new Thread()
      {
        @Override public void run()
        {
          while (r) {
            try {
              Thread.sleep(100);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            if (!isSocketFree || l.isEmpty()) continue;
            // process command
            lock();
            new Drive(URL, act, ctrl, l.removeFirst());

          }
        }
      }.start();
    }

    void add(Instruction i)
    {
      l.addLast(i);
    }
  }

  private void processReady()
  {
    Log.i(TAG, "processReady in Control");
    switch (t.p.getStatus()) {
    case ST_NOT_CAL:
      break;
    case ST_MOVING:
      t.p.setStatus(ST_READY);
      break;
    case ST_TRACING:
      t.p.setStatus(ST_TRACING);
    }
  }

  private void lock()
  {
    isSocketFree = false;
  }

  public void release()
  {
    isSocketFree = true;
  }
}


