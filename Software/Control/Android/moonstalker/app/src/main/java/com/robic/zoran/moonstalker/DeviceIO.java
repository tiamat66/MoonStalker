package com.robic.zoran.moonstalker;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class DeviceIO extends Thread
{
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private MainActivity act;

    DeviceIO(MainActivity act)
    {
        this.act = act;
    }

    @Override
    public void run()
    {
        byte[] buffer = new byte[256];
        int bytes;
        while (act.getBt().getBTsocket() == null) {
            try {sleep(500);}
            catch (InterruptedException e) {e.printStackTrace();}
        }
        Log.i(Telescope.TAG, "BT socket OK.");
        BluetoothSocket socket = act.getBt().getBTsocket();
        try {
            mmInStream = socket.getInputStream();
            mmOutStream= socket.getOutputStream();
        } catch (IOException ignored) {}

        while (true) {
            try {
                bytes = mmInStream.read(buffer);
                String rcvdMsg = new String(buffer, 0, bytes);
                act.getCtr().inMsgProcess(rcvdMsg, bytes, buffer);
            } catch (IOException e) {
                act.getBt().getBtMessageHandler().obtainMessage(BlueToothService.CONNECTION_CANCELED_MESSAGE).sendToTarget();
                Log.d(Telescope.TAG, "Error data receive: " + e.getMessage());
                break;
            }
        }
    }

    void write(String message)
    {
        Log.d(Telescope.TAG, "Data to send: " + message);
        byte[] msgBuffer = message.getBytes();
        try {
            mmOutStream.write(msgBuffer);
            Log.d(Telescope.TAG, "Message sent to Telescope Control: " + message);
        } catch (IOException e) {
            act.getBt().getBtMessageHandler().obtainMessage(BlueToothService.CONNECTION_CANCELED_MESSAGE).sendToTarget();
            Log.d(Telescope.TAG, "Error data send: " + e.getMessage());
        }
    }
}
