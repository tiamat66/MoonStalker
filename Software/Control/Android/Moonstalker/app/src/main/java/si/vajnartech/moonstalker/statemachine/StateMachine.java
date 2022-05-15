package si.vajnartech.moonstalker.statemachine;

import android.os.Bundle;

import java.util.Arrays;

import si.vajnartech.moonstalker.C;
import si.vajnartech.moonstalker.OpCodes;
import si.vajnartech.moonstalker.TelescopeStatus;



public abstract class StateMachine implements StateMachineActions
{
  private static final State state = new State();

  Integer[] edgeStates = {C.ST_READY, C.ST_NOT_CONNECTED};

  protected StateMachine()
  {
    new Runner();
  }

  private static class State
  {
    int status = -1;
    int mode   = -1;
    int telescopeStatus = -1;
    int telescopeMode = -1;

    State storedState = null;

    boolean statusChanged()
    {
      // ce od teleskopa status ni enak strojevemu statusu
      return TelescopeStatus.get() != status;
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

  private boolean isInEdgeState() {
    return Arrays.asList(edgeStates).contains(TelescopeStatus.get());
  }

  private boolean isInLockedState

  private class Runner extends Thread
  {
    volatile boolean running = true;

    final int timeout = 1000;

    @Override
    public void run()
    {
      Ball b;
      addBalls();

      while (running) {
        // ce se status ni spremenil potem zazenemo bunke
        if (!state.statusChanged() && !isInEdgeState()) {
          Ball.executeBall(TelescopeStatus.get());
        }
        else {
          Ball.reset();
          if (!TelescopeStatus.locked())
            state.saveState();
          process();
          if (TelescopeStatus.get() != prevStatus || TelescopeStatus.getMode() != prevMode)
            updateStatus();
          prevStatus = TelescopeStatus.get();
          prevMode = TelescopeStatus.getMode();
        }

        threadSleep();
        monitorState();
      }
    }


    private void addBalls()
    {
      Ball.addBall(C.ST_CONNECTED, new Ball(() -> {
        stopProgress();
        onNoAnswer();
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
//      } else if (prevStatus == ST_NOT_CONNECTED && TelescopeStatus.get() == ST_CONNECTED) {
//         startProgress(MainActivity.ProgressType.INITIALIZING);
//         initTelescope(); ---> iz tega potem pride v READY zato je pa ball na ST_CONNECTED
    } else if (statusChanged(C.ST_CONNECTED, C.ST_READY)) {
      stopProgress();
    }
  }

  void reset()
  {
    for (Integer i : balls.keySet()) balls.get(i).stucked.set(0);
  }
}
