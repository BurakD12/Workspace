package SymmetricEncryption;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class App {

    static String printBytes(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        String data = "Hello, World! This is a secret message.";
        System.out.println("Original Data: " + data);
        System.out.println("Bytes: " + printBytes(data.getBytes())); 
        
        // Step-1: Generate a Key Generator for AES algorithm
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        
        // Step-2: Initialize the Key Generator with a key size (e.g., 256 bits)
        keygen.init(256);

        // Step-3: Generate the Secret Key
        SecretKey secretKey = keygen.generateKey();

        // Step-4: Create an instance of Cipher class for AES algorithm
        Cipher cipher = Cipher.getInstance("AES");

        // Step-5: Initialize the Cipher in ENCRYPT_MODE with the generated Secret Key
    
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // Step-6: Encrypt the data
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        String encryptedString = new String(encryptedData);

        // Step-7: Print the encrypted data as bytes and as a string
        System.out.println("Encrypted Data: " + encryptedString);
        System.out.println("Encrypted Bytes: " + printBytes(encryptedData));

        // Step-8: Initialize the Cipher in DECRYPT_MODE with the same Secret Key
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // Step-9: Decrypt the data
        byte[] decryptedData = cipher.doFinal(encryptedData);
        String decryptedString = new String(decryptedData);

        // Step-10: Print the decrypted data as bytes and as a string
        System.out.println("Decrypted Data: " + decryptedString);
        System.out.println("Decrypted Bytes: " + printBytes(decryptedData));
    }
}
