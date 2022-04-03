package com.robic.zoran.arduinoemulator;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

class BlueToothService
{
  private static final String TAG = "IZAA";

  // SDP UUID service
  private static final UUID   MY_UUID = UUID.fromString(
      "00001101-0000-1000-8000-00805F9B34FB");
  private static final String NAME    = "MOONSTALKER";

  // Status  for Handler
  private static final int RECIEVE_MESSAGE             = 1;
  private static final int CONNECTION_CANCELED_MESSAGE = 2;

  private boolean         connectionCanceled = true;
  private BluetoothSocket socket             = null;
  private BtReadWrite     btReadWrite;
  private AcceptThread    acceptThread;

  private final BluetoothAdapter        btAdapter;
  private final MainActivity            act;
  private final BlueToothServiceHandler handler;

  @SuppressLint("HandlerLeak") BlueToothService(MainActivity act)
  {
    this.act = act;
    btAdapter = BluetoothAdapter.getDefaultAdapter();
    handler = new BlueToothServiceHandler();
    checkBTState();
  }

  private void checkBTState()
  {
    // Check for Bluetooth support and then check to make sure it is turned on
    // Emulator doesn't support Bluetooth and will return null
    if (btAdapter == null) act.exit("Fatal Error", "Bluetooth not support");
    else {
      if (btAdapter.isEnabled()) {
        Log.i(TAG, "...Bluetooth ON...");
      } else {
        //Prompt user to turn on Bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        act.startActivityForResult(enableBtIntent, 1);
      }
    }
  }

  void getPairedDevices()
  {
    Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
    if (pairedDevices.size() > 0) {
      for (BluetoothDevice device : pairedDevices) {
        Log.i(TAG, device.getName() + "\n" + device.getAddress());
      }
    }
  }

  @SuppressLint("HandlerLeak")
  class BlueToothServiceHandler extends Handler
  {
    @Override
    public void handleMessage(Message msg)
    {
      String str;
      switch (msg.what) {
      case RECIEVE_MESSAGE:
        byte[] readBuf = (byte[]) msg.obj;
        String rcvdMsg = new String(readBuf, 0, msg.arg1);
        str = "...Message received from Client...\n" +
              rcvdMsg;
        Log.i(TAG, str);
        act.print(str);
        processMsg(rcvdMsg);
        break;
      case CONNECTION_CANCELED_MESSAGE:
        connectionCanceled = true;
        str = "...Connection Canceled...";
        act.stopBTServer();
        Log.i(TAG, str);
        act.print(str);
        socket = null;
        act.BTServerStarted = false;
        act.startStop();
        break;
      }
    }
  }

  // This acts as Server
  private class AcceptThread extends Thread
  {
    BluetoothServerSocket serverSocket = null;
    boolean               isRunning    = false;

    AcceptThread()
    {
      try {
        serverSocket = btAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
      } catch (IOException ignored) {
      }
    }

