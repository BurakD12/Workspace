package DigitalSignature.Burak;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import AsymmetricEncryption.KeyGenerator;

public class VerifySignature {
    public static void main(String[] args) throws Exception, IOException {
        List<byte[]> list;
        ObjectInputStream in = new ObjectInputStream(new FileInputStream("src/DigitalSignature/Burak/SignStore/SignedData.txt"));
        list = (List<byte[]>) in.readObject();
        in.close();

        byte[] messageBytes = list.get(0);
        byte[] signature = list.get(1);

        byte[] keyBytes = Files.readAllBytes(new File("src/AsymmetricEncryption/KeyStore/public.key").toPath());
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(messageBytes);
        
        if (sig.verify(signature)) {
            System.out.println("Signature successfully verified.");
        } else {
            System.out.println("Signature could not be verified.");
        }
    }
}
