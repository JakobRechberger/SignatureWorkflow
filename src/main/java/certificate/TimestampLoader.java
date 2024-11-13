package certificate;

import org.bouncycastle.tsp.TimeStampResponse;

import java.io.FileInputStream;

public class TimestampLoader {
    public static TimeStampResponse loadTimestampResponse(String tsrFilePath) throws Exception {
        try (FileInputStream fis = new FileInputStream(tsrFilePath)) {
            return new TimeStampResponse(fis);
        }
    }
}
