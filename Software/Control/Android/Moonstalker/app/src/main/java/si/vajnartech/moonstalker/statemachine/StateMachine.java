package si.vajnartech.moonstalker.statemachine;

import android.util.Log;

import java.util.Arrays;

import si.vajnartech.moonstalker.C;
import si.vajnartech.moonstalker.MainActivity;
import si.vajnartech.moonstalker.OpCodes;
import si.vajnartech.moonstalker.R;
import si.vajnartech.moonstalker.TelescopeStatus;


public abstract class StateMachine implements StateMachineActions
{
  protected static final State state = new State();

  protected MainActivity act;

  Integer[] edgeStates = {C.ST_READY, C.ST_NOT_CONNECTED};

  protected StateMachine(MainActivity act)
  {
    this.act = act;

    state.reset();

    Thread runner = new Runner();
    runner.start();
  }

  protected static class State
  {
    private int status = C.ST_NOT_CONNECTED;
    private int mode   = C.ST_READY;
    int telescopeStatus = -1;
    int telescopeMode   = -1;

    State storedState = null;

    void changeStatus()
    {
      status = TelescopeStatus.get();
    }

    void changeMode()
    {
      mode = TelescopeStatus.getMode();
    }

    boolean statusChanged()
    {
      return TelescopeStatus.get() != status;
    }

    public boolean statusChanged(int from, int to)
    {
      if (from == C.ANY && TelescopeStatus.get() == to)
        return true;
      return status == from && TelescopeStatus.get() == to;
    }

    boolean modeChanged()
    {
      return TelescopeStatus.getMode() != mode;
    }

    public boolean modeChanged(int from, int to)
    {
      // mode se je spremenil iz karkoli v to
      if (from == C.ANY && TelescopeStatus.getMode() == to)
        return true;
      return mode == from && TelescopeStatus.getMode() == to;
    }

    void reset()
    {
      TelescopeStatus.set(C.ST_NOT_CONNECTED);
      TelescopeStatus.setMode(C.ST_READY);
    }

    public void restore()
    {
      if (storedState != null) {
        status = storedState.status;
        mode = storedState.mode;
        TelescopeStatus.set(storedState.telescopeStatus);
        TelescopeStatus.setMode(storedState.telescopeMode);
        storedState = null;
      }
    }

    private void saveState()
    {
      if (storedState == null)
        storedState = new State();
      storedState.status = status;
      storedState.mode = mode;
      storedState.telescopeStatus = TelescopeStatus.getMode();
      storedState.telescopeMode = TelescopeStatus.get();
    }
  }

  private boolean isInEdgeState()
  {
    return Arrays.asList(edgeStates).contains(TelescopeStatus.get());
  }

  private class Runner extends Thread
  {
    volatile boolean running = true;

    final int timeout = 1000;

    @Override
    public void run()
    {
      addBalls();

      while (running) {
        // ce se status ni spremenil potem zazenemo bunke
        if (!state.statusChanged() && !isInEdgeState()) {
          Ball.executeBall(TelescopeStatus.get());
        } else {
          Ball.reset();
          if (!TelescopeStatus.locked())
            state.saveState();
        }

        if (state.statusChanged()) {
          process();
          updateUI(TelescopeStatus.get(), TelescopeStatus.getMode());
          state.changeStatus();
        }

        if (state.modeChanged()) {
          processMode();
          updateUI(TelescopeStatus.get(), TelescopeStatus.getMode());
          state.changeMode();
        }

        threadSleep();
        monitorState();
        Log.i("pepe mode", state.mode + " " + TelescopeStatus.getMode());
        Log.i("pepe status", state.status + " " + TelescopeStatus.get());
      }
    }

    private void addBalls()
    {
      Ball.addBall(C.ST_INIT, new Ball(() -> {
        message(act.tx(R.string.msg_no_answer));
        disconnectBluetooth();
        state.reset();
      }, 7));
      Ball.addBall(C.ST_CONNECTED, new Ball(() -> {
        message(act.tx(R.string.msg_no_answer));
        disconnectBluetooth();
        state.reset();
      }, 7));
      Ball.addBall(C.ST_WAITING_ACK, new Ball(this::processAck, 3));
    }

    private void processAck()
    {
      String[] acks = {OpCodes.MOVE_ACK, OpCodes.MVE_ACK, OpCodes.MVS_ACK};

      if (Arrays.asList(acks).contains(TelescopeStatus.getAck())) {
        if (OpCodes.MVE_ACK.equals(TelescopeStatus.getAck())) {
          TelescopeStatus.set(C.ST_READY);
          TelescopeStatus.setMisc(C.NONE);
        } else if (OpCodes.MVS_ACK.equals(TelescopeStatus.getAck())) {
          TelescopeStatus.set(C.ST_MOVING);
        }

        TelescopeStatus.setAck(C.CLEAR);
      } else {
        message(act.tx(R.string.msg_no_answer));
        TelescopeStatus.setAck(C.CLEAR);
        state.restore();
      }
    }

    private void threadSleep()
    {
      try {
        sleep(timeout);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    private void monitorState()
    {
      if (C.mStatus)
        dump("$ status=" + TelescopeStatus.get() + "\n");
    }
  }

  // akcije ob prehodih stanj
  protected abstract void process();
  
  protected abstract  void processMode();
}
