package certificate;
import java.io.FileOutputStream;
import org.bouncycastle.tsp.TimeStampResponse;

public class TimestampResponseSaver {

    public static void saveTimestampResponse(TimeStampResponse tsResponse, String outputPath) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(tsResponse.getEncoded());
        }
    }
}
