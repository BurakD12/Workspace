package AsymmetricEncryption;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.io.File;

public class KeyGenerator {

    static public void generateKeyPair() throws NoSuchAlgorithmException, IOException {
        // Step 1: Initialize KeyPairGenerator for RSA algorithm
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
        
        // Step 2: Create a SecureRandom instance
        SecureRandom secureRandom = new SecureRandom();

        // Step 3: Initialize the KeyPairGenerator with key size and SecureRandom
        keygen.initialize(1024, secureRandom);
        
        // Step 4: Generate the KeyPair
        KeyPair pair = keygen.generateKeyPair();

        // Step 5: Retrieve Public and Private Keys
        PublicKey publicKey = pair.getPublic();
        PrivateKey privateKey = pair.getPrivate();

        writeKeysToFile(publicKey.getEncoded(), "src/AsymmetricEncryption/KeyStore/public.key");
        writeKeysToFile(privateKey.getEncoded(), "src/AsymmetricEncryption/KeyStore/private.key");
    }

    static private void writeKeysToFile(byte[] keyBytes, String filePath) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs(); // Ensure the directory exists

        FileOutputStream fos = new FileOutputStream(file);

        fos.write(keyBytes);
        fos.flush();
        fos.close();
    }

    public static void main(String[] args) {
        try {
            generateKeyPair();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }
}