package com.robic.zoran.moonstalker;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by zoran on 9.3.2016.
 *
 * Class contains functionality for BlueTooth Service
 */
public class BlueToothService {

    private static final String TAG = "bluetooth1";
    // SDP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Status  for Handler
    private static final int RECIEVE_MESSAGE = 1;

    private BluetoothAdapter btAdapter = null;
    private MainActivity mainActivity;
    private boolean isBtPresent = false;
    private boolean isBtPOn = false;
    private BtReadWrite btReadWrite;
    private BluetoothDevice pairedDevice;

    static Handler h;
    String rcvdMsg = "";

    public BlueToothService(MainActivity myMainActivity) {

        mainActivity = myMainActivity;
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:
                        byte[] readBuf = (byte[]) msg.obj;
                        rcvdMsg = new String(readBuf, 0, msg.arg1);
                        Log.d(TAG, "Message received from Arduino:");
                        Log.d(TAG, rcvdMsg);
                        break;
                }
            };
        };

        checkBTState();
    }

    private void checkBTState() {

        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter == null) {
            isBtPresent = false;
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            isBtPresent = true;
            if (btAdapter.isEnabled()) {
                isBtPOn = true;
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mainActivity.startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message) {

        Log.d(TAG, title + " - " + message);
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException ignored) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            btAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException ignored) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            btReadWrite = new BtReadWrite(mmSocket);
            btReadWrite.start();

            //manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException ignored) { }
        }
    }

    public void getPairedDevices() {

        Log.d(TAG, "...Enter getPairedDevices...");
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        // If there are paired devices
        // Loop through paired devices
        if (pairedDevices.size() > 0) for (BluetoothDevice device : pairedDevices) {
            Log.d(TAG, device.getName() + "\n" + device.getAddress());
            pairedDevice = device;
        }
    }

    public void connect() {

        if(pairedDevice == null) {
            Log.d(TAG, "...No paired device...");
        } else {
            ConnectThread connectThread = new ConnectThread(pairedDevice);
            connectThread.start();
        }
    }

    private class BtReadWrite extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public BtReadWrite(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException ignored) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }

    public void write(String msg) {

        if(btReadWrite != null) {
            btReadWrite.write(msg);
            Log.d(TAG, "Message sent to Arduino: " + msg);
        }
    }

    public boolean isBtPOn() {
        return isBtPOn;
    }

    public boolean isBtPresent() {
        return isBtPresent;
    }

    public String getRcvdMsg() {

        String msg = rcvdMsg;
        rcvdMsg = "";

        return(msg);
    }
}
