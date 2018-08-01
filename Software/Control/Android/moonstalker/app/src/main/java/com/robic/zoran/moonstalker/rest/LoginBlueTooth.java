package com.robic.zoran.moonstalker.rest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.robic.zoran.moonstalker.MainActivity;
import com.robic.zoran.moonstalker.R;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class LoginBlueTooth extends Login
{
  private static final String TAG = "IZAA";

  private BluetoothAdapter btAdapter;
  private BluetoothDevice  pairedDevice;

  LoginBlueTooth(String url, MainActivity act, REST task)
  {
    super(url, act);

    this.act = act;
    this.task = task;

    btAdapter = BluetoothAdapter.getDefaultAdapter();
    checkBTState();
    getPairedDevices();
    token = "00001101-0000-1000-8000-00805F9B34FB";
  }

  private void getPairedDevices()
  {
    pairedDevice = null;
    Log.i(TAG, "Enter getPairedDevices");
    Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
    if (pairedDevices.size() > 0) {
      for (BluetoothDevice device : pairedDevices) {
        if (device.getName().contains(url)) {
          Log.i(TAG, device.getName() + "\n" + device.getAddress());
          pairedDevice = device;
        }
      }
    } else {
      Log.i(TAG, "No paired device");
      errorExit();
    }
  }

  private void checkBTState()
  {
    if (btAdapter == null) {
      Log.i(TAG, "Bluetooth not support");
      errorExit();
    } else {
      if (btAdapter.isEnabled()) Log.i(TAG, "Bluetooth ON");
      else {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        act.startActivityForResult(enableBtIntent, 1);
      }
    }
  }

  @Override
  public Integer connect()
  {
    try {
      socket = pairedDevice.createRfcommSocketToServiceRecord(UUID.fromString(token));
    } catch (IOException ignored) { }
    btAdapter.cancelDiscovery();

    try {
      assert socket != null;
      socket.connect();
      Log.i(TAG, "CONNECTION accepted = " + socket);
      return LOGIN_OK;
    } catch (IOException connectException) {
      try {
        Log.i(TAG, "CONNECTION refused = " + socket);
        socket.close();
        return LOGIN_ERR;
      } catch (IOException ignored) { }
    }
    return LOGIN_OK;
  }

  @SuppressWarnings("unchecked") @Override
  protected void onPostExecute(Integer responseCode)
  {
    Log.i(TAG, "Login onPostExecute responseCode=" + responseCode);
    if (task != null && socket != null && responseCode == LOGIN_OK) {
      task.executeOnExecutor(TPE.THREAD_POOL_EXECUTOR, (Object[]) new String[]{token});
    } else if (task != null)
      task.fail(responseCode);
  }
}
