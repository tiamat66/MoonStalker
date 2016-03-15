package com.robic.zoran.moonstalker;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
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
    private static final int RECIEVE_MESSAGE = 1; // Status  for Handler

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private MainActivity mainActivity;
    private boolean isBtPresent = false;
    private boolean isBtPOn = false;
    private StringBuilder sb = new StringBuilder();
    private ConnectThread connectThread;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothDevice pairedDevice;

    static Handler h;
    String rcvdMsg;

    public BlueToothService(MainActivity myMainActivity) {

        mainActivity = myMainActivity;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
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
            } catch (IOException e) { }
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
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            Log.d(TAG, "... Do work to manage the connection (in a separate thread)...");
            //manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    public void getPairedDevices() {

        Log.d(TAG, "...Enter getPairedDevices...");
        pairedDevices = btAdapter.getBondedDevices();
        // If there are paired devices
        // Loop through paired devices
        if (pairedDevices.size() > 0) for (BluetoothDevice device : pairedDevices) {
            Log.d(TAG, device.getName() + "\n" + device.getAddress());
            pairedDevice = device;
            break;
        }
    }

    public void connect() {

        if(pairedDevice == null) {
            Log.d(TAG, "...No paired device...");
        } else {
            connectThread = new ConnectThread(pairedDevice);
            connectThread.start();
        }
    }


}
