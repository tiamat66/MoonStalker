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
 * <p/>
 * Class contains functionality for BlueTooth Service
 */
public class BlueToothService {

    // Delimiters
    private static final String SM = "<";
    private static final String EM = ">";

    //Messages
    //IN
    private static final String RDY = "RDY";
    private static final String NOT_RDY = "NOT_RDY";
    private static final String BTRY_LOW = "BTRY_LOW";
    private static final String BTRY_RESULT = "BTRY";

    private static final String TAG = "bluetooth1";
    // SDP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Status  for Handler
    private static final int CONNECTION_ACCEPTED_MESSAGE = 2;
    private static final int CONNECTION_CANCELED_MESSAGE = 3;
    private static final int BTRY_RESULT_MESSAGE = 4;
    private static final int RDY_MESSAGE = 5;
    private static final int BTRY_LOW_MESSAGE = 7;
    private static final int NOT_READY_MESSAGE = 8;
    private static final int CONNECTION_TIMED_OUT_MESSAGE = 9;

    private static final int CONNECTION_TIMEOUT = 20;

    private BluetoothAdapter btAdapter = null;
    private MainActivity mainActivity;
    private BtReadWrite btReadWrite;
    private BluetoothDevice pairedDevice;
    private boolean isConnected = false;
    private boolean connecting = false;
    private int waiting = 0;

    private ConnectThread connectThread;
    private Shcheduler scheduler;

    private Handler h;

    String rcvdMsg = "";

    public BlueToothService(final MainActivity myMainActivity) {

        mainActivity = myMainActivity;
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case CONNECTION_ACCEPTED_MESSAGE:
                        isConnected = true;
                        connecting = false;
                        mainActivity.updateStatus();
                        Log.d(TAG, "BlueTooth connection accepted");
                        mainActivity.telescope.control.st();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //Prompt to calibrate
                        if (!mainActivity.telescope.isCalibrated)
                            mainActivity.calibrateMessage();
                        break;
                    case CONNECTION_CANCELED_MESSAGE:
                        Log.d(TAG, "...BT connection canceled...");
                        isConnected = false;
                        connecting = false;
                        mainActivity.telescope.clearReady();
                        mainActivity.updateStatus();
                        mainActivity.connectionCanceled();
                        if(mainActivity.telescope.isTracing()) {
                            mainActivity.telescope.offTrace();
                            mainActivity.updateStatus();
                        }

                        break;
                    case RDY_MESSAGE:
                        Log.d(TAG, "Process RDY message from Arduino...");
                        myMainActivity.telescope.setReady();
                        mainActivity.updateStatus();
                        break;
                    case BTRY_RESULT_MESSAGE:
                        byte[] readBuf = (byte[]) msg.obj;
                        rcvdMsg = new String(readBuf, 0, msg.arg1);
                        Log.d(TAG, "Process BTRY_RESULT message from Arduino..." + rcvdMsg);
                        mainActivity.telescope.btryVoltage(rcvdMsg, BTRY_RESULT);
                        break;
                    case NOT_READY_MESSAGE:
                        mainActivity.telescope.clearReady();
                        mainActivity.updateStatus();
                        break;
                    case BTRY_LOW_MESSAGE:
                        mainActivity.telescope.setBatteryOk(false);
                        mainActivity.telescope.clearReady();
                        mainActivity.updateStatus();
                        break;
                    case CONNECTION_TIMED_OUT_MESSAGE:
                        mainActivity.connectionTimedOutMessage();
                        break;
                }
            }
        };

        checkBTState();
    }

    private void processMsg(String msg, int bytes, byte[] buffer) {

        if (chkMsg(msg, RDY)) {

            h.obtainMessage(RDY_MESSAGE).sendToTarget();
            return;
        }

        if (chkMsg(msg, BTRY_RESULT)) {

            h.obtainMessage(BTRY_RESULT_MESSAGE, bytes, -1, buffer).sendToTarget();
            return;
        }

        if (chkMsg(msg, NOT_RDY)) {


            return;
        }

        if (chkMsg(msg, BTRY_LOW)) {

            Log.d(TAG, "Process BTRY_LOW message from Arduino");
            return;
        }

        Log.d(TAG, "Unknown message received from Arduino");
    }

    private boolean chkMsg(String recMsg, String expMsg) {
        recMsg = recMsg.substring(1, 1 + expMsg.length());
        return (recMsg.equals(expMsg));
    }

    private void checkBTState() {

        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
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

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException ignored) {
            }
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
                } catch (IOException ignored) {
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            // Send to message queue Handler
            h.obtainMessage(CONNECTION_ACCEPTED_MESSAGE).sendToTarget();
            btReadWrite = new BtReadWrite(mmSocket);
            btReadWrite.start();
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void getPairedDevices() {

        int i = 0;

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

        getPairedDevices();

        if (pairedDevice == null) {
            Log.d(TAG, "...No paired device...");
        } else {
            connecting = true;
            connectThread = new ConnectThread(pairedDevice);
            connectThread.start();
            scheduler = new Shcheduler();
            scheduler.start();
        }
    }

    private class Shcheduler extends Thread {

        public void run() {

            while (connecting) {

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                waiting++;
                if (waiting == CONNECTION_TIMEOUT) {

                    h.obtainMessage(CONNECTION_TIMED_OUT_MESSAGE).sendToTarget();
                }
            }
            waiting = 0;
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
            } catch (IOException ignored) {
            }

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
                    bytes = mmInStream.read(buffer);
                    rcvdMsg = new String(buffer, 0, bytes);
                    processMsg(rcvdMsg, bytes, buffer);
                } catch (IOException e) {
                    h.obtainMessage(CONNECTION_CANCELED_MESSAGE).sendToTarget();
                    Log.d(TAG, "...Error data receive: " + e.getMessage() + "...");
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
                h.obtainMessage(CONNECTION_CANCELED_MESSAGE).sendToTarget();
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }

    public void write(String msg) {

        if (btReadWrite != null) {
            btReadWrite.write(msg);
            Log.d(TAG, "Message sent to Arduino: " + msg);
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean isConnecting() {
        return connecting;
    }
}
