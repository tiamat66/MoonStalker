package si.vajnartech.moonstalker;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.Set;

import static si.vajnartech.moonstalker.C.ST_CONNECTED;
import static si.vajnartech.moonstalker.C.TAG;

interface BTInterface
{
  void showMessage(String msg);
  void exit(String msg);
  void progressOn();
  void progressOff();
  void onOk();
}

class BlueTooth extends AsyncTask<String, Void, Void>
{
  private BTInterface btInterface;

  private BluetoothAdapter btAdapter;
  private BluetoothDevice  pairedDevice = null;
  private String url;

  static BluetoothSocket socket;

  BlueTooth(String url, MainActivity act, BTInterface i)
  {
    btInterface = i;
    btAdapter = BluetoothAdapter.getDefaultAdapter();
    this.url = url;
    if (btAdapter == null) {
      Log.i(TAG, "Bluetooth not support");
      btInterface.exit("Bluetooth not support");
    } else {
      if (btAdapter.isEnabled())
        Log.i(TAG, "Bluetooth ON");
      else {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        act.startActivityForResult(enableBtIntent, 1);
      }
    }
    btInterface.progressOn();
    executeOnExecutor(THREAD_POOL_EXECUTOR);
  }

  private boolean getPairedDevices(String url)
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
      btInterface.exit("No paired device");
      return false;
    }

    if (pairedDevice == null) {
      Log.i(TAG, "Invalid paired device");
      btInterface.exit("Invalid paired device");
    }

    return true;
  }

  private void connect()
  {
    try {
      socket = pairedDevice.createRfcommSocketToServiceRecord(C.token);
    } catch (IOException e) {
      e.printStackTrace();
    }
    btAdapter.cancelDiscovery();

    try {
      assert socket != null;
      socket.connect();
      btInterface.progressOff();
      TelescopeStatus.set(ST_CONNECTED);
      btInterface.onOk();
    } catch (IOException connectException) {
      connectException.printStackTrace();
      try {
        Log.i(TAG, "Connection refused=" + socket);
        btInterface.showMessage("Connection refused");
        btInterface.progressOff();
        socket.close();
      } catch (IOException ignored) { }
    }
}

  @Override
  protected Void doInBackground(String... strings)
  {
    if (getPairedDevices(url))
      connect();
    return null;
  }
}
