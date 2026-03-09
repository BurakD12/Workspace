package DigitalSignature.Ayberk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

public class DigitalSignature {

    public static void main(String[] args) throws Exception {
        String data = "Hello World!";

        // The list consist of the message and the signature
        List<byte[]> list = new ArrayList<>();

        // 1. Create a Signature object for signing the data
        Signature signature = Signature.getInstance("SHA256withRSA");
        list.add(data.getBytes()); // The message is added to the list

        // 2. Retrieving the private key
        byte[] keyBytes =  Files.readAllBytes(new File("src/AsymmetricEncryption/KeyStore/private.key").toPath());
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        // 3. Sign the data using the private key
        signature.initSign(privateKey);
        signature.update(data.getBytes());
        byte[] digitalSignature = signature.sign();
        list.add(digitalSignature); // The signature is added to the list

        // 4. Writing the message and the signature to a file
        File f = new File("src/DigitalSignature/Ayberk/SignStore/SignedData.txt");
        f.getParentFile().mkdirs(); // Create the directory if it doesn't exist
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
        oos.writeObject(list);
        oos.close();
        System.out.println("Data and signature have been written to the file.");
    }
}