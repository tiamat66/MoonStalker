package si.vajnartech.moonstalker.rest;

import android.os.AsyncTask;
import android.os.Environment;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.net.HttpURLConnection;
import java.net.URL;

interface OnFail
{
  void execute();
}

@SuppressWarnings({"unchecked", "ConstantConditions", "unused"})
public abstract class REST<T> extends AsyncTask<String, Void, T>
{
  static final int OUTPUT_TYPE_JSON   = 0;
  static final int OUTPUT_TYPE_STRING = 1;
  static final int OUTPUT_TYPE_STREAM = 2;

  // ce dostopamo od zunaj
//  static final String SERVER_ADDRESS = "http://89.142.196.96:8007/";
  static final String SERVER_ADDRESS = "https://192.168.1.6:8007/";
  static final String GET_OBJECTS    = SERVER_ADDRESS + "rest/get-object-list/%s"; // type


  private final String url;
  private       String token = "";
  protected OnFail onFail;

  private final Class<T> resultClass;

  private final Gson gson;

  REST(String url, String userName, String password, String server, OnFail onFail)
  {
    super();
    resultClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    this.url = url;
    this.gson = new Gson();
    new Login((REST<Integer>) this, userName, password, server).executeOnExecutor(Login.THREAD_POOL_EXECUTOR);
    this.onFail = onFail;
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

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void writeToInputStream(InputStream in, OutputStream out)
  {
    byte[] buffer = new byte[4096];
    int    len;

    try {
      File fname = new File(Environment.getExternalStorageDirectory().getPath() + "/text.json");
      fname.createNewFile();
      FileOutputStream test = new FileOutputStream(fname);

      while ((len = in.read(buffer)) != -1) {
        out.write(buffer, 0, len);
        test.write(buffer, 0, len);
      }
      test.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  T callServer(Object params, int objectType)
  {
    try {
      URL               url  = new URL(this.url);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      int readTimeout = 0;
      conn.setConnectTimeout(readTimeout);
      conn.setReadTimeout(readTimeout);
      conn.setDoOutput(true);

      conn.setRequestProperty("Authorization", "Token " + token);
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("Content-Encoding", "utf-8");

      if (params != null) {
        OutputStream os = conn.getOutputStream();
        switch (objectType) {
        case OUTPUT_TYPE_JSON:
          os.write(gson.toJson(params).getBytes());
          break;
        case OUTPUT_TYPE_STRING:  // plain string
          os.write(((String) params).getBytes());
          break;
        case OUTPUT_TYPE_STREAM:  // stream
          InputStream is = (InputStream) params;
          writeToInputStream(is, os);
          is.close();
          break;
        }
        os.close();
      }
      conn.connect();
      T   result       = null;
      int responseCode = conn.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        if (resultClass != Void.class) {
          BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()), 512);
          result = gson.fromJson(br, resultClass);
          br.close();
        }
      } else {
        BufferedInputStream is   = new BufferedInputStream(conn.getErrorStream());
        BufferedReader      br   = new BufferedReader(new InputStreamReader(is));
        String              l;
        StringBuilder       resj = new StringBuilder();
        while ((l = br.readLine()) != null)
          resj.append(l);
        br.close();
        is.close();
      }
      return result;
    } catch (IOException e) {
      if (onFail != null)
        onFail.execute();
      e.printStackTrace();
    }
    return null;
  }

  abstract T backgroundFunc();
}

