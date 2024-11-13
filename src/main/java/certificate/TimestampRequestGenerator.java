package certificate;

import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import java.security.NoSuchAlgorithmException;

public class TimestampRequestGenerator {

    public static TimeStampRequest createTimestampRequest(byte[] fileHash) {
        TimeStampRequestGenerator tsqGenerator = new TimeStampRequestGenerator();
        tsqGenerator.setCertReq(true);
        return tsqGenerator.generate(org.bouncycastle.tsp.TSPAlgorithms.SHA256, fileHash);
    }
}
