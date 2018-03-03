package com.robic.zoran.moonstalker;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

class BlueToothService
{
    private static final String TAG = "Bluetooth";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Bluetooth connection status
    private static final int CONNECTION_ACCEPTED_MESSAGE =  1;
    static final int         CONNECTION_CANCELED_MESSAGE =  2;
    private static final int CONNECTION_TIMED_OUT_MESSAGE = 3;

    private static final int TIMEOUT_CONNECTION = 20;

    private BluetoothAdapter btAdapter = null;
    private MainActivity act;
    private BluetoothDevice pairedDevice;
    boolean connected = false;
    boolean connecting = false;
    private int waiting = 0;
    private BTMessageHandler btMessageHandler;
    private BluetoothSocket BTsocket = null;

    BlueToothService(MainActivity act)
    {
        this.act = act;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btMessageHandler = new BTMessageHandler();
        checkBTState();
    }

    BluetoothSocket getBTsocket()
    {
        return BTsocket;
    }

    BTMessageHandler getBtMessageHandler()
    {
        return btMessageHandler;
    }

    private void checkBTState()
    {
        if (btAdapter == null) act.errorExit("Fatal Error",
                "Bluetooth not support");
        else {
            if (btAdapter.isEnabled()) Log.d(TAG, "Bluetooth ON");
            else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                act.startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void getPairedDevices()
    {
        Log.d(TAG, "Enter getPairedDevices");
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) for (BluetoothDevice device : pairedDevices) {
            Log.d(TAG, device.getName() + "\n" + device.getAddress());
            pairedDevice = device;
        }
    }

    void connect()
    {
        getPairedDevices();
        if (pairedDevice == null) Log.d(TAG, "No paired device");
        else {
            connecting = true;
            act.updateStatus();
            ConnectThread connectThread = new ConnectThread(pairedDevice);
            connectThread.start();
            Scheduler scheduler = new Scheduler();
            scheduler.start();
        }
    }

    private class Scheduler extends Thread
    {
        public void run()
        {
            while (connecting) {
                try {
                    sleep(Control.timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                waiting++;
                if (waiting == TIMEOUT_CONNECTION)
                    btMessageHandler.obtainMessage(CONNECTION_TIMED_OUT_MESSAGE).sendToTarget();
            }
            waiting = 0;
        }
    }

    @SuppressLint("HandlerLeak")
    class BTMessageHandler extends Handler
    {
        @Override
        public void handleMessage(Message message)
        {
            switch (message.what) {
                case CONNECTION_ACCEPTED_MESSAGE:
                    Log.d(TAG, "BT connection accepted");
                    connected = true;
                    connecting = false;
                    act.updateStatus();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    act.getCtr().inMsgProcess(Control.INIT);
                    break;
                case CONNECTION_CANCELED_MESSAGE:
                    connected = false;
                    connecting = false;
                    act.connectionCanceled();
                    if(act.getTelescope().tracing)
                        act.getTelescope().setTrace(false);
                    act.getTelescope().setReady(Telescope.ERROR);
                    break;
                case CONNECTION_TIMED_OUT_MESSAGE:
                    connecting = false;
                    act.connectionTimedOutMessage();
                    break;
            }
        }
    }

    private class ConnectThread extends Thread
    {
        private final BluetoothSocket mmSocket;

        ConnectThread(BluetoothDevice device)
        {
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException ignored) {}
            mmSocket = tmp;
        }

        public void run() {
            btAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException ignored) {}
                return;
            }
            BTsocket = mmSocket;
            btMessageHandler.obtainMessage(CONNECTION_ACCEPTED_MESSAGE).sendToTarget();
        }
    }
}
