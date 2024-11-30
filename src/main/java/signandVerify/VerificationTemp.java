package signandVerify;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

public class VerificationTemp {
    public static boolean verifySignature(byte[] file, byte[] signature_file, byte[] publickey_file){
        try {

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publickey_file);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
            java.security.Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(file);

            return signature.verify(signature_file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
