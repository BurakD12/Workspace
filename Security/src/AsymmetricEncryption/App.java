package AsymmetricEncryption;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class App {
    static String getByteString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
    
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        // Step-1: Create a KeyFactory for RSA
        KeyFactory kf = KeyFactory.getInstance("RSA");

        // Step-2: Read the private key bytes from file
        byte[] keyBytes = Files.readAllBytes(new File("src/AsymmetricEncryption/KeyStore/private.key").toPath());
        // Step-3: Generate PrivateKeySpec from the key bytes
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        // Step-4: Generate PrivateKey from the KeyFactory and PrivateKeySpec
        PrivateKey privateKey = kf.generatePrivate(spec);
        System.out.println("Built Private Key!");

        // Step-5: Read the public key bytes from file
        keyBytes = Files.readAllBytes(new File("src/AsymmetricEncryption/KeyStore/public.key").toPath());
        // Step-6: Generate PublicKeySpec from the key bytes
        X509EncodedKeySpec spec2 = new X509EncodedKeySpec(keyBytes);
        // Step-7: Generate PublicKey from the KeyFactory and PublicKeySpec
        PublicKey publicKey = kf.generatePublic(spec2);
        System.out.println("Built Public Key!");

        String message = "Hello World! This is Asymmetric Encryption in Java.";
        byte[] messageBytes = message.getBytes();
        
        System.out.println("Original Message: " + message);
        System.out.println("Message Bytes: " + getByteString(messageBytes));

        // Step-8: Encrypt the message
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] encodedBytes = cipher.doFinal(messageBytes);

        // Step-9: Print the encrypted message and bytes
        System.out.println("Encrypted message: " + new String(encodedBytes));
        System.out.println("Encrypted message bytes: " + getByteString(encodedBytes));

        // Step-10: Decrypt the message
        
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decodedBytes = cipher.doFinal(encodedBytes);
        
        // Step-11: Print the decrypted message and bytes
        System.out.println("Decrypted message: " + new String(decodedBytes));
        System.out.println("Decrypted message bytes: " + getByteString(decodedBytes));
    }
}
