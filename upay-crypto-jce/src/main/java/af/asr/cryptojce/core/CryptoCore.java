package af.asr.cryptojce.core;


import java.security.InvalidAlgorithmParameterException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.util.Arrays;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PSource.PSpecified;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import af.asr.cryptojce.exception.common.NoSuchAlgorithmException;
import af.asr.cryptojce.exception.crypto.exception.InvalidDataException;
import af.asr.cryptojce.exception.crypto.exception.InvalidKeyException;
import af.asr.cryptojce.exception.crypto.exception.InvalidParamSpecException;
import af.asr.cryptojce.exception.crypto.exception.SignatureException;
import af.asr.cryptojce.exception.crypto.spi.CryptoCoreSpec;
import af.asr.cryptojce.util.CryptoUtil;
import af.asr.cryptojce.util.CryptoUtils;
import af.asr.cryptojce.util.EmptyCheckUtils;
import af.asr.cryptojce.util.SecurityExceptionCodeConstant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.Synchronized;

/**
 * This class provided <b> Basic and Core Cryptographic functionalities </b>.
 *
 * This class follows {@link CryptoCoreSpec} and implement all basic
 * Cryptographic functions.
 *
 * @author Urvil Joshi
 * @since 1.0.0
 *
 * @see CryptoCoreSpec
 * @see PrivateKey
 * @see PublicKey
 * @see Signature
 * @see SecretKey
 * @see Cipher
 * @see GCMParameterSpec
 * @see SecureRandom
 */
//Code optimization remaining (Code Dupe)
@Component
public class CryptoCore implements CryptoCoreSpec<byte[], byte[], SecretKey, PublicKey, PrivateKey, String> {

    // Used as a hack for softhsm oeap padding decryption usecase will be when we
    // will use in HSM
    private static final String RSA_ECB_NO_PADDING = "RSA/ECB/NoPadding";

    @Value("${kernel.keygenerator.asymmetric-key-length:2048}")
    private int asymmetricKeyLength;

    private static final String MGF1 = "MGF1";

    private static final String HASH_ALGO = "SHA-256";

    private static final String AES = "AES";

    @Value("${kernel.crypto.gcm-tag-length:128}")
    private int tagLength;

    @Value("${kernel.crypto.symmetric-algorithm-name:AES/GCM/PKCS5Padding}")
    private String symmetricAlgorithm;

    @Value("${kernel.crypto.asymmetric-algorithm-name:RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING}")
    private String asymmetricAlgorithm;

    @Value("${kernel.crypto.hash-algorithm-name:PBKDF2WithHmacSHA512}")
    private String passwordAlgorithm;

    @Value("${kernel.crypto.sign-algorithm-name:SHA512withRSA}")
    private String signAlgorithm;

    @Value("${kernel.crypto.hash-symmetric-key-length:256}")
    private int symmetricKeyLength;

    @Value("${kernel.crypto.hash-iteration:100000}")
    private int iterations;

    //private Map<String, Cipher> cipherRegistry;

    private SecureRandom secureRandom;

    private SecretKeyFactory secretKeyFactory;

    private Signature signature;

    @PostConstruct
    public void init() {
        //cipherRegistry = new ConcurrentHashMap<>();
		/*try {
			//cipherRegistry.put(symmetricAlgorithm, Cipher.getInstance(symmetricAlgorithm));
			//cipherRegistry.put(asymmetricAlgorithm, Cipher.getInstance(asymmetricAlgorithm));
			//cipherRegistry.put(RSA_ECB_NO_PADDING, Cipher.getInstance(RSA_ECB_NO_PADDING));
			secretKeyFactory = SecretKeyFactory.getInstance(passwordAlgorithm);
			signature = Signature.getInstance(signAlgorithm);

		} catch (java.security.NoSuchAlgorithmException e) {
			throw new NoSuchAlgorithmException(
					SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorCode(),
					SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorMessage(), e);
		}*/
        secureRandom = new SecureRandom();
    }


