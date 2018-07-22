package com.robic.zoran.moonstalker;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.robic.zoran.moonstalker.Control.BATTERY;
import static com.robic.zoran.moonstalker.Control.CMD_BATTERY;
import static com.robic.zoran.moonstalker.Control.ERROR;
import static com.robic.zoran.moonstalker.Control.MSG_BATTERY;
import static com.robic.zoran.moonstalker.Control.MSG_ERROR;
import static com.robic.zoran.moonstalker.Control.MSG_READY;
import static com.robic.zoran.moonstalker.Control.READY;
import static com.robic.zoran.moonstalker.Telescope.TAG;

class DeviceIO extends Thread
{
  static final boolean EMULATED = true;
  private ArduinoEmulator     e = null;

  private InputStream inStream;
  private OutputStream outStream;
  private MainActivity act;
  private boolean  listening = false;

  DeviceIO(MainActivity act)
  {
    this.act = act;
    if (EMULATED)
      this.e = new ArduinoEmulator();
    this.listening = true;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  @Override
  public void run()
  {
    Control c = act.getCtr();
    while (listening) {
      ArduinoEmulator.to(1000);
      String m = e.read();
      if (m.isEmpty()) continue;
      Bundle b = chkMsg(m);
      if (b == null) continue;
      c.inMsgProcess(b.getInt("msg"), b);
    }
  }

  public void run_original()
  {
    byte[] buffer = new byte[256];
    int bytes;
    while (act.getBt().getBTsocket() == null) ArduinoEmulator.to(500);
    Log.i(Telescope.TAG, "BT socket OK.");
    BluetoothSocket socket = act.getBt().getBTsocket();
    try {
      inStream = socket.getInputStream();
      outStream = socket.getOutputStream();
    } catch (IOException ignored) {
    }
    while (listening) {
      try {
        bytes = inStream.read(buffer);
        String rcvdMsg = new String(buffer, 0, bytes);
        Bundle b = chkMsg(rcvdMsg);
        if (b == null) continue;
        act.getCtr().inMsgProcess(b.getInt("msg"), b);
      } catch (IOException e) {
        act.getBt().getBtMessageHandler().
            obtainMessage(BlueToothService.CONNECTION_CANCELED_MESSAGE).sendToTarget();
        Log.i(Telescope.TAG, "Error data receive: " + e.getMessage());
        break;
      }
    }
  }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  //emulator write
  void write(String message, boolean t)
  {
    if (!t) {
      Log.i(Telescope.TAG, "This method is for Emulator");
      return;
    }
    Log.i(TAG, "Data to send: " + message);
    e.write(message);
  }

  void write(String message)
  {
    Log.i(TAG, "Data to send: " + message);
    byte[] msgBuffer = message.getBytes();
    try {
      outStream.write(msgBuffer);
      Log.i(TAG, "Message sent to Telescope Control: " + message);
    } catch (IOException e) {
      act.getBt().getBtMessageHandler().obtainMessage(BlueToothService.CONNECTION_CANCELED_MESSAGE).sendToTarget();
      Log.i(TAG, "Error data send: " + e.getMessage());
    }
  }

  private Bundle chkMsg(String recMsg)
  {
    if (recMsg.isEmpty()) return null;
    Bundle params = new Bundle();
    String bfrs[] = recMsg.split(",");

    if (recMsg.contains(MSG_READY)) {
      params.putInt("msg", READY);
    }
    else if (recMsg.contains(MSG_BATTERY)) {
      params.putInt("msg", BATTERY);
      params.putFloat("arg1", Float.valueOf(bfrs[1]));
    }
    else if (recMsg.contains(MSG_ERROR)) {
      params.putInt("msg", ERROR);
      params.putFloat("arg1", Float.valueOf(bfrs[1]));
    }
    return params;
  }
}
