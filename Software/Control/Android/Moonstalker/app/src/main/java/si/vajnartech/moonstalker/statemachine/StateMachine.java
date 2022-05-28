package si.vajnartech.moonstalker.statemachine;

import android.util.Log;

import java.util.Arrays;

import si.vajnartech.moonstalker.C;
import si.vajnartech.moonstalker.MainActivity;
import si.vajnartech.moonstalker.OpCodes;
import si.vajnartech.moonstalker.ProgressIndicator;
import si.vajnartech.moonstalker.TelescopeStatus;
import si.vajnartech.moonstalker.UI;


public abstract class StateMachine implements StateMachineActions
{
  private static final State state = new State();

  protected MainActivity act;
  protected UI           userInterface;


  Integer[] edgeStates = {C.ST_READY, C.ST_NOT_CONNECTED};

  protected StateMachine(MainActivity act)
  {
    this.act = act;
    userInterface = new UI(act);

    state.reset();
    Log.i("pepe", "zagata");

    Thread runner = new Runner();
    runner.start();
    updateStatus();
  }

  private static class State
  {
    int status          = -1;
    int mode            = -1;
    int telescopeStatus = -1;
    int telescopeMode   = -1;

    State storedState = null;

    void changeState()
    {
      status = TelescopeStatus.get();
      mode = TelescopeStatus.getMode();
    }

    boolean statusChanged()
    {
      // ce od teleskopa status ni enak strojevemu statusu
      return TelescopeStatus.get() != status;
    }

    boolean modeChanged()
    {
      return TelescopeStatus.getMode() != mode;
    }

    void reset()
    {
      status = C.ST_NOT_CONNECTED;
      mode = C.ST_READY;
      TelescopeStatus.set(C.ST_NOT_CONNECTED);
      TelescopeStatus.setMode(C.ST_READY);
    }

    void restore()
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
          if (state.statusChanged() || state.modeChanged())
            updateStatus();
          state.changeState();
        }

        Log.i("pepe", "current status=" + state.status);
        Log.i("pepe", "teles status=" + TelescopeStatus.get());
        threadSleep();
        monitorState();
      }
    }

    private void addBalls()
    {
      // tale moz se lahko zasteka ce se ni v CONNECTED->INIT po 7h sekundah
      Ball.addBall(C.ST_CONNECTED, new Ball(() -> {
        stopProgress();
        onNoAnswer();
//        disconnectBluetooth();
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
          onNoAnswer();
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

  private boolean statusChanged(int from, int to)
  {
    return state.status == from && TelescopeStatus.get() == to;
  }

  // tukaj so dfinirane akcije ob prehodih stanj
  private void process()
  {
    if (statusChanged(C.ST_NOT_READY, C.ST_READY)) {
      TelescopeStatus.unlock();
      state.restore();
    } else if (statusChanged(C.ST_READY, C.ST_NOT_READY)) {
      TelescopeStatus.lock();
    } else if (statusChanged(C.ST_NOT_CONNECTED, C.ST_CONNECTING)) {
      connect();
    } else if (statusChanged(C.ST_CONNECTING, C.ST_CONNECTED)) {
      initTelescope();
    } else if (statusChanged(C.ST_CONNECTED, C.ST_INIT)) {
      startProgress(ProgressIndicator.ProgressType.INITIALIZING);
      st();
    } else if (statusChanged(C.ST_CONNECTED, C.ST_READY)) {
      stopProgress();
    }
  }
}
