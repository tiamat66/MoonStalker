package com.robic.zoran.arduinoemulator;

/**
 * Created by zoran on 14.3.2016.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class BlueToothService {

    private static final String TAG = "bluetooth1";
    // SDP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String NAME = "MOONSTALKER";

    // MAC-address of Bluetooth module (you must edit this line)
    private static String address = "00:15:FF:F2:19:00";
    private static final int RECIEVE_MESSAGE = 1; // Status  for Handler

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private MainActivity mainActivity;
    private boolean isBtPresent = false;
    private boolean isBtPOn = false;
    private StringBuilder sb = new StringBuilder();
    //private ConnectedThread mConnectedThread = null;
    private Set<BluetoothDevice> pairedDevices;
    private AcceptThread acceptThread;


    TextView txtArduino;
    static Handler h;
    String rcvdMsg;

    public BlueToothService(MainActivity myMainActivity) {

        mainActivity = myMainActivity;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        acceptThread = new AcceptThread();

        /*
        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:                                                   // if receive massage
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);                 // create string from bytes array
                        sb.append(strIncom);                                                // append string
                        int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line
                        if (endOfLineIndex > 0) {                                            // if end-of-line,
                            String sbprint = sb.substring(0, endOfLineIndex);               // extract string
                            sb.delete(0, sb.length());                                      // and clear
                            txtArduino.setText("Data from Arduino: " + sbprint);            // update TextView
                            rcvdMsg = sbprint;
                        }
                        //Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                        break;
                }
            };
        };*/
        checkBTState();
    }

    private void checkBTState() {

        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        Log.d(TAG, "...enter checkBTState...");
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
        mainActivity.finish();
    }

    public void getPairedDevices() {

        Log.d(TAG, "...Enter getPairedDevices...");
        pairedDevices = btAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                Log.d(TAG, device.getName() + "\n" + device.getAddress());
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    // This acts as Server
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = btAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    //manageConnectedSocket(socket);
                    Log.d(TAG, "...Connection was accepted...");
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }
    }

    public void startBtServer() {

        acceptThread.start();
        Log.d(TAG, "...Bluetooth Server Started...");
    }
}