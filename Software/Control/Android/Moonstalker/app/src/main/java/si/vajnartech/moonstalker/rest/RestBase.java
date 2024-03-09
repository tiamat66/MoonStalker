package si.vajnartech.moonstalker.rest;


import static si.vajnartech.moonstalker.OpCodes.MSG_CONN_ERROR;

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

import si.vajnartech.moonstalker.processor.QueueUI;

public abstract class RestBase<P, R> extends AsyncTaskExecutor<String, Void, R>
{
    protected QueueUI queue;
    private final Gson gson = new Gson();
    public static final String TAG                = "REST";
    public static final int    SOCKET_TIMEOUT     = -100;
    public static final int    CONNECT_EXCEPTION  = -101;
    public static final int    IO_EXCEPTION       = -102;

    protected       int       READ_TIMEOUT    = 0;
    protected       String    REQUEST_METHOD  = "POST";
    protected       String    url;
    protected       String    token           = "";
    protected       int       responseCode    = 0;
    protected       String    responseData    = "";
    protected       String    responseMessage = "";
    protected       Exception serverException = null;

    public RestBase(String url, String user, String pwd, String auth, QueueUI queue)
    {
        super();
        this.url = url;
        this.queue = queue;
        new RestLogin(this, auth, user, pwd).execute();
    }

    @Override
    protected R doInBackground(String params)
    {
        if (params == null || params.isEmpty())
            return null;
        token = params;
        return backgroundFunc();
    }

    protected R callServer(P params)
    {
        try {
            HttpURLConnection conn = null;
            try {
                conn = new GetHttpConnection(url)
                {
                    @Override public void setConnParams(HttpURLConnection conn) throws IOException
                    {
                        conn.setRequestMethod(REQUEST_METHOD);
                        if ("POST".equals(REQUEST_METHOD))
                            conn.setDoOutput(true);

                        conn.setConnectTimeout(READ_TIMEOUT);
                        conn.setReadTimeout(READ_TIMEOUT);
                        conn.setRequestProperty("Authorization", "Token " + token);
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
                    BufferedInputStream is   = new BufferedInputStream(conn.getErrorStream());
                    BufferedReader      br   = new BufferedReader(new InputStreamReader(is));
                    String              l;
                    StringBuilder       resj = new StringBuilder();
                    while ((l = br.readLine()) != null)
                        resj.append(l);
                    responseData = resj.toString();
                    br.close();
                    is.close();
                }
                return result;
            } finally {
                if (conn != null)
                    conn.disconnect();
            }
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
        }
        return null;
    }

    protected abstract R deserialize(BufferedReader br);

    public abstract R backgroundFunc();

    public void fail(Integer responseCode)
    {
        onFailure();
    }

    protected void onFailure()
    {
        queue.obtainMessage(MSG_CONN_ERROR, null).sendToTarget();
    }

    @Override
    protected final void onCancelled()
    {
        onFailure();
    }
}

