package Secure_Multi_Client_Chat_App;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

import javax.crypto.SecretKey;

public class Client {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;
    private KeyPair keyPair;
    private Map<String, PublicKey> peerPublicKeys = new ConcurrentHashMap<>();

    public void connect(String serverHost, int serverPort, String username) throws Exception {
        socket = new Socket(serverHost, serverPort);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        out.flush();
        System.out.println("[Client] Connected to server at " + serverHost + ":" + serverPort);

        this.username = username;
        keyPair = CryptoUtils.generateRSAKeyPair();
        System.out.println("[Client] RSA Key Pair generated.");

        KeyExchangeMessage keyExchangeMsg = new KeyExchangeMessage(username, keyPair.getPublic());
        sendToServer(keyExchangeMsg);
        System.out.println("[Client] Key exchange message sent to server.");

        ClientReceiver receiver = new ClientReceiver(in, this);
        Thread receiverThread = new Thread(receiver);
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    public void broadcast(String plaintext) {
        try {
            if (peerPublicKeys.isEmpty()) {
                System.out.println("[Client] No peers available to send the message.");
                return;
            }

            // 1. Generate a fresh AES key
            SecretKey aesKey = CryptoUtils.generateAESKey();

            // 2. Encrypt plaintext with AES key
            byte[] ciphertext = CryptoUtils.encryptAES(plaintext.getBytes(), aesKey);

            // 3. Compute SHA-256 digest of plaintext
            byte[] digest = CryptoUtils.computeDigest(plaintext.getBytes());

            // 4. Sign plaintext with sender's RSA private key
            byte[] signature = CryptoUtils.sign(plaintext.getBytes(), keyPair.getPrivate());

            // 5. Wrap AES key for each peer
            Map<String, byte[]> encryptedKeys = new HashMap<>();
            for (Entry<String, PublicKey> entry : peerPublicKeys.entrySet()) {
                PublicKey peerPublicKey = entry.getValue();
                // Wrap AES key with peer's RSA public key
                byte[] wrappedKey = CryptoUtils.wrapAESKey(aesKey, peerPublicKey);
                encryptedKeys.put(entry.getKey(), wrappedKey);
            }

            // 6. Build and send the SecureMessage
            SecureMessage secureMessage = new SecureMessage(
                    MessageType.BROADCAST,
                    this.username, null, ciphertext,
                    encryptedKeys, digest, signature);

            sendToServer(secureMessage);
            System.out.println("[You (Broadcast)] " + plaintext);
        } catch (Exception e) {
            System.out.println("[Client] Error while encrypting message.");
        }
    }

    public void whisper(String targetUsername, String plaintext) {
        try {
            if (peerPublicKeys.isEmpty()) {
                System.out.println("[Client] No peers available to send the message.");
                return;
            }

            if (!peerPublicKeys.containsKey(targetUsername)) {
                System.out.println("[Client] Target not found.");
                return;
            }

            PublicKey targetPublicKey = peerPublicKeys.get(targetUsername);

            // 1. Generate a fresh AES key
            SecretKey aesKey = CryptoUtils.generateAESKey();

            // 2. Encrypt plaintext with AES key
            byte[] encryptedMessageBytes = CryptoUtils.encryptAES(plaintext.getBytes(), aesKey);

            // 3. Compute SHA-256 digest of plaintext
            byte[] digest = CryptoUtils.computeDigest(plaintext.getBytes());

            // 4. Sign plaintext with sender's RSA private key
            byte[] signature = CryptoUtils.sign(plaintext.getBytes(), this.keyPair.getPrivate());

            // 5. Wrap AES key for the target peer
            Map<String, byte[]> encryptedKeys = new HashMap<>();
            byte[] wrappedKey = CryptoUtils.wrapAESKey(aesKey, targetPublicKey);
            encryptedKeys.put(targetUsername, wrappedKey);

            SecureMessage secureMessage = new SecureMessage(MessageType.WHISPER,
                    this.username, targetUsername, encryptedMessageBytes,
                    encryptedKeys, digest, signature);

            sendToServer(secureMessage);
            System.out.println("[You (Whisper to " + targetUsername + ")] " + plaintext);
        } catch (Exception e) {
            System.out.println("[Client] Error while encrypting message.");
        }
    }

    public void receiveSecureMessage(SecureMessage msg) {
        // 1. Get the encrypted bytes for the symmetric key
        byte[] encryptAESBytes = msg.getEncryptedKeysFor(this.username);
        if (encryptAESBytes == null) {
            System.out.println("[Client] No encrypted key slot found for this client. Discarding message.");
            return;
        }
        
        try {
            // 2. Unwrap the symmetric key using the private key
            SecretKey aesKey = CryptoUtils.unwrapAESKey(encryptAESBytes, this.keyPair.getPrivate());

            // 3. Decrypt ciphertext
            byte[] plaintextBytes = CryptoUtils.decryptAES(msg.getCiphertext(), aesKey);

            // 4. Verify digest
            if (!CryptoUtils.verifyDigest(plaintextBytes, msg.getDigest())) {
                System.out.println("[Client] Message digest could not be verified.");
                return;
            }

            // 5. Verify digital signature
            PublicKey pubKey = peerPublicKeys.get(msg.getSenderUsername());
            if (pubKey == null) {
                System.out.println("[WARNING] No public key found for sender '"
                        + msg.getSenderUsername() + "'. Cannot verify authenticity. Message discarded.");
                return;
            }
            if (!CryptoUtils.verify(plaintextBytes, msg.getDigitalSignature(), pubKey)) {
                System.out.println("[WARNING] Signature verification FAILED for message from "
                        + msg.getSenderUsername() + ". Message discarded.");
                return;
            }
            
            // 6. Displaying the message
            String message = new String(plaintextBytes);

            if (msg.getType().equals(MessageType.BROADCAST)) {
                System.out.println("[Broadcast from " + msg.getSenderUsername() + "] " + message);
            } else {
                System.out.println("[Whisper from " + msg.getSenderUsername() + "] " + message);
            }
        } catch (Exception e) {
            System.out.println("[Client] Error while decrypting message.");
        }
    }

    public void receiveKeyDistribution(KeyDistributionMessage msg) {
        // 1. Store the key in peerPublicKeys
        try {
            peerPublicKeys.put(msg.getUsername(), msg.getPublicKey());
            System.out.println("[Client] The user: " + msg.getUsername() + " has been added as a peer.");
        } catch (Exception e) {
            System.out.println("[Client] Error storing the public key for the new user: " + msg.getUsername());
        }
    }

    public void sendToServer(Object obj) throws Exception {
        out.writeObject(obj);
        out.flush();
    }

    public void disconnect() {
        try {
            if (!socket.isClosed() && socket != null) {
                in.close();
                out.close();        
                socket.close();
            }
        } catch (IOException e) {
                System.out.println("[Client] Error disconnecting from server safely.");
        }
    }

    public static void main(String[] args) throws Exception {
        Client client = new Client();
        Scanner scan = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String username = scan.nextLine();
        client.connect("localhost", 12345, username);

        System.out.println("[Options]\n1. Type: '/w' to whisper to another user.\n2. Type: '/b' to broadcast a message to all current users.\n3. type '/q' to quit the chat.");
        while (true) {
            
            System.out.print("[Client] ");
            String selection = scan.nextLine();
            
            switch (selection) {
                case "/w" -> {
                    System.out.print("\nEnter the username of the user you want to whisper to: ");
                    String targetUsername = scan.nextLine();
                    System.out.print("\nEnter your message: ");
                    String plaintext = scan.nextLine();

                    client.whisper(targetUsername, plaintext);
                }
                case "/b" -> {
                    System.out.print("\nEnter your message: ");
                    String plaintext = scan.nextLine();

                    client.broadcast(plaintext);
                }
                case "/q" -> {
                    System.out.println("Exiting the chat...");
                    client.disconnect();
                    break;
                }
                default -> {
                    System.out.println("Request not possible please choose from the existing options.");
                }
                    
            }
        }
    }
}

