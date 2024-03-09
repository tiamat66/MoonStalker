package si.vajnartech.moonstalker;

import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static si.vajnartech.moonstalker.C.TAG;
import static si.vajnartech.moonstalker.OpCodes.*;

import si.vajnartech.moonstalker.rest.AsyncTaskExecutor;

public class IOProcessor extends AsyncTaskExecutor<String, Void, String>
{
  private final Instruction      instruction;
  private final BluetoothSocket  socket;
  private final ControlInterface ctrlInterface;

  IOProcessor(Instruction opCode, ControlInterface i)
  {
    super();
    ctrlInterface = i;
    this.socket = BlueTooth.socket;
    instruction = opCode;
  }

  @Override
  protected String doInBackground(String params)
  {
    if (instruction == null || socket == null)
      return null;

    try {
      OutputStream outStream = socket.getOutputStream();
      write(instruction.toString(), outStream);

      byte[]      buffer   = new byte[256];
      int         bytes;
      InputStream inStream = socket.getInputStream();
      String      result;
      try {
        bytes = inStream.read(buffer);
        result = new String(buffer, 0, bytes);
      } catch (IOException e) {
        result = null;
        // Send connection canceled
        Log.i(TAG, "IOException in Bluetooth IO processing");
      }
      Log.i(TAG, "Received msg from arduino=" + result);
      return result;
    } catch (IOException e) {
      // Send connection canceled
      Log.i(TAG, "IOException in Bluetooth IO processing");
    }

    return null;
  }

  @Override
  protected void onPostExecute(String j)
  {
    if (j != null) {
      Log.i(TAG, "on post execute OK");
      ctrlInterface.dump("$ " + j + "\n");
      process(j);
    } else {
      Log.i(TAG, "on post execute ERROR");
    }
    ctrlInterface.releaseSocket();
  }

  private void write(String message, OutputStream os)
  {
    Log.i(TAG, "Data to send: " + message);
    byte[] msgBuffer = message.getBytes();
    try {
      os.write(msgBuffer);
      ctrlInterface.dump("$ msg send: " + message + "\n");
    } catch (IOException e) {
      Log.i(TAG, "Error data send: " + e.getMessage());
    }
  }

  private Instruction _parseInMsg(String input)
  {
    if (input.contains("<" + READY + ">"))
      return new Instruction(READY);
    else if (input.contains("<" + NOT_READY))
      return new Instruction(NOT_READY);
    else if (input.contains("<" + MOVE_ACK))
      return new Instruction(MOVE_ACK);
    else if (input.contains("<" + BATTERY))
      return new Instruction(BATTERY, input.substring(input.indexOf(BATTERY), input.length()-1).split(" ")[1]);
    else if (input.contains("<" + MVS_ACK))
      return new Instruction(MVS_ACK);
    else if (input.contains("<" + MVE_ACK))
      return new Instruction(MVE_ACK);
    return new Instruction("");
  }

  private void process(String instruction)
  {
    Instruction j = _parseInMsg(instruction);

    if (j.opCode.equals(BATTERY))
    {
      Log.i(TAG, "processing BTRY from response with p1 = " + j.parameters.get(0));
      Bundle b = new Bundle();
      int val = Integer.parseInt(j.parameters.get(0));
      b.putInt("p1", val);
      ctrlInterface.messageProcess(j.opCode, b);
    } else
      ctrlInterface.messageProcess(j.opCode, new Bundle());

    ctrlInterface.dump(String.format("$ msg rcvd: %s\n", j.opCode));
  }
}
