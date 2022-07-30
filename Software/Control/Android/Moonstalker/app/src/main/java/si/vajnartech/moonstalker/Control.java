package si.vajnartech.moonstalker;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static si.vajnartech.moonstalker.C.K;
import static si.vajnartech.moonstalker.C.ST_INIT;
import static si.vajnartech.moonstalker.C.ST_NOT_READY;
import static si.vajnartech.moonstalker.C.ST_READY;
import static si.vajnartech.moonstalker.C.ST_WAITING_ACK;
import static si.vajnartech.moonstalker.C.TAG;
import static si.vajnartech.moonstalker.OpCodes.BATTERY;
import static si.vajnartech.moonstalker.OpCodes.ERROR;
import static si.vajnartech.moonstalker.OpCodes.GET_STATUS;
import static si.vajnartech.moonstalker.OpCodes.INIT;
import static si.vajnartech.moonstalker.OpCodes.IN_MSG;
import static si.vajnartech.moonstalker.OpCodes.MOVE;
import static si.vajnartech.moonstalker.OpCodes.MOVE_ACK;
import static si.vajnartech.moonstalker.OpCodes.MOVE_START;
import static si.vajnartech.moonstalker.OpCodes.MOVE_STOP;
import static si.vajnartech.moonstalker.OpCodes.MVE_ACK;
import static si.vajnartech.moonstalker.OpCodes.MVS_ACK;
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
  private final AtomicBoolean isSocketFree;

  private final InMessageHandler inMessageHandler;
  private final CommandProcessor processor;


  Control(MainActivity act)
  {
    super(act);
    inMessageHandler = new InMessageHandler();
    isSocketFree = new AtomicBoolean(true);
    processor = new CommandProcessor(this, act);
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
  void move()
  {
    double dif_az;
    double dif_hi;
    double hSteps = 0;
    double vSteps = 0;
    int    cur_h_steps;
    int    cur_v_steps;

    if (TelescopeStatus.locked()) {
      Log.i(TAG, "cannot execute move, telescope is locked");
      return;
    }

    dif_az = newObject.getAzimuth() - skyObject.getAzimuth();
    dif_hi = newObject.getHeight() - skyObject.getHeight();

    hSteps += (dif_az * K) / 360.0;
    vSteps += (dif_hi * K) / 360.0;

    cur_h_steps = (int) hSteps;
    cur_v_steps = (int) vSteps;
    if (Math.abs(cur_h_steps) >= PRECISION || Math.abs(cur_v_steps) >= PRECISION) {
      TelescopeStatus.set(ST_WAITING_ACK);
      outMessageProcess(MOVE, Integer.toString(cur_h_steps), Integer.toString(setMaxSpeedRPM()));
    }
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

  private void outMessageProcess(String opcode, String p1, String p2)
  {
    processor.add(new Instruction(opcode, p1, p2));
  }

  private void outMessageProcess(String opcode, String p1, String p2, String p3)
  {
    processor.add(new Instruction(opcode, p1, p2, p3));
  }

  @Override
  public void inMsgProcess(String opcode, Bundle params)
  {
    params.putString("opcode", opcode);
    inMessageHandler.obtainMessage(IN_MSG, params).sendToTarget();
  }

  @Override public void onLocationChanged(@NonNull Location location)
  {

  }

  private static class InMessageHandler extends Handler
  {
    InMessageHandler()
    {
      super();
    }

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
        processInit();
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

    AtomicBoolean running = new AtomicBoolean(false);
    Control       ctrl;
    MainActivity  ctx;

    CommandProcessor(Control ctrl, MainActivity act)
    {
      this.ctrl = ctrl;
      ctx = act;
      running.set(true);
      processorStart();
    }

    void processorStart()
    {
      ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
      executor.scheduleAtFixedRate(() -> {
        while (running.get()) {
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
              ctx.runOnUiThread(() -> ctx.monitor.update(str));
            }
          }).executeOnExecutor(THREAD_POOL_EXECUTOR);
        }
      }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    void add(Instruction i)
    {
      if (TelescopeStatus.locked()) return;
      instrBuffer.addLast(i);
    }
  }

  private static void processReady()
  {
    Log.i(TAG, "processReady in Control = " + TelescopeStatus.get());
    TelescopeStatus.set(ST_READY);
    TelescopeStatus.setMode(ST_READY);
    TelescopeStatus.unlock();
  }

  private static void processInit()
  {
    TelescopeStatus.set(ST_INIT);
  }

  private static void processNotReady()
  {
    TelescopeStatus.set(ST_NOT_READY);
  }

  private static void processMvAck()
  {
    TelescopeStatus.unlock();
    TelescopeStatus.setAck(OpCodes.MOVE_ACK);
  }

  private static void processMvsAck()
  {
    TelescopeStatus.unlock();
    TelescopeStatus.setAck(MVS_ACK);
  }

  private static void processMveAck()
  {
    TelescopeStatus.unlock();
    TelescopeStatus.setAck(MVE_ACK);
  }

  private static void processBattery(int val)
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



