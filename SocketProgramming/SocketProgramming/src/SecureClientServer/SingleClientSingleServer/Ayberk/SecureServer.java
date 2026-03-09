package SecureClientServer.SingleClientSingleServer.Ayberk;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SecureServer {
    private static final int PORT = 12345;
    private static final String HOST = "localhost";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int RSA_KEY_SIZE = 2048;
    private static final int AES_KEY_SIZE = 256;

    private KeyPair keyPair;
    private ServerSocket serverSocket;

    public SecureServer() {
        generateKeyPair();
    }

    private void generateKeyPair() {
        try {
            System.out.println("Generating RSA key pair...");
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyGen.initialize(RSA_KEY_SIZE);
            this.keyPair = keyGen.generateKeyPair();
            System.out.println("RSA key pair generated.");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error generating RSA key pair: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Send public key to client
    private void sendPublicKey(Socket socket) throws IOException {
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // Send length of the public key
        out.writeInt(publicKeyBytes.length);

        // Send the public key bytes
        out.write(publicKeyBytes);
        out.flush();
        System.out.println("Sent public key (" + publicKeyBytes.length + " bytes) to client.");
    }

    // Receive and decrypt the symmetric key from client
    private SecretKey receiveAndDecryptSymmetricKey(Socket clientSocket) throws IOException {
        DataInputStream in = new DataInputStream(clientSocket.getInputStream());

        // Receive length of the encrypted symmetric key
        int encryptedKeyLength = in.readInt();

        // Receive the encrypted symmetric key bytes
        byte[] encryptedSymmetricKey = new byte[encryptedKeyLength];
        in.readFully(encryptedSymmetricKey);

        System.out.println("Received encrypted symmetric key (" + encryptedKeyLength + " bytes) from client.");

        // Decrypt the symmetric key using RSA private key
        try {
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decryptedKeyBytes = cipher.doFinal(encryptedSymmetricKey);

            // Create SecretKey from decrypted bytes
            SecretKey symmetricKey = new SecretKeySpec(decryptedKeyBytes, AES_ALGORITHM);
            System.out.println("Successfully decrypted symmetric key.");
            return symmetricKey;
        } catch (Exception e) {
            System.err.println("Error decrypting symmetric key: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Receive and decrypt message from client
    private String receiveAndDecryptMessage(Socket clientSocket, SecretKey symmetricKey) throws IOException {
        DataInputStream in = new DataInputStream(clientSocket.getInputStream());

        // Receive length of the encrypted message
        int encryptedMessageLength = in.readInt();

        // Receive the encrypted message bytes
        byte[] encryptedMessageBytes = new byte[encryptedMessageLength];
        in.readFully(encryptedMessageBytes);

        // Decrypt the message using AES symmetric key
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, symmetricKey);
            byte[] decryptedMessageBytes = cipher.doFinal(encryptedMessageBytes);
            String decryptedMessage = new String(decryptedMessageBytes);
            System.out.println("Received encrypted message (" + encryptedMessageLength + " bytes) from client.");
            System.out.println("Decrypted message: " + decryptedMessage);
            return decryptedMessage;
        } catch (Exception e) {
            System.err.println("Error decrypting message: " + e.getMessage());
            e.printStackTrace();
            return null;    
        }
    }
    
    private boolean sendEncryptedMessage(Socket clientSocket, SecretKey symmetricKey, String message) throws IOException {
        try {
            // Encrypt the message using AES symmetric key
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
            byte[] encryptedMessageBytes = cipher.doFinal(message.getBytes());

            // Send length of the encrypted message
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            out.writeInt(encryptedMessageBytes.length);

            // Send the encrypted message bytes
            out.write(encryptedMessageBytes);
            out.flush();
            return true;
        } catch (Exception e) {
            System.err.println("Error encrypting/sending message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void start() {
        try {

            // Create server socket
            serverSocket = new ServerSocket(PORT);
            System.out.println("Secure server started on " + HOST + ":" + PORT);

            while (true) {
                // Accept client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                try {
                    // Send public key to client
                    sendPublicKey(clientSocket);

                    // Receive and decrypt symmetric key from client
                    SecretKey symmetricKey = receiveAndDecryptSymmetricKey(clientSocket);
                    if (symmetricKey == null) {
                        System.err.println("Failed to receive/decrypt symmetric key. Closing connection.");
                        clientSocket.close();
                        continue;
                    }

                    // Communication loop
                    System.out.println("\nSecure communication established. Type 'quit' to end the session.");
                    BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

                    while (true) {
                        // Receive and decrypt message from client
                        String clientMessage = receiveAndDecryptMessage(clientSocket, symmetricKey);
                        if (clientMessage == null) {
                            System.err.println("Failed to receive/decrypt message. Closing connection.");
                            break;
                        }
                        
                        System.out.println("\n[Client]: " + clientMessage);

                        // Check for quit command
                        if (clientMessage.toLowerCase().equals("quit")) {
                            System.out.println("Client requested to end the session. Closing connection.");
                            break;
                        }

                        // Send response back to client
                        System.out.print("[Server]: ");
                        String serverResponse = consoleReader.readLine();
                        if (serverResponse == null || serverResponse.toLowerCase().equals("quit")) {
                            sendEncryptedMessage(clientSocket, symmetricKey, serverResponse != null ? serverResponse : "Server is closing the connection.");
                            break;
                        }

                        sendEncryptedMessage(clientSocket, symmetricKey, serverResponse);
                    }
                } catch (IOException e) {
                    System.err.println("Error during communication with client: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    // Close client connection
                    try {
                        clientSocket.close();
                        System.out.println("Client connection closed.");
                    } catch (IOException e) {
                        System.err.println("Error closing client socket: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
            return;
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                    System.out.println("Server socket closed.");
                }
            } catch (Exception e) {
                System.out.println("Error closing server socket: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SecureServer server = new SecureServer();
        server.start();
    }
}