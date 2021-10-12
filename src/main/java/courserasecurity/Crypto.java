package courserasecurity;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {

	public static void GenerateKeyPair(UserData user, String password, SecretKey sk) {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			KeyPair pair = generator.generateKeyPair();
			
			PrivateKey privateKey = pair.getPrivate();
			PublicKey publicKey = pair.getPublic();
			
			
			
			String pubKeyB64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
			user.setPubKey(pubKeyB64);
			
			IvParameterSpec privKeyIv = generateAndStoreIv(user);
			
			String encPrivKey = encryptAES(privateKey.getEncoded(), sk, privKeyIv);
			user.setPrivKey(encPrivKey);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	public static SecretKey generateAESKey() {
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(256);
			SecretKey key = keyGenerator.generateKey();
			return key;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * generate the AES secret key from the user's password
	 * this key is used to encrypt/decrypt the user's private RSA key
	 * @param password user's password
	 * @param salt user's salt
	 * @return AES secret key
	 */
	public static SecretKey buildAESKeyFromPassword(String password, byte[] salt) {
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
			SecretKey secret = factory.generateSecret(spec);
			return new SecretKeySpec(secret.getEncoded(), "AES");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * generate a new IV
	 * set it to a certain object (e.g. Message or User) which
	 * will need to store it for a later decipher task
	 * @param ivTarget where the IV will be stored as B64 bytes
	 * @return IvParameterSpec for encryption
	 */
	public static IvParameterSpec generateAndStoreIv(IVClass ivTarget) {
	    byte[] iv = new byte[16];
	    
	    new SecureRandom().nextBytes(iv);
		ivTarget.setIv(enB64(iv));

	    return new IvParameterSpec(iv);
	}

	public static String encryptAES(byte[] plaintext, SecretKey key, IvParameterSpec iv) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, key, iv);
			byte[] cipherText = cipher.doFinal(plaintext);
			return enB64(cipherText);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String decrypt(String cipherText, SecretKey key, IvParameterSpec iv) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		    cipher.init(Cipher.DECRYPT_MODE, key, iv);
		    byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
		    return new String(Base64.getEncoder().encodeToString(plainText));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


	public static String encryptRSA(byte[] plainText, String pubKeyB64)
	{
		byte[] publicKeyBytes = deB64(pubKeyB64);
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes );
			PublicKey pubKey = keyFactory.generatePublic(publicKeySpec);
			
			Cipher encryptCipher = Cipher.getInstance("RSA");
			encryptCipher.init(Cipher.ENCRYPT_MODE, pubKey);
			byte[] cypherText = encryptCipher.doFinal(plainText);
			return enB64(cypherText);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String enB64(byte[] binaryText) {
		return Base64.getEncoder().encodeToString(binaryText);
	}

	private static byte[] deB64(String txtB64) {
		return Base64.getDecoder().decode(txtB64);
	}

	public static String DecryptPk(String cypherB64, String privateKeyB64) {
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			byte[] privKeyBytes = Base64.getDecoder().decode(privateKeyB64);
			EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privKeyBytes);
			// EncodedKeySpec privKeySpec = new X509EncodedKeySpec(privKeyBytes);
			PrivateKey privateKey = keyFactory.generatePrivate(privKeySpec);

			Cipher decryptCipher = Cipher.getInstance("RSA");

			decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
			byte[] decryptedMessageBytes = decryptCipher.doFinal(Base64.getDecoder().decode(cypherB64));
			return Base64.getEncoder().encodeToString(decryptedMessageBytes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static IvParameterSpec rebuildIv(String cryptIv) {
		byte[] ivBytes = deB64(cryptIv);
	    return new IvParameterSpec(ivBytes);
	}
}
