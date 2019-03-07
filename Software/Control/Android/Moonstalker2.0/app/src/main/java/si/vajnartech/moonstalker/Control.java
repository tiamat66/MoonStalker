package si.vajnartech.moonstalker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.LinkedList;

import si.vajnartech.moonstalker.rest.IOProcessor;
import si.vajnartech.moonstalker.rest.Instruction;

import static si.vajnartech.moonstalker.C.ST_MOVING;
import static si.vajnartech.moonstalker.C.ST_NOT_CAL;
import static si.vajnartech.moonstalker.C.ST_READY;
import static si.vajnartech.moonstalker.C.ST_TRACING;

public class Control extends Telescope
{
  private static final String  TAG = "IZAA";
  private              boolean isSocketFree;

  // in messages
  private static final int READY   = 2;
  private static final int BATTERY = 3;
  static final         int ERROR   = 5;
  static final         int INIT    = 10;

  // out messages
  static final         int MOVE        = 7;
  private static final int GET_STATUS  = 1;
  private static final int GET_BATTERY = 3;

  private InMessageHandler inMessageHandler;
  private MainActivity     act;
  private CommandProcessor proc;

  Control(MainActivity act)
  {
    super(act);
    inMessageHandler = new InMessageHandler();
    isSocketFree = true;
    proc = new CommandProcessor(this);
  }

  @Override
  void mv(int hSteps, int vSteps)
  {
    if (TelescopeStatus.get() != ST_TRACING)
      TelescopeStatus.set(ST_MOVING);
    outMessageProcess(MOVE, Integer.toString(hSteps), Integer.toString(vSteps));
  }

  private void outMessageProcess(int msg, String p1, String p2)
  {
    Log.i(TAG, "outMessageProcess MOVE, socket=" + isSocketFree);
    switch (msg) {
    case MOVE:
      proc.add(new Instruction(OpCodes.MOVE, p1, p2));
      break;
    }
  }

  private void outMessageProcess(int msg)
  {
    switch (msg) {
    case GET_STATUS:
      proc.add(new Instruction(OpCodes.ST));
      break;
    case GET_BATTERY:
      proc.add(new Instruction(OpCodes.BTRY));
      break;
    }
  }

  @Override public void inMsgProcess(int msg, Bundle bundle)
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
      Bundle b = (Bundle) message.obj;
      switch (message.what) {
      case READY:
        processReady();
        break;
      case BATTERY:
        TelescopeStatus.setBatteryVoltage(b.getFloat("p1"));
        break;
      case ERROR:
        TelescopeStatus.setError(b.getString("p1"));
        break;
      case INIT:
        outMessageProcess(GET_STATUS);
        outMessageProcess(GET_BATTERY);
        break;
      default:
        return;
      }
      Log.i(TAG, "Get message and process it from Server: " + message);
    }
  }

  class CommandProcessor
  {
    LinkedList<Instruction> l = new LinkedList<>();
    boolean                 r;
    Control                 ctrl;

    CommandProcessor(Control ctrl)
    {
      this.ctrl = ctrl;
      r = true;
      processorStart();
    }

    void processorStart()
    {
      new Thread()
      {
        @Override
        public void run()
        {
          while (r) {
            try {
              Thread.sleep(100);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            if (!isSocketFree || l.isEmpty()) continue;
            lock();
            new IOProcessor(l.removeFirst(), BlueTooth.socket, ctrl);
          }
        }
      }.start();
    }

    void add(Instruction i)
    {
      Log.i("IZAA", "Instrukcija = " + i);
      l.addLast(i);
    }
  }

  private void processReady()
  {
    Log.i(TAG, "processReady in Control");
    switch (TelescopeStatus.get()) {
    case ST_NOT_CAL:
      break;
    case ST_MOVING:
    case ST_READY:
      TelescopeStatus.set(ST_READY);
      break;
    case ST_TRACING:
      TelescopeStatus.set(ST_TRACING);
      break;
    }
  }

  private void lock()
  {
    isSocketFree = false;
  }

  public void release()
  {
    Log.i(TAG, "Release--------------------------------------------------------------------------");
    isSocketFree = true;
  }

  @Override
  boolean isLocked()
  {
    return !isSocketFree;
  }
}



