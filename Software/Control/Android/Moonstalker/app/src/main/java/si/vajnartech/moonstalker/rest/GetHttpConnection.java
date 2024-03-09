package si.vajnartech.moonstalker.rest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

public class GetHttpConnection
{
    private String urlString;

    public GetHttpConnection(String urlString)
    {
        this.urlString = urlString;
    }

    private String checkRedirect(String urlString, HttpURLConnection conn) throws IOException
    {
        switch (conn.getResponseCode()) {
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
                String location = conn.getHeaderField("Location");
                location = URLDecoder.decode(location, "UTF-8");
                URL base = new URL(urlString);
                URL next = new URL(base, location);  // Deal with relative URLs
                return next.toExternalForm();
        }
        return null;
    }

    public HttpURLConnection get() throws IOException
    {
        int               times = 0;
        HttpURLConnection conn;
        while (true) {
            if (times > 5)
                throw new IOException("Stuck in redirect loop");

            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            setConnParams(conn);

            conn.connect();
            String redirectString = checkRedirect(urlString, conn);
            if (redirectString != null) {
                urlString = redirectString;
                times++;
                continue;
            }
            break;
        }

        return conn;
    }

    public void setConnParams(HttpURLConnection conn) throws RuntimeException, IOException
    {
        throw new RuntimeException("Not implemented");
    }
}

