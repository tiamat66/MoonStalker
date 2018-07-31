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
import android.widget.Toast;

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

  // Delimiters
  private static final String SM = "<";
  private static final String EM = ">";

  //Messages
  //IN
  private static final String MOVE = "MV";
  private static final String ST   = "ST?";
  private static final String BTRY = "BTRY?";

  //OUT
  private static final String RDY      = "RDY";
  private static final String NOT_RDY  = "NOT_RDY";
  private static final String BTRY_RES = "BTRY";

  // SDP UUID service
  private static final UUID             MY_UUID                     = UUID.fromString(
      "00001101-0000-1000-8000-00805F9B34FB");
  private static final String           NAME                        = "MOONSTALKER";
  // Status  for Handler
  private static final int              RECIEVE_MESSAGE             = 1;
  private static final int              CONNECTION_CANCELED_MESSAGE = 2;
  private              BluetoothAdapter btAdapter                   = null;
  private MainActivity            act;
  private BtReadWrite             btReadWrite;
  private AcceptThread            acceptThread;
  private BlueToothServiceHandler handler;
  private String                  outMessage;
  private boolean         connectionCanceled = true;
  private BluetoothSocket socket             = null;

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
        Log.i("IZAA", device.getName() + "\n" + device.getAddress());
      }
    }
  }

  @SuppressLint("HandlerLeak") class BlueToothServiceHandler extends Handler
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
    private boolean isRunning = false;

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

      isRunning = true;
      while (isRunning) {
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
    String str = "...Bluetooth Server Started...";
    Log.i(TAG, str);
    Toast.makeText(act, str, Toast.LENGTH_LONG).show();
    act.print(str);
  }

  void stopBtServer()
  {
    acceptThread.cancel();
    String str = "...Bluetooth Server Stopped...";
    Log.i(TAG, str);
    act.print(str);
  }

  abstract class ProcessMsg<T>
  {
    private final Class<T> resultClass;
    T result;

    ProcessMsg(String msg)
    {
      //noinspection unchecked
      resultClass = (Class<T>)
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
      Log.i(TAG, "COMAND = " + result.opCode);
      processMessage();
    }

    private void processMessage()
    {
      switch (result.opCode) {
      case OpCodes.ST:
        Log.i(TAG, "Process STATUS message from Client");
        rdy();
        break;
      case OpCodes.BTRY:
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
    if (chkMsg(msg, MOVE)) {
      Log.i(TAG, "Process MV message from Client)");
      rdy();
      return;
    }
    if (chkMsg(msg, ST)) {
      Log.i(TAG, "Process STATUS message from Client)");
      rdy();
      return;
    }
    if (chkMsg(msg, BTRY)) {

      Log.i(TAG, "Process BTRY message from Client)");
      btryRes();
      return;
    }
    Log.i(TAG, "Unknown message received from Arduino");
    new Command(msg);
  }

  private boolean chkMsg(String recMsg, String expMsg)
  {
    recMsg = recMsg.substring(1, 1 + expMsg.length());
    return (recMsg.equals(expMsg));
  }

  private void rdy()
  {
    Log.i(TAG, "Sending RDY");
    new Handler().postDelayed(new Runnable()
    {
      @Override public void run()
      {
        write(Json.toJson(new Response(OpCodes.RDY)));
      }
    }, 1000);
  }

  private void notRdy()
  {
    // send <NOT_RDY>
    outMessage = SM +
                 NOT_RDY +
                 EM;
    Log.i(TAG, outMessage);
    write(outMessage);
  }

  private void btryRes()
  {
    Log.i(TAG, "Sending BTRY");
    new Handler().postDelayed(new Runnable()
    {
      @Override public void run()
      {
        write(Json.toJson(new Response(OpCodes.BTRY, "11.3")));
      }
    }, 1000);
  }

  static class OpCodes
  {
    final static int ST   = 1;
    final static int RDY  = 2;
    final static int BTRY = 3;
  }
}

@SuppressWarnings("WeakerAccess") class Instruction
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

@SuppressWarnings("WeakerAccess") class Response
{
  public int    opCode;
  public String p1;
  public String p2;

  Response(int opCode)
  {
    this.opCode = opCode;
  }

  Response(int opCode, String p1)
  {
    this.opCode = opCode;
    this.p1 = p1;
  }

}