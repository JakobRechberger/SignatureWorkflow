package signandVerify;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.tsp.*;

import static certificate.TimestampFile.timestampFile;
import static signandVerify.OCSPValidator.checkCertStatus;

public class SignWithFreeTSA {

    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        // Generate RSA Key Pair for Signing
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        // Generate Self-Signed Certificate (for demonstration)
        X509Certificate cert = generateCertificate("CN=Example", keyPair, 365, "SHA256withRSA");

        // Data to Sign
        String filepath = "C:\\Users\\jakob\\IdeaProjects\\BlockchainTest.zip";
        File fileToSign = new File(filepath);  // Replace with your file path
        byte[] fileData = readFile(fileToSign);

        // Sign the data
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(fileData);
        byte[] signedData = signature.sign();

        // Get Timestamp from FreeTSA
        timestampFile(filepath);

        // Save signature and timestamp to file (for archival)
        saveToFile("signed_data.bin", signedData);

        System.out.println("Signature and timestamp generated and saved.");
        FileInputStream fis = new FileInputStream("tsa.crt");
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate tsaCert = (X509Certificate) certFactory.generateCertificate(fis);
        fis.close();
        checkCertStatus(cert ,tsaCert);
    }

    private static X509Certificate generateCertificate(String dn, KeyPair keyPair, int days, String algorithm)
            throws Exception {
        X500Name issuer = new X500Name(dn);
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date startDate = new Date();
        Date endDate = new Date(System.currentTimeMillis() + (days * 86400000L));
        X509CertificateHolder certHolder = new JcaX509v3CertificateBuilder(
                issuer, serial, startDate, endDate, issuer, keyPair.getPublic())
                .build(new JcaContentSignerBuilder(algorithm).build(keyPair.getPrivate()));
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
    }

    // Utility methods
    private static byte[] readFile(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return data;
    }

    private static void saveToFile(String fileName, byte[] data) throws Exception {
        FileOutputStream fos = new FileOutputStream(fileName);
        fos.write(data);
        fos.close();
    }
}

