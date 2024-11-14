package certificate;

import org.bouncycastle.tsp.TimeStampResponse;

import java.io.File;
import java.io.FileInputStream;

public class TimestampLoader {
    public static TimeStampResponse loadTimestampResponse(File tsrFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(tsrFile)) {
            return new TimeStampResponse(fis);
        }
    }
}
