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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

class BlueToothService
{
    private static final String TAG = "Bluetooth";

    // Delimiters
    private static final String SM = "<";
    private static final String EM = ">";

    //Messages
    //IN
    private static final String MOVE = "MV";
    private static final String ST =   "ST?";
    private static final String BTRY = "BTRY?";

    //OUT
    private static final String RDY =      "RDY";
    private static final String NOT_RDY =  "NOT_RDY";
    private static final String BTRY_RES = "BTRY";

    // SDP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String NAME = "MOONSTALKER";
    // Status  for Handler
    private static final int RECIEVE_MESSAGE = 1;
    private static final int CONNECTION_CANCELED_MESSAGE = 2;
    private BluetoothAdapter btAdapter = null;
    private MainActivity act;
    private BtReadWrite btReadWrite;
    private AcceptThread acceptThread;
    private BlueToothServiceHandler handler;
    private String outMessage;
    private boolean connectionCanceled = true;
    private BluetoothSocket socket = null;

    @SuppressLint("HandlerLeak")
    BlueToothService(MainActivity act)
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
        else
        {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
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
                Log.d(TAG, device.getName() + "\n" + device.getAddress());
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
                    Log.d(TAG, str);
                    act.print(str);
                    processMsg(rcvdMsg);
                    break;
                case CONNECTION_CANCELED_MESSAGE:
                    connectionCanceled = true;
                    str = "...Connection Canceled...";
                    act.stopBTServer();
                    Log.d(TAG, str);
                    act.print(str);
                    socket = null;
                    break;
            }
        }
    }

    // This acts as Server
    private class AcceptThread extends Thread
    {
        BluetoothServerSocket mmServerSocket = null;
        boolean isRunning = false;

        AcceptThread()
        {
            try {
                mmServerSocket = btAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException ignored) { }
        }

        @Override
        public void run()
        {
            // Keep listening until exception occurs or a socket is returned
            isRunning = true;
            while (isRunning) {
                try {
                    if(!connectionCanceled) continue;
                    if (mmServerSocket != null) socket = mmServerSocket.accept();
                    else Log.e(TAG, "Error Server Socket");
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    btReadWrite = new BtReadWrite(socket);
                    btReadWrite.start();
                    Log.d(TAG, "...Connection was accepted...");
                    connectionCanceled = false;
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

       void cancel()
        {
            try {
                mmServerSocket.close();
            } catch (IOException ignored) { }
            isRunning = false;
        }
    }

    private class BtReadWrite extends Thread
    {
        final InputStream mmInStream;
        final OutputStream mmOutStream;
        private boolean isRunning = false;

        BtReadWrite(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException ignored) { }

            mmInStream = tmpIn;
            if(mmInStream != null) {
                Log.d(TAG, "...In Stream OK...");
            }
            mmOutStream = tmpOut;
        }

        @Override
        public void run()
        {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes;

            isRunning = true;
            while (isRunning) {
                if(connectionCanceled) break;
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                    Log.d(TAG, "...Readed" + bytes + " bytes...");
                    handler.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
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
            Log.d(TAG, str);
            act.print(str);
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                handler.obtainMessage(CONNECTION_CANCELED_MESSAGE).sendToTarget();
                 str = "...Error data send: " + e.getMessage() + "...";
                Log.d(TAG, str);
                act.print(str);
            }
        }
    }

    void write(String msg)
    {
        if(btReadWrite != null) {
            btReadWrite.write(msg);
            String str = "...Message sent to Client: " + msg + "...";
            Log.d(TAG, str);
            act.print(str);
        }
    }

    void startBtServer() throws InterruptedException
    {
        acceptThread = new AcceptThread();
        acceptThread.start();
        String str = "...Bluetooth Server Started...";
        Log.d(TAG, str);
        Toast.makeText(act, str, Toast.LENGTH_LONG).show();
        act.print(str);
    }

    void stopBtServer()
    {
        acceptThread.cancel();
        String str = "...Bluetooth Server Stopped...";
        Log.d(TAG, str);
        act.print(str);
    }

    private void processMsg(String msg)
    {
       if(chkMsg(msg, MOVE)) {
            Log.d(TAG, "Process MV message from Client)");
            rdy();
            return;
        }
        if(chkMsg(msg, ST)) {
            Log.d(TAG, "Process STATUS message from Client)");
            rdy();
            return;
        }
        if(chkMsg(msg, BTRY)) {

            Log.d(TAG, "Process STATUS message from Client)");
            btryRes();
            return;
        }
        Log.d(TAG, "Unknown message received from Arduino");
    }

    private boolean chkMsg(String recMsg, String expMsg)
    {
        recMsg = recMsg.substring(1, 1+expMsg.length());
        return(recMsg.equals(expMsg));
    }

    private void rdy()
    {
        // send <RDY>
        outMessage = SM +
                RDY +
                EM;
        Log.d(TAG, outMessage);
        write(outMessage);
    }

    private void notRdy()
    {
        // send <NOT_RDY>
        outMessage = SM +
                NOT_RDY +
                EM;
        Log.d(TAG, outMessage);
        write(outMessage);
    }

    private void btryRes()
    {
        // send <BTRY>
        outMessage = SM +
                BTRY_RES + " 11.4V" +
                EM;
        Log.d(TAG, outMessage);
        write(outMessage);
    }
}