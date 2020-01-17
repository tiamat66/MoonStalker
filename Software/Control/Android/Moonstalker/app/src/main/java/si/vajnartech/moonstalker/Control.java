package si.vajnartech.moonstalker;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.LinkedList;

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

interface OutCommandInterface
{
  void outMessageProcess(String opcode);
  void outMessageProcess(String opcode, String p1);
  void outMessageProcess(String opcode, String p1, String p2);
  void outMessageProcess(String opcode, String p1, String p2, String p3);
}

public class Control extends Telescope implements OutCommandInterface
{
  private boolean          isSocketFree;
  private InMessageHandler inMessageHandler;
  private CommandProcessor processor;
  private MainActivity     act;
  private int              fakeA = 0;
  private int              fakeH = 0;


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
  void mv(int hSteps, int vSteps, int speed)
  {
    TelescopeStatus.set(ST_MOVING);
    outMessageProcess(MOVE, Integer.toString(hSteps), Integer.toString(vSteps));
  }

  @Override
  public void outMessageProcess(String opcode)
  {
    processor.add(new Instruction(opcode));
  }

  @Override
  public void outMessageProcess(String opcode, String p1)
  {
    processor.add(new Instruction(opcode, p1));
  }

  @Override
  public void outMessageProcess(String opcode, String p1, String p2)
  {
    processor.add(new Instruction(opcode, p1, p2));
  }

  @Override
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



