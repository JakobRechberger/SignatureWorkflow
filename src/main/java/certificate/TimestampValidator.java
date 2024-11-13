package certificate;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenInfo;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class TimestampValidator {
    public static void verifyTimestamp(String filePath, String tsrFilePath) {
        try {
            //compare hash of file to hash retrieved from timestamped file
            Security.addProvider(new BouncyCastleProvider());
            byte[] fileHash = FileHashGenerator.generateSHA256Hash(filePath);
            TimeStampResponse tsResponse = TimestampLoader.loadTimestampResponse(tsrFilePath);
            TimeStampToken tsToken = tsResponse.getTimeStampToken();
            TimeStampTokenInfo tsInfo = tsToken.getTimeStampInfo();
            if (!MessageDigest.isEqual(fileHash, tsInfo.getMessageImprintDigest())) {
                throw new TSPException("The timestamp does not match the original file hash.");
            }
            System.out.println("The timestamp matches the original file hash.");

            //load tsa.crt of timestamp issuer (X.509Certificate from https://freetsa.org/index_de.php) and verify with timestamp
            FileInputStream fis = new FileInputStream("tsa.crt");
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate tsaCert = (X509Certificate) certFactory.generateCertificate(fis);
            fis.close();

            X509CertificateHolder tsaCertHolder = new X509CertificateHolder(tsaCert.getEncoded());
            SignerInformationVerifier verifier = new JcaSimpleSignerInfoVerifierBuilder()
                    .setProvider("BC")
                    .build(tsaCertHolder);
            tsToken.validate(verifier);
            System.out.println("The timestamp token is valid and was issued by the trusted TSA.");
            System.out.println("Timestamped at: " + tsInfo.getGenTime());

        } catch (Exception e) {
            System.err.println("Timestamp verification failed: " + e.getMessage());
            e.printStackTrace();
        }
}
    public static void main(String[] args) {
        String filePath = "C:\\Users\\jakob\\IdeaProjects\\BlockchainTest.zip";
        String tsrFilePath = "C:\\Users\\jakob\\IdeaProjects\\timestamp.tsr";

        verifyTimestamp(filePath, tsrFilePath);
    }
}
