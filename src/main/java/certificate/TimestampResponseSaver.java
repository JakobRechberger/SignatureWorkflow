package certificate;
import java.io.File;
import java.io.FileOutputStream;
import org.bouncycastle.tsp.TimeStampResponse;

public class TimestampResponseSaver {

    public static File saveTimestampResponse(TimeStampResponse tsResponse) throws Exception {
        File file = File.createTempFile("timestamp_", ".tsr");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(tsResponse.getEncoded());
        }
        return file;
    }
}
