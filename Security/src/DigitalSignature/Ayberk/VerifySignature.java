package DigitalSignature.Ayberk;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

public class VerifySignature {

    public static void main(String[] args) throws Exception {
        List<byte[]> list;
        ObjectInputStream in = new ObjectInputStream(new FileInputStream("src/DigitalSignature/Ayberk/SignStore/SignedData.txt"));
        list = (List<byte[]>) in.readObject();
        in.close();

        byte[] msg = list.get(0);
        byte[] signature = list.get(1);

        // Retrieve the public key from the file
        byte[] keyBytes = Files.readAllBytes(new File("src/AsymmetricEncryption/KeyStore/public.key").toPath());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(spec);

        // Initialize the signature with the public key and verify the signature
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(msg);

        boolean isVerified = sig.verify(signature);
        if (isVerified) {
            System.out.println("Signature is valid.");
        } else {
            System.out.println("Signature is invalid.");
        }
    }
}