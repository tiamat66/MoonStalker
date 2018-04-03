package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

class BlueToothService
{
  private static final String TAG = "IZAA";
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  static final int NOT_CONNECTED = 1;
  static final int CONNECTING    = 2;
  static final int CONNECTED     = 3;

  // Bluetooth connection status
  private static final int CONNECTION_ACCEPTED_MESSAGE  = 1;
  static final int         CONNECTION_CANCELED_MESSAGE  = 2;
  private static final int CONNECTION_TIMED_OUT_MESSAGE = 3;

  private static final int TIMEOUT_CONNECTION = 20;

  private BluetoothAdapter btAdapter = null;
  private MainActivity act;
  private BluetoothDevice pairedDevice;
  private int waiting = 0;
  private BTMessageHandler btMessageHandler;
  private BluetoothSocket BTsocket = null;

  private int status = NOT_CONNECTED;

  BlueToothService(MainActivity act)
  {
    this.act = act;

    btAdapter = BluetoothAdapter.getDefaultAdapter();
    btMessageHandler = new BTMessageHandler();
    checkBTState();
  }

  BluetoothSocket getBTsocket()
  {
    return BTsocket;
  }

  BTMessageHandler getBtMessageHandler()
  {
    return btMessageHandler;
  }

  private void checkBTState()
  {
    if (btAdapter == null) act.errorExit("Fatal Error",
      "Bluetooth not support");
    else {
      if (btAdapter.isEnabled()) Log.i(TAG, "Bluetooth ON");
      else {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        act.startActivityForResult(enableBtIntent, 1);
      }
    }
  }

  private void getPairedDevices()
  {
    Log.i(TAG, "Enter getPairedDevices");
    Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
    if (pairedDevices.size() > 0) for (BluetoothDevice device : pairedDevices) {
      Log.i(TAG, device.getName() + "\n" + device.getAddress());
      pairedDevice = device;
    }
  }

  void connect()
  {
    if (DeviceIO.EMULATED) {
      ArduinoEmulator.to(2500);
      btMessageHandler.obtainMessage(CONNECTION_ACCEPTED_MESSAGE).sendToTarget();
      return;
    }

    getPairedDevices();
    if (pairedDevice == null) Log.i(TAG, "No paired device");
    else {
      setStatus(CONNECTING);
      act.updateStatus();
      ConnectThread connectThread = new ConnectThread(pairedDevice);
      connectThread.start();
      new Scheduler();
    }
  }

  private class Scheduler extends Thread
  {
    Scheduler()
    {
      Log.i(TAG,"Starting scheduler");
      this.start();
    }

    @Override
    public void run()
    {
      while (getStatus() == CONNECTING) {
          ArduinoEmulator.to(500);
        waiting++;
        if (waiting == TIMEOUT_CONNECTION)
          btMessageHandler.obtainMessage(CONNECTION_TIMED_OUT_MESSAGE).sendToTarget();
      }
      waiting = 0;
    }
  }

  @SuppressLint("HandlerLeak")
  class BTMessageHandler extends Handler
  {
    @Override
    public void handleMessage(Message message)
    {
      switch (message.what) {
        case CONNECTION_ACCEPTED_MESSAGE:
          Log.i(TAG, "BT connection accepted");
          setStatus(CONNECTED);
          try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}
          act.getCtr().inMsgProcess(Control.INIT, null);
          break;
        case CONNECTION_CANCELED_MESSAGE:
          setStatus(NOT_CONNECTED);
          act.connectionCanceled();
          break;
        case CONNECTION_TIMED_OUT_MESSAGE:
          setStatus(NOT_CONNECTED);
          act.connectionTimedOutMessage();
          break;
      }
    }
  }

  private class ConnectThread extends Thread
  {
    private final BluetoothSocket mmSocket;

    ConnectThread(BluetoothDevice device)
    {
      BluetoothSocket tmp = null;
      try {
        tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
      } catch (IOException ignored) {
      }
      mmSocket = tmp;
    }

    public void run()
    {
      btAdapter.cancelDiscovery();
      try {
        mmSocket.connect();
      } catch (IOException connectException) {
        try {
          mmSocket.close();
        } catch (IOException ignored) {
        }
        return;
      }
      BTsocket = mmSocket;
      btMessageHandler.obtainMessage(CONNECTION_ACCEPTED_MESSAGE).sendToTarget();
    }
  }

  void setStatus(int val)
  {
    status = val;
    act.updateStatus();
  }

  int getStatus()
  {
    return status;
  }
}
