package si.vajnartech.moonstalker.rest;

import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import si.vajnartech.moonstalker.Control;
import si.vajnartech.moonstalker.OpCodes;
import si.vajnartech.moonstalker.Telescope;

import static si.vajnartech.moonstalker.C.TAG;

public class IOProcessor extends AsyncTask<String, Void, String>
{
  private Instruction     instruction;
  private BluetoothSocket socket;
  private Control         ctrl;
  private Telescope telescope;

  public IOProcessor(Instruction opCode, BluetoothSocket socket, Telescope t)
  {
    super();
    telescope = t;
    this.socket = socket;
    instruction = opCode;
  }

  @Override
  protected String doInBackground(String... params)
  {
    if (instruction == null || socket == null)
      return null;

      try {
        OutputStream   outStream = socket.getOutputStream();
        write(instruction.toString(), outStream);

        String         result;
        InputStream    inStream  = socket.getInputStream();
        BufferedReader br        = new BufferedReader(new InputStreamReader(inStream), 512);
        result = br.readLine();
        br.close();
        return result;
      } catch (IOException e) {
        e.printStackTrace();
        Log.i(TAG, "IOException in Bluetooth IO processing");
      }
    return null;
  }

  @Override
  protected void onPostExecute(String j)
  {
    ctrl.release();
    if (j != null) {
      Log.i(TAG, "on post execute OK");
      process(j);
    } else {
      Log.i(TAG, "on post execute ERROR");
    }
    Log.i(TAG, "release socket");
  }

  private void write(String message, OutputStream os)
  {
    Log.i(TAG, "Data to send: " + message);
    byte[] msgBuffer = message.getBytes();
    try {
      os.write(msgBuffer);
      Log.i(TAG, "Message sent to Telescope Control: " + message);
    } catch (IOException e) {
      Log.i(TAG, "Error data send: " + e.getMessage());
    }
  }

  private void process(String instruction)
  {
    Instruction j = parse(instruction);
    if (j == null)
      return;

    switch (j.opCode) {
    case OpCodes.RDY:
      Log.i(TAG, "processing RDY from response ");
      telescope.inMsgProcess(j.opCode, null);
      break;
    case OpCodes.BTRY:
      Log.i(TAG, "processing BTRY from response with p1 = " + j.p1);
      Bundle b = new Bundle();
      b.putFloat("p1", Float.parseFloat(j.p1));
      ctrl.inMsgProcess(j.opCode, b);
      break;
    default:
      Log.i(TAG, "unknown response received");
    }
  }

  private static Instruction parse(String ins)
  {
    // no param instruction
    ins = "<ST>";
    Log.i("IZAA", ins.substring(ins.indexOf("<"), ins.indexOf(">")));
    return null;
  }
}