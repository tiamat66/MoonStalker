package si.vajnartech.moonstalker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.LinkedList;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static java.lang.Thread.sleep;
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
import static si.vajnartech.moonstalker.OpCodes.MOVE_ACK;
import static si.vajnartech.moonstalker.OpCodes.MOVE_START;
import static si.vajnartech.moonstalker.OpCodes.MOVE_STOP;
import static si.vajnartech.moonstalker.OpCodes.NOT_READY;
import static si.vajnartech.moonstalker.OpCodes.READY;

interface ControlInterface
{
  void releaseSocket();
  void messageProcess(String msg, Bundle bundle);
  void dump(String str);
}

public class Control extends Telescope
{
  private              boolean isSocketFree;
  private InMessageHandler inMessageHandler;
  private CommandProcessor processor;
  private MainActivity act;
  int fakeA = 0;
  int fakeH = 0;


  Control(MainActivity act)
  {
    super(act);
    this.act = act;
    inMessageHandler = new InMessageHandler();
    isSocketFree = true;
    processor = new CommandProcessor(this, act);
  }

  void moveStart(final C.Directions direction)
  {
    TelescopeStatus.set(ST_MOVING_S);
    outMessageProcess(MOVE_START, Integer.toString(direction.getValue()), "");
    new Thread(new Runnable() {
      @Override public void run()
      {
        while(TelescopeStatus.get() == ST_MOVING_S) {
          switch (direction) {
          case UP:
            fakeH++;
            break;
          case DOWN:
            fakeH--;
            break;
          case LEFT:
            fakeA--;
            break;
          case RIGHT:
            fakeA++;
            break;
          }
          try {
            sleep(500);
            act.runOnUiThread(new Runnable()
            {
              @Override public void run()
              {
                SelectFragment.setTelescopeLocationString(act, fakeA, fakeH);
              }
            });
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }).start();
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
        processBattery(parms.getInt("p1"));
        break;
      case ERROR:
        TelescopeStatus.setError(parms.getString("p1"));
        break;
      case INIT:
        outMessageProcess(GET_STATUS, "", "");
        outMessageProcess(GET_BATTERY, "", "");
        break;
      case NOT_READY:
        processNotReady();
        break;
      case MOVE_ACK:
        processMvAck();
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
    MainActivity            ctx;

    CommandProcessor(Control ctrl, MainActivity act)
    {
      this.ctrl = ctrl;
      ctx = act;
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
              sleep(1000);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            if (!isSocketFree || instrBuffer.isEmpty()) continue;
            lock();
            new IOProcessor(instrBuffer.removeFirst(), new ControlInterface() {
              @Override
              public void releaseSocket()
              {
                release();
              }

              @Override
              public void messageProcess(String msg, Bundle bundle)
              {
                inMsgProcess(msg, bundle);
              }

              @Override
              public void dump(final String str)
              {
                ctx.runOnUiThread(new Runnable() {
                  @Override public void run()
                  {
                    ctx.monitor.update(str);
                  }
                });
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
    TelescopeStatus.unlock();
    if (TelescopeStatus.get() == ST_CONNECTED) {
      TelescopeStatus.set(ST_READY);
      TelescopeStatus.setMode(ST_READY);
    } else if (TelescopeStatus.get() == ST_MOVING) {
      TelescopeStatus.set(ST_READY);
    } else if (TelescopeStatus.get() == ST_MOVING_E) {
      TelescopeStatus.set(ST_READY);
    }
  }

  private void processNotReady()
  {
    TelescopeStatus.lock();
  }

  private void processMvAck()
  {
    TelescopeStatus.lock();
  }

  private void processBattery(int val)
  {
    TelescopeStatus.setBatteryVoltage(val);
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
}