    @Override
    public byte[] symmetricEncrypt(SecretKey key, byte[] data, byte[] aad) {
        Objects.requireNonNull(key, SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorMessage());
        CryptoUtils.verifyData(data);
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(symmetricAlgorithm);
        } catch (java.security.NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new NoSuchAlgorithmException(
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorMessage(), e);
        }
        byte[] output = null;
        byte[] randomIV = generateIV(cipher.getBlockSize());
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), AES);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(tagLength, randomIV);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
            output = new byte[cipher.getOutputSize(data.length) + cipher.getBlockSize()];
            if (aad != null && aad.length != 0) {
                cipher.updateAAD(aad);
            }
            byte[] processData = doFinal(data, cipher);
            System.arraycopy(processData, 0, output, 0, processData.length);
            System.arraycopy(randomIV, 0, output, processData.length, randomIV.length);
        } catch (java.security.InvalidKeyException e) {
            throw new InvalidKeyException(SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorMessage(), e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_PARAM_SPEC_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_PARAM_SPEC_EXCEPTION.getErrorMessage(), e);
        }
        return output;
    }

    @Override
    public byte[] symmetricEncrypt(SecretKey key, byte[] data, byte[] iv, byte[] aad) {
        Objects.requireNonNull(key, SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorMessage());
        CryptoUtils.verifyData(data);
        if (iv == null) {
            symmetricEncrypt(key, data, aad);
        }
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(symmetricAlgorithm);
        } catch (java.security.NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new NoSuchAlgorithmException(
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorMessage(), e);
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), AES);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(tagLength, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
            if (aad != null && aad.length != 0) {
                cipher.updateAAD(aad);
            }
            return doFinal(data, cipher);
        } catch (java.security.InvalidKeyException e) {
            throw new InvalidKeyException(SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorMessage(), e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidParamSpecException(
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_PARAM_SPEC_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_PARAM_SPEC_EXCEPTION.getErrorMessage(), e);
        }
    }

    @Override
    public byte[] symmetricDecrypt(SecretKey key, byte[] data, byte[] aad) {
        Objects.requireNonNull(key, SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorMessage());
        CryptoUtils.verifyData(data);
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(symmetricAlgorithm);
        } catch (java.security.NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new NoSuchAlgorithmException(
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorMessage(), e);
        }
        byte[] output = null;
        try {
            byte[] randomIV = Arrays.copyOfRange(data, data.length - cipher.getBlockSize(), data.length);
            SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), AES);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(tagLength, randomIV);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
            if (aad != null && aad.length != 0) {
                cipher.updateAAD(aad);
            }
            output = doFinal(Arrays.copyOf(data, data.length - cipher.getBlockSize()), cipher);
        } catch (java.security.InvalidKeyException e) {
            throw new InvalidKeyException(SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorMessage(), e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_PARAM_SPEC_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_PARAM_SPEC_EXCEPTION.getErrorMessage(), e);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidDataException(
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_DATA_LENGTH_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_DATA_LENGTH_EXCEPTION.getErrorMessage(), e);
        }
        return output;
    }

    @Override
    public byte[] symmetricDecrypt(SecretKey key, byte[] data, byte[] iv, byte[] aad) {
        Objects.requireNonNull(key, SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorMessage());
        CryptoUtils.verifyData(data);
        if (iv == null) {
            symmetricDecrypt(key, data, aad);
        }
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(symmetricAlgorithm);
        } catch (java.security.NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new NoSuchAlgorithmException(
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorMessage(), e);
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), AES);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(tagLength, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);
            if (aad != null) {
                cipher.updateAAD(aad);
            }
            return doFinal(data, cipher);
        } catch (java.security.InvalidKeyException e) {
            throw new InvalidKeyException(SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorMessage(), e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidParamSpecException(
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_PARAM_SPEC_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_PARAM_SPEC_EXCEPTION.getErrorMessage(), e);
        }
    }

    @Override
    public byte[] asymmetricEncrypt(PublicKey key, byte[] data) {
        Objects.requireNonNull(key, SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorMessage());
        CryptoUtils.verifyData(data);
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(asymmetricAlgorithm);
        } catch (java.security.NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new NoSuchAlgorithmException(
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorMessage(), e);
        }
        final OAEPParameterSpec oaepParams = new OAEPParameterSpec(HASH_ALGO, MGF1, MGF1ParameterSpec.SHA256,
                PSpecified.DEFAULT);
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, oaepParams);
        } catch (java.security.InvalidKeyException e) {
            throw new InvalidKeyException(SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorCode(),
                    e.getMessage(), e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidParamSpecException(
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_PARAM_SPEC_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_PARAM_SPEC_EXCEPTION.getErrorMessage(), e);
        }
        return doFinal(data, cipher);
    }

    @Override
    public byte[] asymmetricDecrypt(PrivateKey key, byte[] data) {
        Objects.requireNonNull(key, SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorMessage());
        CryptoUtils.verifyData(data);
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(RSA_ECB_NO_PADDING);
        } catch (java.security.NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new NoSuchAlgorithmException(
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorMessage(), e);
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
        } catch (java.security.InvalidKeyException e) {
            throw new InvalidKeyException(SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorCode(),
                    e.getMessage(), e);
        }
        /*
         * This is a hack of removing OEAP padding after decryption with NO Padding as
         * SoftHSM does not support it.Will be removed after HSM implementation
         */
        byte[] paddedPlainText = doFinal(data, cipher);
        if (paddedPlainText.length < asymmetricKeyLength / 8) {
            byte[] tempPipe = new byte[asymmetricKeyLength / 8];
            System.arraycopy(paddedPlainText, 0, tempPipe, tempPipe.length - paddedPlainText.length,
                    paddedPlainText.length);
            paddedPlainText = tempPipe;
        }
        final OAEPParameterSpec oaepParams = new OAEPParameterSpec(HASH_ALGO, MGF1, MGF1ParameterSpec.SHA256,
                PSpecified.DEFAULT);
        return unpadOEAPPadding(paddedPlainText, oaepParams);

    }

    /*
     * This is a hack of removing OEAP padding after decryption with NO Padding as
     * SoftHSM does not support it.Will be removed after HSM implementation
     */
    @SuppressWarnings("restriction")
    private byte[] unpadOEAPPadding(byte[] paddedPlainText, OAEPParameterSpec paramSpec) {
        byte[] unpaddedData = null;
        try {
            sun.security.rsa.RSAPadding padding = sun.security.rsa.RSAPadding.getInstance(
                    sun.security.rsa.RSAPadding.PAD_OAEP_MGF1, asymmetricKeyLength / 8, new SecureRandom(), paramSpec);
            unpaddedData = padding.unpad(paddedPlainText);
        } catch (java.security.InvalidKeyException e) {
            throw new InvalidKeyException(SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorCode(),
                    e.getMessage(), e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidParamSpecException(
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_PARAM_SPEC_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_PARAM_SPEC_EXCEPTION.getErrorMessage(), e);
        } catch (BadPaddingException e) {
            throw new InvalidDataException(SecurityExceptionCodeConstant.PHOENIX_INVALID_DATA_EXCEPTION.getErrorCode(),
                    e.getMessage(), e);
        }
        return unpaddedData;
    }

    @Override
    public String hash(byte[] data, byte[] salt) {
        CryptoUtils.verifyData(data);
        CryptoUtils.verifyData(salt, SecurityExceptionCodeConstant.SALT_PROVIDED_IS_NULL_OR_EMPTY.getErrorCode(),
                SecurityExceptionCodeConstant.SALT_PROVIDED_IS_NULL_OR_EMPTY.getErrorMessage());
        char[] convertedData = new String(data).toCharArray();
        PBEKeySpec pbeKeySpec = new PBEKeySpec(convertedData, salt, iterations, symmetricKeyLength);
        SecretKey key;
        try {
            secretKeyFactory = SecretKeyFactory.getInstance(passwordAlgorithm);
            key = secretKeyFactory.generateSecret(pbeKeySpec);
        } catch (InvalidKeySpecException e) {
            throw new InvalidParamSpecException(
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_PARAM_SPEC_EXCEPTION.getErrorCode(), e.getMessage(), e);
        }
        catch (java.security.NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException(
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorMessage(), e);
        }
        return DatatypeConverter.printHexBinary(key.getEncoded());
    }

    @Override
    public String sign(byte[] data, PrivateKey privateKey) {
        Objects.requireNonNull(privateKey, SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorMessage());
        CryptoUtils.verifyData(data);
        try {
            signature = Signature.getInstance(signAlgorithm);
            signature.initSign(privateKey);
            signature.update(data);
            return CryptoUtil.encodeBase64String(signature.sign());
        } catch (java.security.InvalidKeyException e) {
            throw new InvalidKeyException(SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorCode(),
                    e.getMessage(), e);
        } catch (java.security.SignatureException e) {
            throw new SignatureException(SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorCode(),
                    e.getMessage(), e);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException(
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorMessage(), e);
        }
    }

    @Override
    public boolean verifySignature(byte[] data, String sign, PublicKey publicKey) {
        if (EmptyCheckUtils.isNullEmpty(sign)) {
            throw new SignatureException(SecurityExceptionCodeConstant.PHOENIX_SIGNATURE_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_SIGNATURE_EXCEPTION.getErrorMessage());
        }
        Objects.requireNonNull(publicKey, SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorMessage());
        CryptoUtils.verifyData(data);
        try {
            signature = Signature.getInstance(signAlgorithm);
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(CryptoUtil.decodeBase64(sign));
        } catch (java.security.InvalidKeyException e) {
            throw new InvalidKeyException(SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorCode(),
                    e.getMessage(), e);
        } catch (java.security.SignatureException e) {
            throw new SignatureException(SecurityExceptionCodeConstant.PHOENIX_INVALID_KEY_EXCEPTION.getErrorCode(),
                    e.getMessage(), e);
        }
        catch (java.security.NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException(
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorCode(),
                    SecurityExceptionCodeConstant.PHOENIX_NO_SUCH_ALGORITHM_EXCEPTION.getErrorMessage(), e);
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public SecureRandom random() {
        return secureRandom;
    }

    /**
     * Generator for IV(Initialisation Vector)
     *
     * @param blockSize blocksize of current cipher
     * @return generated IV
     */
    private byte[] generateIV(int blockSize) {
        byte[] byteIV = new byte[blockSize];
        secureRandom.nextBytes(byteIV);
        return byteIV;
    }

    private byte[] doFinal(byte[] data, Cipher cipher) {
        try {
            return cipher.doFinal(data);
        } catch (IllegalBlockSizeException e) {
            throw new InvalidDataException(
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_DATA_SIZE_EXCEPTION.getErrorCode(), e.getMessage(), e);
        } catch (BadPaddingException e) {
            throw new InvalidDataException(
                    SecurityExceptionCodeConstant.PHOENIX_INVALID_ENCRYPTED_DATA_CORRUPT_EXCEPTION.getErrorCode(),
                    e.getMessage(), e);
        }
    }
}