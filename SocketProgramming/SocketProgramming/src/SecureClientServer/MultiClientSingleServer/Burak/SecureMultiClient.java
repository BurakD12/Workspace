package SecureClientServer.MultiClientSingleServer.Burak;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class SecureMultiClient {
    private static final String HOST = "localhost";
    private static final int PORT = 8888;
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int AES_KEY_SIZE = 256;
    private PublicKey pubKey;
    private SecretKey symmetricKey;
    private Socket socket;
    private boolean running = true;

    private PublicKey receivePublicKey() throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        int len = in.readInt();
        byte[] pubKeyBytes = new byte[len];
        in.readFully(pubKeyBytes);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(pubKeyBytes);
        try {
            KeyFactory factory = KeyFactory.getInstance(RSA_ALGORITHM);
            return factory.generatePublic(spec);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("No such algorithm could be found. " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (InvalidKeySpecException e) {
            System.err.println("Invalid key spec for receiving public key. " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean generateAndSendSymmetricKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGen.init(AES_KEY_SIZE);
            this.symmetricKey = keyGen.generateKey();
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] encryptedKey = cipher.doFinal(this.symmetricKey.getEncoded());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(encryptedKey.length);
            out.write(encryptedKey);
            out.flush();
            return true;
        } catch (Exception e) {
            System.err.println("Error while generating and sending symmetric key. " + e.getMessage());
            return false;
        }
    }

    private boolean sendEncryptedMessage(String message) {
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
            byte[] encryptedMessageBytes = cipher.doFinal(message.getBytes("UTF-8"));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(encryptedMessageBytes.length);
            out.write(encryptedMessageBytes);
            out.flush();
            return true;
        } catch (Exception e) {
            System.err.println("Error encrypting and sending message. " + e.getMessage());
            return false;
        }
    }

    private String receiveAndDecryptMessage() {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            int len = in.readInt();
            byte[] encrypted = new byte[len];
            in.readFully(encrypted);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, symmetricKey);
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, "UTF-8");
        } catch (Exception e) {
            System.err.println("Error receiving and decrypting message. " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void listenForMessages() {
        Thread listenThread = new Thread(() -> {
            while (running) {
                try {
                    String message = receiveAndDecryptMessage();
                    if (message == null) {
                        running = false;
                        break;
                    }
                    System.out.println("\n[Server]: " + message);
                    if (message.equalsIgnoreCase("exit")) {
                        System.out.println("Exiting the server per server request...");
                        running = false;
                    }
                } catch (Exception e) {
                    if (running) {
                        System.err.println("Error receiving the message. " + e.getMessage());
                    }
                }
            }
        });
        listenThread.setDaemon(true);
        listenThread.start();
    }

    public void connect() {
        try {
            System.out.println("Connecting to server at " + HOST + ":" + PORT + "...") ;
            socket = new Socket(HOST, PORT);
            System.out.println("Connected to server!");

            pubKey = receivePublicKey();
            if (pubKey == null) {
                System.out.println("Failed to receive the public key.");
                socket.close();
                return;
            }

            if (!generateAndSendSymmetricKey()) {
                System.out.println("Failed to generate and send the symmetric key.");
                socket.close();
                return;
            }

            System.out.println("Secure connection with the host established.");

            listenForMessages();

            Scanner scan = new Scanner(System.in);
            
            while (running) {
                try {    
                    System.out.print("\n[Client]:");
                    String message = scan.nextLine();
                    
                    if (message.trim().isEmpty() || message == null) {
                        continue;
                    }
                    
                    if (!sendEncryptedMessage(message)) {
                        System.out.println("Message couldn't be sent.");
                        running = false;
                        break;
                    }
                    
                    if (message.equalsIgnoreCase("exit")) {
                        System.out.println("Client requested to exit.");
                        System.out.println("Exiting server...");
                        running = false;
                    }
                } catch (Exception e) {
                    System.err.println("Error reading input: " + e.getMessage());
                }                
            }
        } catch (UnknownHostException e) {
            System.err.println("Error connecting to host: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally{
            try {
                if (!socket.isClosed() && socket != null) {
                    socket.close();
                    System.out.println("Socket closed.");
                }
            } catch (Exception e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
        
    }

    public static void main(String[] args) {
        SecureMultiClient secureMultiClient = new SecureMultiClient();
        secureMultiClient.connect();
    }
}
