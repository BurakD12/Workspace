package DigitalSignature.Burak;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

public class DigitalSignature {
    public static void main(String[] args) throws Exception {
        String data = "Hello world.";

        List<byte[]> list = new ArrayList<>();

        list.add(data.getBytes());
        Signature sig = Signature.getInstance("SHA256withRSA");

        byte[] keyBytes = Files.readAllBytes(new File("src/AsymmetricEncryption/KeyStore/private.key").toPath());
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        sig.initSign(privateKey);
        sig.update(data.getBytes());
        list.add(sig.sign());


        File f = new File("src/DigitalSignature/Burak/SignStore/SignedData.txt");
        f.getParentFile().mkdirs();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
        oos.writeObject(list);
        oos.close();
        System.out.println("Data and signature have been written to the file.");
    }
}
