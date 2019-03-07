package si.vajnartech.moonstalker;

import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import si.vajnartech.moonstalker.rest.Instruction;

import static si.vajnartech.moonstalker.C.TAG;
import static si.vajnartech.moonstalker.OpCodes.BATTERY;
import static si.vajnartech.moonstalker.OpCodes.READY;

public class IOProcessor extends AsyncTask<String, Void, String>
{
  private Instruction     instruction;
  private BluetoothSocket socket;
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
        OutputStream   outStream = socket.getOutputStream();
        write(instruction.toString(), outStream);

        byte[] buffer = new byte[256];  // buffer store for the stream
        int    bytes;
        InputStream    inStream = socket.getInputStream();
        String result;

        try {
          // Read from the InputStream
          bytes = inStream.read(buffer);        // Get number of bytes and message in "buffer"
          Log.i(TAG, "...Read " + bytes + " bytes...");
//          handler.obtainMessage(RECIEVE_MESSAGE, bytes, -1,
//                                buffer).sendToTarget();     // Send to message queue Handler
          result = new String(buffer, 0, bytes);
        } catch (IOException e) {
          result = null;
          // Send connection canceled
        }


//        String         result;
//        InputStream    inStream = socket.getInputStream();
//        BufferedReader br       = new BufferedReader(new InputStreamReader(inStream), 512);
//        result = br.readLine();
//        br.close();
        Log.i("IZAA", "Received=" + result);
        return result;
      } catch (IOException e) {
        e.printStackTrace();
        // connection canceled
        Log.i(TAG, "IOException in Bluetooth IO processing");
      }
      Log.i("IZAA", "ADJENJAVVVVVVVV...............");
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
    Log.i(TAG, "release socket");
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
    Instruction j = parse(instruction);
    if (j == null)
      return;

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

  private static Instruction parse(String ins)
  {
    // no param instruction
    ins = "<ST>";
    Log.i("IZAA", ins.substring(ins.indexOf("<"), ins.indexOf(">")));
    return null;
  }
}
