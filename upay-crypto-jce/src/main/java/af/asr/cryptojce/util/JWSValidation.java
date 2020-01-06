package af.asr.cryptojce.util;


import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import af.asr.cryptojce.exception.crypto.spi.JwsSpec;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Component;

/**
 *
 * This class will verify and sign the JWT
 *
 */
@Component
public class JWSValidation implements JwsSpec<String, String, X509Certificate,PrivateKey> {

    /** The public key. */
    protected PublicKey publicKey;

    /**
     *
     * @param pKey
     * @param certificate
     * @param payload
     * @return signature
     * @throws JoseException
     */
    @Override
    public String jwsSign(String payload, PrivateKey pKey, X509Certificate certificate) {
        try {
            JsonWebSignature jws = new JsonWebSignature();
            List<X509Certificate> certList= new ArrayList<>();
            certList.add(certificate);
            X509Certificate[] certArray=certList.toArray(new X509Certificate[]{});
            jws.setCertificateChainHeaderValue(certArray);
            jws.setPayload(payload);
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
            jws.setKey(pKey);
            jws.setDoKeyValidation(false);
            return jws.getCompactSerialization();
        } catch (JoseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param sign
     * @return boolean
     */
    @Override
    public boolean verifySignature(String sign) {
        try {
            JsonWebSignature jws = new JsonWebSignature();
            jws.setCompactSerialization(sign);
            List<X509Certificate> certificateChainHeaderValue = jws.getCertificateChainHeaderValue();
            X509Certificate certificate = certificateChainHeaderValue.get(0);
            certificate.checkValidity();
            publicKey = certificate.getPublicKey();
            //certificate.verify(publicKey);
            jws.setKey(publicKey);
            return jws.verifySignature();
        } catch (CertificateException | JoseException e) {
            e.printStackTrace();
        }
        return false;

    }

}
