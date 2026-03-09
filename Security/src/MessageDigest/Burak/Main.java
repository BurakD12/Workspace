package MessageDigest.Burak;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String password = "Mr. and Mrs. Brown went to the seaside.";
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hashedMessage = md.digest(password.getBytes("UTF-8"));

        StringBuilder sb = new StringBuilder();
        for (byte hash : hashedMessage) {
            sb.append(String.format("%02x", hash));
        }
        System.out.println("SHA-1 hash: " + sb);

        //=================================================================

        String name = "Burak";
        MessageDigest md2 = MessageDigest.getInstance("SHA-256");
        byte[] nameBytes = name.getBytes("UTF-8");
        byte[] passwordBytes = password.getBytes("UTF-8");

        md2.update(nameBytes);
        md2.update(passwordBytes);

        byte[] combinedHash = md2.digest();

        StringBuilder sb2 = new StringBuilder();
        for (byte hash : combinedHash) {
            sb2.append(String.format("%02x", hash));
        }
        System.out.println("SHA-256 hash: " + sb2);
    }
}
