package si.vajnartech.moonstalker.rest;


import static si.vajnartech.moonstalker.OpCodes.MSG_CONN_ERROR;
import static si.vajnartech.moonstalker.processor.Controller.URL;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;

import si.vajnartech.moonstalker.processor.Processor;

// Body so parametri, ki jih posljemo v telesu requesta
public abstract class RestBase<Body, R> extends AsyncTaskExecutor<String, Void, R>
{
    protected Processor machine;
    protected final Gson gson = new Gson();
    public static final int    SOCKET_TIMEOUT     = -100;
    public static final int    CONNECT_EXCEPTION  = -101;
    public static final int    IO_EXCEPTION       = -102;

    private static String cachedToken = "";

    protected int readTimeout = 5000;
    protected       String    REQUEST_METHOD  = "POST";
    protected       String    url;
    private String token = "";
    protected       int       responseCode    = 0;
    protected       String    responseData    = "";
    protected       String    responseMessage = "";
    protected       Exception serverException = null;

    public RestBase(String url, Processor machine)
    {
        this.url = url;
        this.machine = machine;
        if (cachedToken.isEmpty()) {
            new Login(this, machine).execute(null);
        } else {
            this.execute(cachedToken);
        }
    }

    // login
    public RestBase(Processor machine)
    {
        url =  URL + "login";
        this.machine = machine;
    }

    public static void setCachedToken(String token) {
        cachedToken = token;
    }

    @Override
    protected R doInBackground(String params)
    {
        token = params; // token je null ob loginu sele sedaj se logiramo
        return backgroundFunc();
    }

    protected R callServer(Body params)
    {
        HttpURLConnection conn = null;
        try {
            conn = new GetHttpConnection(url)
            {
                @Override
                public void setConnParams(HttpURLConnection conn) throws IOException
                {
                    // not login
                    if (token != null) {
                        readTimeout = 0;
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }

                    conn.setRequestMethod(REQUEST_METHOD);
                    if ("POST".equals(REQUEST_METHOD))
                        conn.setDoOutput(true);
                    conn.setConnectTimeout(readTimeout);
                    conn.setReadTimeout(readTimeout);

                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Content-Encoding", "utf-8");

                    if (params != null) {
                        OutputStream os = conn.getOutputStream();
                        synchronized (params) {
                            os.write(gson.toJson(params).getBytes());
                        }
                        os.close();
                    }
                }
            }.get();

            R result = null;
            responseCode = conn.getResponseCode();
            responseMessage = conn.getResponseMessage();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is), 512);
                result = deserialize(br);
                br.close();
            } else {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    BufferedInputStream is = new BufferedInputStream(errorStream);
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String l;
                    StringBuilder resj = new StringBuilder();
                    while ((l = br.readLine()) != null)
                        resj.append(l);
                    responseData = resj.toString();
                    br.close();
                    is.close();
                }
            }
            return result;
        } catch (SocketTimeoutException e) {
            responseCode = SOCKET_TIMEOUT;
            serverException = e;
            responseMessage = "Timeout connecting to " + url;
            onFailure();
        } catch (ConnectException e) {
            responseCode = CONNECT_EXCEPTION;
            serverException = e;
            responseMessage = "Connect exception";
            // tu ni bilo pohandlan izpad internetne povezave in je progress bar pri messages in dokumentih ostajal
            cancel(false);
        } catch (IOException e) {
            responseCode = IO_EXCEPTION;
            serverException = e;
            responseMessage = "IO exception";
            onFailure();
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        return null;
    }

    protected abstract R deserialize(BufferedReader br);

    public abstract R backgroundFunc();

    public void fail(@SuppressWarnings("unused") Integer responseCode)
    {
        onFailure();
    }

    protected void onFailure()
    {
        machine.set(MSG_CONN_ERROR);
    }

    @Override
    protected final void onCancelled()
    {
        onFailure();
    }
}
