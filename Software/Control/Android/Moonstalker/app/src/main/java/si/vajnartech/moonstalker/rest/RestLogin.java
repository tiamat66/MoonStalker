package si.vajnartech.moonstalker.rest;

import android.net.Uri;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class RestLogin extends AsyncTaskExecutor<String, Void, Integer>
{
    protected RestBase<?, ?> task;
    protected String token = "";
    protected String user, pwd, url;
    Gson gson = new Gson();

    public RestLogin(RestBase<?, ?> task, String url, String user, String pwd)
    {
        super();
        this.task = task;
        this.user = user;
        this.pwd = pwd;
        this.url =url;
    }

    @Override
    protected Integer doInBackground(String params)
    {
        try {
            HttpURLConnection conn = null;
            try {
                conn = new GetHttpConnection(url)
                {
                    @Override public void setConnParams(HttpURLConnection conn) throws IOException
                    {
                        conn.setConnectTimeout(5000);
                        conn.setReadTimeout(5000);
                        conn.setRequestMethod("POST");
                        conn.setDoOutput(true);

                        String post = new Uri.Builder()
                                .appendQueryParameter("username", user)
                                .appendQueryParameter("password", pwd)
                                .build().getEncodedQuery();

                        OutputStream   os  = conn.getOutputStream();
                        BufferedWriter out = new BufferedWriter(
                                new OutputStreamWriter(os, StandardCharsets.UTF_8));
                        out.write(post);
                        out.flush();
                        out.close();
                        os.close();
                    }
                }.get();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedInputStream is = new BufferedInputStream(conn.getInputStream());
                    BufferedReader      br = new BufferedReader(new InputStreamReader(is));
                    try {
                        token = gson.fromJson(br, RegistrationToken.class).token;
                    } catch (Exception e) {
                        throw new IOException("Can't get registration token");
                    }
                    br.close();
                    is.close();
                }
                return conn.getResponseCode();
            } finally {
                if (conn != null)
                    conn.disconnect();
            }
        } catch (SocketTimeoutException ignored) {
            task.onFailure();
        } catch (IOException e) {
            task.onFailure();
            e.printStackTrace();
        }
        return null;
    }

    protected void onPostExecute(Integer responseCode) //(HttpResponse response)
    {
        try {
            if (responseCode != null && responseCode == HttpURLConnection.HTTP_OK && task != null) {
                task.execute(token);
            } else if (task != null) {
                task.fail(responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class RegistrationToken
{
    public String token;
}
