package SecureClientServer.SingleClientSingleServer.Ayberk;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

/**
 * Secure Socket Client Application
 * Receives public key from server, generates symmetric key, encrypts it with RSA, and uses it for secure communication.
 */
public class SecureClient {
    private static final int PORT = 12345;
    private static final String HOST = "localhost";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int AES_KEY_SIZE = 256; // bits
    
    private PublicKey publicKey;
    private SecretKey symmetricKey;
    private Socket socket;
    private boolean running = true;
    
    /**
     * Receive public key from server
     */
    private PublicKey receivePublicKey() throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        
        // Receive length of public key (4 bytes)
        int publicKeyLength = in.readInt();
        
        // Receive public key
        byte[] publicKeyBytes = new byte[publicKeyLength];
        in.readFully(publicKeyBytes);
        
        System.out.println("Received public key (" + publicKeyLength + " bytes)");
        
        // Deserialize public key
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PublicKey pubKey = keyFactory.generatePublic(keySpec);
            System.out.println("Public key loaded successfully!");
            return pubKey;
        } catch (Exception e) {
            System.err.println("Error loading public key: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Generate symmetric key, encrypt it with public key, and send to server
     */
    private boolean generateAndSendSymmetricKey() {
        try {
            // Generate symmetric key
            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGen.init(AES_KEY_SIZE);
            this.symmetricKey = keyGen.generateKey();
            System.out.println("Generated symmetric key");
            
            // Encrypt symmetric key with public key
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedKey = cipher.doFinal(symmetricKey.getEncoded());
            
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            
            // Send length of encrypted symmetric key (4 bytes)
            out.writeInt(encryptedKey.length);
            
            // Send encrypted symmetric key
            out.write(encryptedKey);
            out.flush();
            System.out.println("Sent encrypted symmetric key (" + encryptedKey.length + " bytes) to server");
            return true;
        } catch (Exception e) {
            System.err.println("Error generating/sending symmetric key: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Encrypt and send message to server
     */
    private boolean sendEncryptedMessage(String message) {
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
            byte[] encryptedMessage = cipher.doFinal(message.getBytes("UTF-8"));
            
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            
            // Send length of encrypted message (4 bytes)
            out.writeInt(encryptedMessage.length);
            
            // Send encrypted message
            out.write(encryptedMessage);
            out.flush();
            return true;
        } catch (Exception e) {
            System.err.println("Error encrypting/sending message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Receive and decrypt message from server
     */
    private String receiveAndDecryptMessage() {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            
            // Receive length of encrypted message (4 bytes)
            int encryptedMessageLength = in.readInt();
            
            // Receive encrypted message
            byte[] encryptedMessage = new byte[encryptedMessageLength];
            in.readFully(encryptedMessage);
            
            // Decrypt message using symmetric key
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, symmetricKey);
            byte[] decryptedBytes = cipher.doFinal(encryptedMessage);
            return new String(decryptedBytes, "UTF-8");
        } catch (IOException e) {
            if (running) {
                System.err.println("Error receiving message: " + e.getMessage());
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error decrypting message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Listen for incoming messages from server in a separate thread
     */
    private void listenForMessages() {
        Thread listenThread = new Thread(() -> {
            while (running) {
                try {
                    String message = receiveAndDecryptMessage();
                    if (message == null) {
                        if (running) {
                            System.out.println("\nServer disconnected");
                        }
                        break;
                    }
                    System.out.println("\n[Server]: " + message);
                    
                    if (message.toLowerCase().equals("quit")) {
                        System.out.println("Server requested to quit");
                        running = false;
                    }
                } catch (Exception e) {
                    if (running) {
                        System.err.println("\nError receiving message: " + e.getMessage());
                    }
                }
            }
        });
        listenThread.setDaemon(true);
        listenThread.start();
    }
    
    /**
     * Connect to server and establish secure communication
     */
    public void connect() {
        try {
            // Create socket and connect
            System.out.println("Connecting to server at " + HOST + ":" + PORT + "...");
            socket = new Socket(HOST, PORT);
            System.out.println("Connected to server!");
            
            // Receive public key from server
            publicKey = receivePublicKey();
            if (publicKey == null) {
                System.out.println("Failed to receive public key");
                socket.close();
                return;
            }
            
            // Generate and send encrypted symmetric key
            if (!generateAndSendSymmetricKey()) {
                System.out.println("Failed to send symmetric key");
                socket.close();
                return;
            }
            
            System.out.println("\nSecure communication established!");
            System.out.println("You can now send messages. Type 'quit' to exit.\n");
            
            // Start thread to listen for server messages
            listenForMessages();
            
            // Main loop for sending messages
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            
            while (running) {
                try {
                    System.out.print("[Client]: ");
                    String message = consoleReader.readLine();
                    if (message == null || message.trim().isEmpty()) {
                        continue;
                    }
                    
                    if (!sendEncryptedMessage(message)) {
                        System.out.println("Failed to send message");
                        break;
                    }
                    
                    if (message.toLowerCase().equals("quit")) {
                        System.out.println("Disconnecting...");
                        running = false;
                    }
                    
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error reading input: " + e.getMessage());
                    }
                    break;
                }
            }
            
        } catch (ConnectException e) {
            System.err.println("Could not connect to server at " + HOST + ":" + PORT);
            System.err.println("Make sure the server is running.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            running = false;
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    System.out.println("Connection closed");
                }
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }
    
    public static void main(String[] args) {
        SecureClient client = new SecureClient();
        client.connect();
    }
}
