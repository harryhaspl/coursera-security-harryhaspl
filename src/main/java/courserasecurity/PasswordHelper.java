package courserasecurity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

public class PasswordHelper {
	
	public static byte[] encryptPassword(String password) {
		byte[] salt = generatePasswordSalt();
		byte[] pwd = new String(password).getBytes(StandardCharsets.UTF_8);

		byte[] hashedPassword = hashPassword(pwd, salt);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		try {
			outputStream.write( salt );
			outputStream.write( hashedPassword );
		} catch (IOException e) {
		}

		return outputStream.toByteArray();
	}
	
	public static byte[] generatePasswordSalt() {
		Random r = new SecureRandom();
		byte[] salt = new byte[16];
		r.nextBytes(salt);
		return salt;
	}

	
	public static byte[] hashPassword(byte[] password, byte[] salt) {
		System.out.println(
				"hashing password " + new String(password) + " with salt: " + Base64.getEncoder().encodeToString(salt));
		
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
			md.update(salt);
			md.update(password);
		} catch (NoSuchAlgorithmException e1) {
		}

		byte[] digest = md.digest(salt);
		System.out.println( "result: " + Base64.getEncoder().encodeToString(digest));
		return digest;
	}
	
	public static boolean checkPassword(String storedPassword, String providedPassword) {
		byte[] decodedBytes = deB64(storedPassword);
		byte[] salt = Arrays.copyOf(decodedBytes, 16);
		byte[] storedHash = Arrays.copyOfRange(decodedBytes, 16, decodedBytes.length);
		byte[] pwd = new String(providedPassword).getBytes(StandardCharsets.UTF_8);
		byte[] pwdHash = hashPassword(pwd, salt);
		if (pwdHash.length != storedHash.length)
			return false;
		for (int i = 0; i < pwdHash.length; i++) {
			if (pwdHash[i] != storedHash[i])
				return false;
		}
		return true;
	}

	public static byte[] getSaltFromPassString(String password) {
		byte[] decodedBytes = deB64(password);
		byte[] salt = Arrays.copyOf(decodedBytes, 16);
		return salt;
	}
	
	private static byte[] deB64(String txtB64) {
		return Base64.getDecoder().decode(txtB64);
	}

	
}
