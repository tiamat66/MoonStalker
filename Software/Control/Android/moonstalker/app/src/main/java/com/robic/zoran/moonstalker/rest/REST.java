package com.robic.zoran.moonstalker.rest;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.robic.zoran.moonstalker.Control;
import com.robic.zoran.moonstalker.Json;
import com.robic.zoran.moonstalker.MainActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;

@SuppressWarnings("ALL")
public abstract class REST<T> extends AsyncTask<String, Void, T>
{
  static final String TAG = "IZAA";

  private String url   = "";
  private String token = "";
  protected MainActivity act;


  private final Class<T> resultClass;

  private Login login;

  @SuppressWarnings("unchecked") REST(String url, MainActivity act)
  {
    super();
    resultClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).
        getActualTypeArguments()[0];
    this.url = url;
    this.act = act;
    login = new LoginBlueTooth(url, act, this);
    login.executeOnExecutor(Login.TPE.THREAD_POOL_EXECUTOR);
  }

  @Override
  protected T doInBackground(String... params)
  {
    try {
      if (params.length > 0)
        token = params[0];
      return backgroundFunc();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private void write(String message, OutputStream os)
  {
    Log.i(TAG, "Data to send: " + message);
    byte[] msgBuffer = message.getBytes();
    try {
      os.write(msgBuffer);
      Log.i(TAG, "Message sent to Telescope Control: " + message);
    } catch (IOException e) {
      Log.i(TAG, "Error data send: " + e.getMessage());
    }
  }

  T deusExMachina(Object params)
  {
    if (params == null)
      return null;

    try {
      OutputStream outStream = login.socket.getOutputStream();
      write(Json.toJson(params), outStream);

      T           result;
      InputStream inStream = login.socket.getInputStream();
      if (resultClass != Void.class) {
        BufferedReader br = new BufferedReader(new InputStreamReader(inStream), 512);
        result = Json.fromJson(br, resultClass);
        br.close();
        return result;
      }
    } catch (IOException ignored) {
      Log.i(TAG, "IOException in deusExMachina");

    }

    return null;
  }

  abstract T backgroundFunc();

  abstract void fail(int responseCode);

  void errorExit()
  {
    act.finish();
    System.exit(0);
  }
}

