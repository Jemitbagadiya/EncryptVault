package security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

// Utility class for AES encryption and decryption using user-specific key
public class AESUtil {

    // Encryption algorithm used
    private static final String ALGORITHM = "AES";

    // Converts Base64 encoded key string into SecretKeySpec
    private static SecretKeySpec getKeyFromString(String base64Key) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(decodedKey, ALGORITHM);
    }

    // Encrypts input file and writes encrypted data to output file
    public static void encryptFile(File inputFile, File outputFile, String base64Key) throws Exception {

        // Initialize encryption cipher with user key
        SecretKeySpec secretKey = getKeyFromString(base64Key);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        FileInputStream fis = new FileInputStream(inputFile);
        FileOutputStream fos = new FileOutputStream(outputFile);

        byte[] buffer = new byte[4096];
        int bytesRead;

        // Read file in chunks and encrypt
        while ((bytesRead = fis.read(buffer)) != -1) {
            byte[] output = cipher.update(buffer, 0, bytesRead);
            if (output != null) {
                fos.write(output);
            }
        }

        // Final encryption block
        byte[] finalBytes = cipher.doFinal();
        if (finalBytes != null) {
            fos.write(finalBytes);
        }

        fis.close();
        fos.close();
    }

    // Decrypts encrypted file and writes original data to output file
    public static void decryptFile(File inputFile, File outputFile, String base64Key) throws Exception {

        // Initialize decryption cipher with user key
        SecretKeySpec secretKey = getKeyFromString(base64Key);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        FileInputStream fis = new FileInputStream(inputFile);
        FileOutputStream fos = new FileOutputStream(outputFile);

        byte[] buffer = new byte[4096];
        int bytesRead;

        // Read encrypted file in chunks and decrypt
        while ((bytesRead = fis.read(buffer)) != -1) {
            byte[] output = cipher.update(buffer, 0, bytesRead);
            if (output != null) {
                fos.write(output);
            }
        }

        // Final decryption block
        byte[] finalBytes = cipher.doFinal();
        if (finalBytes != null) {
            fos.write(finalBytes);
        }

        fis.close();
        fos.close();
    }
}