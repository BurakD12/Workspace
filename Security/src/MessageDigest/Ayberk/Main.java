package MessageDigest.Ayberk;

import java.security.MessageDigest;

public class Main {

    public static void main(String[] args) throws Exception {
        String password = "The quick brown fox jumps over the lazy dog";
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(password.getBytes("UTF-8")); // Hash the password using SHA-1 (MessageDigest)

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b)); // Convert each byte to a hexadecimal string
        }
        System.out.println("SHA-1 Hash: " + hexString.toString()); // Print the resulting hash

        // =============================================================================================

        String name = "Ayberk";
        byte[] data1 = name.getBytes("UTF-8"); // Convert the name to bytes 
        byte[] data2 = password.getBytes("UTF-8"); // Convert the password to bytes

        MessageDigest md2 = MessageDigest.getInstance("SHA-256");
        md2.update(data1); // Update the MessageDigest with the name bytes
        md2.update(data2); // Update the MessageDigest with the password bytes
        byte[] combinedHash = md2.digest(); // Compute the combined hash    

        StringBuilder combinedHexString = new StringBuilder();
        for (byte b : combinedHash) {
            combinedHexString.append(String.format("%02x", b)); // Convert each byte of the combined hash to a hexadecimal string
        }
        System.out.println("Combined SHA-256 Hash: " + combinedHexString.toString()); // Print the resulting combined hash        
    }
}