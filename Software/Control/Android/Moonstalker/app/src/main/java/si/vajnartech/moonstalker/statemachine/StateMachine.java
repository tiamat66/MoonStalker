package si.vajnartech.moonstalker.statemachine;

import android.util.Log;

import java.util.Arrays;

import si.vajnartech.moonstalker.C;
import si.vajnartech.moonstalker.MainActivity;
import si.vajnartech.moonstalker.OpCodes;
import si.vajnartech.moonstalker.R;
import si.vajnartech.moonstalker.TelescopeStatus;
import si.vajnartech.moonstalker.UI;


public abstract class StateMachine implements StateMachineActions
{
  protected static final State state = new State();

  protected MainActivity act;
  protected UI           userInterface;


  Integer[] edgeStates = {C.ST_READY, C.ST_NOT_CONNECTED};

  protected StateMachine(MainActivity act)
  {
    this.act = act;
    userInterface = new UI(act);

    state.reset();

    Thread runner = new Runner();
    runner.start();
    updateStatus();
  }

  protected static class State
  {
    private int status = -1;
    private int mode   = -1;
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
      status = C.ST_NOT_CONNECTED;
      mode = C.ST_READY;
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
          process();
          if (state.statusChanged()) {
            updateStatus();
            state.changeStatus();
          }
        }

        if (state.modeChanged()) {
          processMode();
          state.changeMode();
        }

        threadSleep();
        monitorState();
      }
    }

    private void addBalls()
    {
      Ball.addBall(C.ST_INIT, new Ball(() -> {
        stopProgress();
        message(act.tx(R.string.msg_no_answer));
        disconnectBluetooth();
        state.reset();
      }, 7));
      Ball.addBall(C.ST_CONNECTED, new Ball(() -> {
        stopProgress();
        message(act.tx(R.string.msg_no_answer));
        disconnectBluetooth();
        state.reset();
      }, 7));
      Ball.addBall(C.ST_NOT_READY, new Ball(() -> {
        TelescopeStatus.unlock();
        st();
      }, 5));

      Ball.addBall(C.ST_WAITING_ACK, new Ball(() -> {
        if (TelescopeStatus.getAck().isEmpty()) return;
        if (TelescopeStatus.getAck().equals(OpCodes.MVS_ACK) ||
            TelescopeStatus.getAck().equals(OpCodes.MVE_ACK)) {
          if (OpCodes.MVE_ACK.equals(TelescopeStatus.getAck())) {
            TelescopeStatus.set(C.ST_READY);
            TelescopeStatus.setMisc(C.NONE);
          } else
            TelescopeStatus.set(C.ST_MOVING);
          TelescopeStatus.setAck(C.CLEAR);
        } else {
          message(act.tx(R.string.msg_no_answer));
          TelescopeStatus.setAck(C.CLEAR);
          state.restore();
        }
      }, 3));
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