    @Override
    public void run()
    {
      // Keep listening until exception occurs or a socket is returned
      isRunning = true;
      while (isRunning) {
        try {
          if (!connectionCanceled) continue;
          if (serverSocket != null) socket = serverSocket.accept();
          else Log.i(TAG, "Error Server Socket");
        } catch (IOException e) {
          break;
        }
        // If a connection was accepted
        if (socket != null) {
          btReadWrite = new BtReadWrite(socket);
          btReadWrite.start();
          Log.i(TAG, "...Connection was accepted...");
          connectionCanceled = false;
          try {
            serverSocket.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }

    void cancel()
    {
      try {
        serverSocket.close();
      } catch (IOException ignored) {
      }
      isRunning = false;
    }
  }

  private class BtReadWrite extends Thread
  {
    final InputStream  mmInStream;
    final OutputStream mmOutStream;

    BtReadWrite(BluetoothSocket socket)
    {
      InputStream  tmpIn  = null;
      OutputStream tmpOut = null;

      // Get the input and output streams, using temp objects because
      // member streams are final
      try {
        tmpIn = socket.getInputStream();
        tmpOut = socket.getOutputStream();
      } catch (IOException ignored) {
      }

      mmInStream = tmpIn;
      if (mmInStream != null) {
        Log.i(TAG, "...In Stream OK...");
      }
      mmOutStream = tmpOut;
    }

    @Override
    public void run()
    {
      byte[] buffer = new byte[256];  // buffer store for the stream
      int    bytes;

      while (true) {
        if (connectionCanceled) break;
        try {
          // Read from the InputStream
          bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
          Log.i(TAG, "...Readed " + bytes + " bytes...");
          handler.obtainMessage(RECIEVE_MESSAGE, bytes, -1,
                                buffer).sendToTarget();     // Send to message queue Handler
        } catch (IOException e) {
          handler.obtainMessage(CONNECTION_CANCELED_MESSAGE).sendToTarget();
          break;
        }
      }
    }

    /* Call this from the main activity to send data to the remote device */
    void write(String message)
    {
      String str;

      str = "...Data to send: " + message + "...";
      Log.i(TAG, str);
      act.print(str);
      byte[] msgBuffer = message.getBytes();
      try {
        mmOutStream.write(msgBuffer);
      } catch (IOException e) {
        handler.obtainMessage(CONNECTION_CANCELED_MESSAGE).sendToTarget();
        str = "...Error data send: " + e.getMessage() + "...";
        Log.i(TAG, str);
        act.print(str);
      }
    }
  }

  void write(String msg)
  {
    if (btReadWrite != null) {
      btReadWrite.write(msg);
      String str = "...Message sent to Client: " + msg + "...";
      Log.i(TAG, str);
      act.print(str);
    }
  }

  void startBtServer() throws InterruptedException
  {
    acceptThread = new AcceptThread();
    acceptThread.start();
  }

  void stopBtServer()
  {
    acceptThread.cancel();
    String str = "...Bluetooth Server Stopped...";
    Log.i(TAG, str);
    act.print(str);
  }

  @SuppressWarnings({"unchecked", "ConstantConditions"})
  abstract static class ProcessMsg<T>
  {
    T result;

    ProcessMsg(String msg)
    {
      Class<T> resultClass = (Class<T>)
          ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
      if (resultClass != Void.class) {
        InputStream    stream = new ByteArrayInputStream(msg.getBytes(StandardCharsets.UTF_8));
        BufferedReader br     = new BufferedReader(new InputStreamReader(stream), 512);
        result = Json.fromJson(br, resultClass);
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  class Command extends ProcessMsg<Instruction>
  {
    Command(String msg)
    {
      super(msg);
      Log.i(TAG, "COMAND = " + msg);
      processMessage(msg);
    }

    private void processMessage(String msg)
    {
      if (msg.contains("MVS")) {
        if (msg.contains("MVST?")) {
          Log.i(TAG, "Process GET STATUS message from Client");
          st();
        } else {
          Log.i(TAG, "Processing MOVE START message from Client, " + msg);
          mvs();
        }
        return;
      }

      if (msg.contains("MVE")) {
        Log.i(TAG, "Processing MOVE END message from Client, ");
        mve();
      }

      switch (msg) {
      case "<ST?>":
        Log.i(TAG, "Process STATUS message from Client");
        rdy();
        break;
      case "<BTRY?>":
        Log.i(TAG, "Process BTRY message from Client");
        btryRes();
        break;
      default:
        Log.i(TAG, "processMessage in ProcessMsg: Unknown op code");
      }
    }
  }

  private void processMsg(String msg)
  {
    Log.i(TAG, "Process message = " + msg);
    new Command(msg);
  }

  private void rdy()
  {
    Log.i(TAG, "Sending RDY");
    new Handler().postDelayed(() -> write("<RDY>"), 500);
  }

  private void st()
  {
    final String cmd = "RDY";
    Log.i(TAG, "Sending " + cmd);
    new Handler().postDelayed(() -> write(String.format("<%s>", cmd)), 3000);
  }

  private void mve()
  {
    final String cmd = "MVE_ACK";
    Log.i(TAG, "Sending " + cmd);
    new Handler().postDelayed(() -> write(String.format("<%s>", cmd)), 1000);
  }

  private void mvs()
  {
    final String cmd = "MVS_ACK N 500";
    Log.i(TAG, "Sending " + cmd);
    new Handler().postDelayed(() -> write(String.format("<%s>", cmd)), 1000);
  }

  private void btryRes()
  {
    Log.i(TAG, "Sending BTRY");
    new Handler().postDelayed(() -> write("<BTRY 11300>"), 700);
  }
}

@SuppressWarnings({"WeakerAccess", "NullableProblems"})
class Instruction
{
  public int    opCode;
  public String p1;
  public String p2;

  @Override
  public String toString()
  {
    return "Instruction{" +
           "opCode=" + opCode +
           ", p1=" + p1 +
           ", p2=" + p2 +
           '}';
  }
}
