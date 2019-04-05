package si.vajnartech.moonstalker;

import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static si.vajnartech.moonstalker.C.TAG;
import static si.vajnartech.moonstalker.OpCodes.BATTERY;
import static si.vajnartech.moonstalker.OpCodes.READY;

public class IOProcessor extends AsyncTask<String, Void, String>
{
  private Instruction      instruction;
  private BluetoothSocket  socket;
  private ControlInterface ctrlInterface;

  IOProcessor(Instruction opCode, ControlInterface i)
  {
    super();
    ctrlInterface = i;
    this.socket = BlueTooth.socket;
    instruction = opCode;
  }

  @Override
  protected String doInBackground(String... params)
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
      Log.i(TAG, "Message sent to Telescope Control: " + message);
    } catch (IOException e) {
      Log.i(TAG, "Error data send: " + e.getMessage());
    }
  }

  private void process(String instruction)
  {
    Instruction j = new Instruction(instruction);

    switch (j.opCode) {
    case READY:
      Log.i(TAG, "processing RDY from response ");
      ctrlInterface.messageProcess(j.opCode, new Bundle());
      break;
    case BATTERY:
      Log.i(TAG, "processing BTRY from response with p1 = " + j.p1);
      Bundle b = new Bundle();
      b.putFloat("p1", Float.parseFloat(j.p1));
      ctrlInterface.messageProcess(j.opCode, b);
      break;
    default:
      Log.i(TAG, "unknown response received");
    }
  }
}
