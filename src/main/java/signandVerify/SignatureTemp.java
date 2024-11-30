package signandVerify;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class SignatureTemp {
    public static List<File> signFile(File file) {
        String filepath = file.getAbsolutePath();
        String filename = file.getName().substring(0,file.getName().length()-4);
        String contentRoot = file.getParent();
        List<File> files = new ArrayList<>();
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            keyGen.initialize(2048, random);
            KeyPair keyPair = keyGen.generateKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();
            java.security.Signature signature = java.security.Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);

            byte[] bytes = Files.readAllBytes(Paths.get(filepath).toAbsolutePath());
            signature.update(bytes);
            byte[] digitalSignature = signature.sign();
            Path directoryPath = Paths.get(contentRoot + File.separator + "signatures");
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }
            File signatureFile = File.createTempFile("signature", "_file");
            File publickeyFile = File.createTempFile("publickey", "_file");
            Files.write(signatureFile.toPath(), digitalSignature);
            files.add(signatureFile);
            Files.write(publickeyFile.toPath(), keyPair.getPublic().getEncoded());
            files.add(publickeyFile);

            return files;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
