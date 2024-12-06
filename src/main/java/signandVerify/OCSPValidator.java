package signandVerify;

import java.math.BigInteger;
import java.net.URI;
import java.security.cert.*;
import java.util.Collections;

public class OCSPValidator {

    public static void checkCertStatus(X509Certificate cert, X509Certificate issuerCert) throws Exception {
        URI ocspUri = new URI("http://www.freetsa.org:2560");

        // Create OCSP request
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        CertPath certPath = certFactory.generateCertPath(Collections.singletonList(cert));
        TrustAnchor trustAnchor = new TrustAnchor(issuerCert, null);
        PKIXParameters params = new PKIXParameters(Collections.singleton(trustAnchor));
        params.setRevocationEnabled(true);

        CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX");
        certPathValidator.validate(certPath, params);

        System.out.println("OCSP Check Completed: Certificate is valid.");
    }
}

