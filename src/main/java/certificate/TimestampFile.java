package certificate;

import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampResponse;

import java.io.File;

public class TimestampFile {
    public static void timestampFile(String filePath){
        try {
            File directoryPath = new File(new File(filePath).getParent());
            String outputTimestampFile = directoryPath + "\\timestamp.tsr";

            byte[] fileHash = FileHashGenerator.generateSHA256Hash(filePath);

            TimeStampRequest tsRequest = TimestampRequestGenerator.createTimestampRequest(fileHash);

            TimeStampResponse tsResponse = TimestampServiceClient.sendTimestampRequest(tsRequest);

            TimestampResponseSaver.saveTimestampResponse(tsResponse, outputTimestampFile);

            System.out.println("Timestamp response saved to " + outputTimestampFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        timestampFile("C:\\Users\\jakob\\IdeaProjects\\BlockchainTest.zip");
    }
}


