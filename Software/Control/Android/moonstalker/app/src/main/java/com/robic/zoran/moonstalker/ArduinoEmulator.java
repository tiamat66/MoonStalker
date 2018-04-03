package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;

import static com.robic.zoran.moonstalker.Control.CMD_BATTERY;
import static com.robic.zoran.moonstalker.Control.CMD_MOVE;
import static com.robic.zoran.moonstalker.Control.CMD_STATUS;
import static com.robic.zoran.moonstalker.Control.GET_BATTERY;
import static com.robic.zoran.moonstalker.Control.GET_STATUS;
import static com.robic.zoran.moonstalker.Control.MOVE;
import static com.robic.zoran.moonstalker.Control.MSG_BATTERY;
import static com.robic.zoran.moonstalker.Control.MSG_READY;

class ArduinoEmulator
{
  private String  state;
  private EmulatorHandler eh;
  private ArrayList<String> q;

  ArduinoEmulator()
  {
    eh = new EmulatorHandler();
    q = new ArrayList<>();
    state = "";
  }

  String read()
  {
    if (q.size() == 0) return "";
    String s = q.get(0);
    q.remove(0);
    return s;
  }

  void write(String msg)
  {
    int i = chkMsg(msg);
    eh.obtainMessage(i).sendToTarget();
  }

  static void to(long t)
  {
    try {
      Thread.sleep(t);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private int chkMsg(String recMsg)
  {
    if (recMsg.isEmpty()) return -1;

    String bfrs[] = recMsg.split(",");
    if (bfrs[0].contains(CMD_STATUS))  return GET_STATUS;
    if (bfrs[0].contains(CMD_BATTERY)) return GET_BATTERY;
    if (bfrs[0].contains(CMD_MOVE))    return MOVE;
    return -1;
  }

  @SuppressLint("HandlerLeak")
  private class EmulatorHandler extends Handler
  {
    @Override
    public void handleMessage(Message message)
    {
      switch (message.what)
      {
        case GET_STATUS:
          to(2000);
          state = MSG_READY;
          q.add(state);
          break;
        case GET_BATTERY:
          to(1500);
          state = MSG_BATTERY + ",12";
          q.add(state);
          break;
        case MOVE:
          to(500);
          state = MSG_READY;
          q.add(state);
          break;
      }
      Log.i("IZAA", "Emulator send=" + state);
    }
  }
}
