package si.vajnartech.moonstalker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.LinkedList;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static si.vajnartech.moonstalker.C.ST_CONNECTED;
import static si.vajnartech.moonstalker.C.ST_MOVING;
import static si.vajnartech.moonstalker.C.ST_MOVING_E;
import static si.vajnartech.moonstalker.C.ST_MOVING_S;
import static si.vajnartech.moonstalker.C.ST_READY;
import static si.vajnartech.moonstalker.C.TAG;
import static si.vajnartech.moonstalker.OpCodes.BATTERY;
import static si.vajnartech.moonstalker.OpCodes.ERROR;
import static si.vajnartech.moonstalker.OpCodes.GET_BATTERY;
import static si.vajnartech.moonstalker.OpCodes.GET_STATUS;
import static si.vajnartech.moonstalker.OpCodes.INIT;
import static si.vajnartech.moonstalker.OpCodes.IN_MSG;
import static si.vajnartech.moonstalker.OpCodes.MOVE;
import static si.vajnartech.moonstalker.OpCodes.MOVE_START;
import static si.vajnartech.moonstalker.OpCodes.MOVE_STOP;
import static si.vajnartech.moonstalker.OpCodes.READY;

interface ControlInterface
{
  void releaseSocket();
  void messageProcess(String msg, Bundle bundle);
}

public class Control extends Telescope
{
  private              boolean isSocketFree;
  private InMessageHandler inMessageHandler;
  private CommandProcessor processor;

  Control(MainActivity act)
  {
    super(act);
    inMessageHandler = new InMessageHandler();
    isSocketFree = true;
    processor = new CommandProcessor(this);
  }

  void moveStart(C.Directions direction)
  {
    TelescopeStatus.set(ST_MOVING_S);
    outMessageProcess(MOVE_START, Integer.toString(direction.getValue()), "");
  }

  void moveStop()
  {
    TelescopeStatus.set(ST_MOVING_E);
    outMessageProcess(MOVE_STOP, "", "");
  }

  @Override
  void mv(int hSteps, int vSteps)
  {
    TelescopeStatus.set(ST_MOVING);
    outMessageProcess(MOVE, Integer.toString(hSteps), Integer.toString(vSteps));
  }

  private void outMessageProcess(String opcode, String p1, String p2)
  {
    Log.i(TAG, "outMessageProcess; socket=" + isSocketFree);
    Instruction i;
    if (p1.isEmpty() && p2.isEmpty())
      i = new Instruction(opcode);
    else if (!p1.isEmpty() && p2.isEmpty())
      i = new Instruction(opcode, p1);
    else
      i = new Instruction(opcode, p1, p2);
    processor.add(i);
  }

  @Override
  public void inMsgProcess(String opcode, Bundle params)
  {
    params.putString("opcode", opcode);
    switch (opcode) {
    case READY:
      inMessageHandler.obtainMessage(IN_MSG, params).sendToTarget();
      break;
    case BATTERY:
      inMessageHandler.obtainMessage(IN_MSG, params).sendToTarget();
      break;
    case ERROR:
      inMessageHandler.obtainMessage(IN_MSG, params).sendToTarget();
      break;
    case INIT:
      inMessageHandler.obtainMessage(IN_MSG, params).sendToTarget();
      break;
    }
  }

  @SuppressLint("HandlerLeak")
  private class InMessageHandler extends Handler
  {
    @Override
    public void handleMessage(Message message)
    {
      if (message.what != IN_MSG)
        return;

      Bundle parms = (Bundle) message.obj;
      String opcode = "";
      if (parms != null)
        opcode = (String) parms.get("opcode");

      assert opcode != null;
      switch (opcode) {
      case READY:
        processReady();
        break;
      case BATTERY:
        TelescopeStatus.setBatteryVoltage(parms.getFloat("p1"));
        break;
      case ERROR:
        TelescopeStatus.setError(parms.getString("p1"));
        break;
      case INIT:
        outMessageProcess(GET_STATUS, "", "");
        outMessageProcess(GET_BATTERY, "", "");
        break;
      default:
        return;
      }

      Log.i(TAG, "Get message and process it from Server: " + opcode);
    }
  }

  class CommandProcessor
  {
    LinkedList<Instruction> instrBuffer = new LinkedList<>();
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
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            if (!isSocketFree || instrBuffer.isEmpty()) continue;
            lock();
            new IOProcessor(instrBuffer.removeFirst(), new ControlInterface() {
              @Override public void releaseSocket()
              {
                release();
              }

              @Override public void messageProcess(String msg, Bundle bundle)
              {
                inMsgProcess(msg, bundle);
              }
            }).executeOnExecutor(THREAD_POOL_EXECUTOR);
          }
        }
      }.start();
    }

    void add(Instruction i)
    {
      instrBuffer.addLast(i);
    }
  }

  private void processReady()
  {
    Log.i(TAG, "processReady in Control = " + TelescopeStatus.get());
    if (TelescopeStatus.get() == ST_CONNECTED) {
      TelescopeStatus.set(ST_READY);
      TelescopeStatus.setMode(ST_READY);
    } else if (TelescopeStatus.get() == ST_MOVING) {
      TelescopeStatus.set(ST_READY);
    } else if (TelescopeStatus.get() == ST_MOVING_E) {
      TelescopeStatus.set(ST_READY);
    }
  }

  private void lock()
  {
    isSocketFree = false;
  }

  private void release()
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



