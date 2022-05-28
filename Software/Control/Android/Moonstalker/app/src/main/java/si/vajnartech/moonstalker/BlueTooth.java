package si.vajnartech.moonstalker;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Set;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import static si.vajnartech.moonstalker.C.TAG;

interface BTInterface
{
  void progressOn();

  void progressOff();

  void onOk();

  void onError(String msg);
}

abstract class BlueTooth extends AsyncTask<String, Void, Void> implements BTInterface
{
  private static  BluetoothAdapter btAdapter;
  static volatile BluetoothSocket  socket;

  private final String url;

  private BluetoothDevice pairedDevice = null;

  protected final WeakReference<MainActivity> act;

  BlueTooth(String url, MainActivity act)
  {
    super();

    this.url = url;
    this.act = new WeakReference<>(act);
  }

  static void initBluetooth(MainActivity act)
  {
    btAdapter = BluetoothAdapter.getDefaultAdapter();

    if (btAdapter == null) {
      Log.i(TAG, "Bluetooth not supported");
    } else {
      if (btAdapter.isEnabled())
        Log.i(TAG, "Bluetooth ON");
      else {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        ActivityResultLauncher<Intent> myActivityResultLauncher = act.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              if (result.getResultCode() == Activity.RESULT_OK) {
                Log.i(TAG, "Enabled BT service");
              } else {
                Log.i(TAG, "Error Enabling BT service");
              }
            });
        myActivityResultLauncher.launch(enableBtIntent);
      }
    }
  }

  private boolean getPairedDevices(String url)
  {
    pairedDevice = null;
    if (btAdapter == null) {
      onError(act.get().tx(R.string.connection_failed));
      return false;
    }

    if (ActivityCompat.checkSelfPermission(
        act.get(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
      Log.i(TAG, "Permission BLUETOOTH_CONNECT not granted");
      onError(act.get().tx(R.string.connection_failed));
      return false;
    }
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
        onError(act.get().tx(R.string.connection_failed));
      return false;
    }

    if (pairedDevice == null) {
      Log.i(TAG, "Invalid paired device");
        onError(act.get().tx(R.string.connection_failed));
    }

    return true;
  }

  private void connect()
  {
    progressOn();
    try {
      if (ActivityCompat.checkSelfPermission(
          act.get(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
        Log.i(TAG, "Permission BLUETOOTH_CONNECT not granted");
        return;
      }
      if (pairedDevice != null)
        socket = pairedDevice.createRfcommSocketToServiceRecord(C.token);
    } catch (IOException e) {
      e.printStackTrace();
    }
    btAdapter.cancelDiscovery();

    try {
      assert socket != null;
      socket.connect();
      progressOff();

      onOk();
    } catch (IOException connectException) {
      connectException.printStackTrace();
      onError(act.get().tx(R.string.connection_failed));
      try {
        Log.i(TAG, "Connection refused=" + socket);
        progressOff();
        socket.close();
      } catch (IOException ignored) {}
    }
  }

  @Override
  protected Void doInBackground(String... strings)
  {
    if (getPairedDevices(url))
      connect();
    return null;
  }

  static void disconnect()
  {
    if (socket != null) {
      try {
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
