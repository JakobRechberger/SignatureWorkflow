package certificate;

import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampResponse;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TimestampServiceClient {
    public static TimeStampResponse sendTimestampRequest(TimeStampRequest tsRequest) throws Exception {
        // FreeTSA URL
        URL url = new URL("https://freetsa.org/tsr");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/timestamp-query");

        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        out.write(tsRequest.getEncoded());
        out.flush();
        out.close();

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Failed to connect to TSA. HTTP error code: " + connection.getResponseCode());
        }

        // Read the response
        InputStream in = connection.getInputStream();
        TimeStampResponse tsResponse = new TimeStampResponse(in);
        in.close();

        // Validate the timestamp response against the request
        tsResponse.validate(tsRequest);

        return tsResponse;
    }
}
