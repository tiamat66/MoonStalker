package si.vajnartech.moonstalker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static java.lang.Thread.sleep;
import static si.vajnartech.moonstalker.C.*;
import static si.vajnartech.moonstalker.OpCodes.*;

interface ControlInterface
{
  void releaseSocket();

  void messageProcess(String msg, Bundle bundle);

  void dump(String str);
}

@SuppressWarnings("SameParameterValue")
public class Control extends Telescope
{
  private AtomicBoolean isSocketFree;

  private InMessageHandler inMessageHandler;
  private CommandProcessor processor;
  private MainActivity     act;

  private int fakeA = 0;
  private int fakeH = 0;


  Control(MainActivity act)
  {
    super(act);
    this.act = act;
    inMessageHandler = new InMessageHandler();
    isSocketFree = new AtomicBoolean(true);
    processor = new CommandProcessor(this, act);
  }

  private void onManualMoving(final String direction)
  {
    new Thread(new Runnable()
    {
      @Override public void run()
      {
        while (TelescopeStatus.get() == ST_MOVING &&
               (TelescopeStatus.getMode() == ST_CALIBRATING || TelescopeStatus.getMode() == ST_MANUAL)) {
          switch (direction) {
          case "N":
            fakeH++;
            break;
          case "S":
            fakeH--;
            break;
          case "W":
            fakeA--;
            break;
          case "E":
            fakeA++;
            break;
          case "NE":
            fakeH++;
            fakeA++;
            break;
          case "SE":
            fakeH--;
            fakeA++;
            break;
          case "SW":
            fakeH--;
            fakeA--;
            break;
          case "NW":
            fakeH++;
            fakeA--;
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

  void moveStart(final String direction)
  {
    if (TelescopeStatus.locked()) return;
    TelescopeStatus.set(ST_WAITING_ACK);
    outMessageProcess(MOVE_START, direction, "500");
  }

  void moveStop()
  {
    if (TelescopeStatus.locked()) return;
    TelescopeStatus.set(ST_WAITING_ACK);
    outMessageProcess(MOVE_STOP);
  }

  @Override
  void mv(int hSteps, int vSteps, int speed)
  {
    TelescopeStatus.set(ST_MOVING);
//    outMessageProcess(MOVE, Integer.toString(hSteps), Integer.toString(vSteps));
  }

  @Override
  void st()
  {
    outMessageProcess(GET_STATUS);
  }

  private void outMessageProcess(String opcode)
  {
    processor.add(new Instruction(opcode));
  }

  private void outMessageProcess(String opcode, String p1)
  {
    processor.add(new Instruction(opcode, p1));
  }

  private void outMessageProcess(String opcode, String p1, String p2)
  {
    processor.add(new Instruction(opcode, p1, p2));
  }

  public void outMessageProcess(String opcode, String p1, String p2, String p3)
  {
    processor.add(new Instruction(opcode, p1, p2, p3));
  }

  @Override
  public void inMsgProcess(String opcode, Bundle params)
  {
    params.putString("opcode", opcode);
    inMessageHandler.obtainMessage(IN_MSG, params).sendToTarget();
  }

  @SuppressLint("HandlerLeak")
  private class InMessageHandler extends Handler
  {
    @Override
    public void handleMessage(Message message)
    {
      if (message.what != IN_MSG)
        return;


      Bundle parms  = (Bundle) message.obj;
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
        outMessageProcess(GET_STATUS);
        break;
      case NOT_READY:
        processNotReady();
        break;
      case MOVE_ACK:
        processMvAck();
        break;
      case MVS_ACK:
        processMvsAck();
        break;
      case MVE_ACK:
        processMveAck();
        break;
      }

      Log.i(TAG, "Get message and process it from Server: " + opcode);
    }
  }

  class CommandProcessor
  {
    LinkedList<Instruction> instrBuffer = new LinkedList<>();

    boolean      r;
    Control      ctrl;
    MainActivity ctx;

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
            if (!isSocketFree.get() || instrBuffer.isEmpty() || TelescopeStatus.locked()) continue;
            lock();
            TelescopeStatus.lock();
            new IOProcessor(instrBuffer.removeFirst(), new ControlInterface()
            {
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
                ctx.runOnUiThread(new Runnable()
                {
                  @Override public void run()
                  {
                    ctx.monitor.update(str);
                  }
                });
              }
            }).execute();
          }
        }
      }.start();
    }

    void add(Instruction i)
    {
      if (TelescopeStatus.locked()) return;
      instrBuffer.addLast(i);
    }
  }

  private void processReady()
  {
    Log.i(TAG, "processReady in Control = " + TelescopeStatus.get());
    TelescopeStatus.set(ST_READY);
    TelescopeStatus.setMode(ST_READY);
    TelescopeStatus.unlock();
  }

  private void processNotReady()
  {
    TelescopeStatus.set(ST_NOT_READY);
  }

  private void processMvAck()
  {
    TelescopeStatus.set(ST_NOT_READY); // TODO.
  }

  private void processMvsAck()
  {
    TelescopeStatus.unlock();
    TelescopeStatus.setAck(MVS_ACK);
  }

  private void processMveAck()
  {
    TelescopeStatus.unlock();
    TelescopeStatus.setAck(MVE_ACK);
  }

  private void processBattery(int val)
  {
    TelescopeStatus.setBatteryVoltage(val);
  }

  private void lock()
  {
    isSocketFree.set(false);
  }

  private void release()
  {
    Log.i(TAG, "Release--------------------------------------------------------------------------");
    isSocketFree.set(true);
  }
}



