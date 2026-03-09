package SecureClientServer.SingleClientSingleServer.Burak;

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
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
    private static final int AES_KEY_SIZE = 256;
    private static final int RSA_KEY_SIZE = 2048;

    private KeyPair keyPair;
    private ServerSocket serverSocket;

    public SecureServer(){
        generateKeyPair();
    }

    private void generateKeyPair() {
        try {
            System.out.println("Generating RSA keypair...");
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyGen.initialize(RSA_KEY_SIZE);
            this.keyPair = keyGen.generateKeyPair();
            System.out.println("KeyPair has been generated.");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("KeyPair couldn't be generated: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendPublicKey(Socket clientSocket) throws IOException{
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        DataOutputStream clientOutputStream = new DataOutputStream(clientSocket.getOutputStream());

        clientOutputStream.writeInt(publicKeyBytes.length);
        clientOutputStream.write(publicKeyBytes);
        clientOutputStream.flush();
        System.out.println("Sent public key (" + publicKeyBytes.length + " bytes) to client.");
    }

    private SecretKey receiveAndDecryptSymmetricKey(Socket clientSocket) throws IOException {
        DataInputStream clientInputStream = new DataInputStream(clientSocket.getInputStream());
        int encryptedKeyLength = clientInputStream.readInt();
        byte[] encryptedSymmetricKey = new byte[encryptedKeyLength];
        clientInputStream.readFully(encryptedSymmetricKey);

        System.out.println("Received encrypted symmetric key (" + encryptedKeyLength + " bytes) from client.");

        try {
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decryptedKeyBytes = cipher.doFinal(encryptedSymmetricKey);

            SecretKey symmetricKey = new SecretKeySpec(decryptedKeyBytes, AES_ALGORITHM);
            System.out.println("Successfully decrypted symmetric key.");
            return symmetricKey;
        } catch (Exception e) {
            System.err.println("Error decrypting symmetric key: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String receiveAndDecryptMessage(Socket clientSocket, SecretKey symmetricKey) throws IOException {
        DataInputStream clientInputStream = new DataInputStream(clientSocket.getInputStream());
        int encryptedMessageLen = clientInputStream.readInt();
        byte[] encryptedMessageBytes = new byte[encryptedMessageLen];

        clientInputStream.readFully(encryptedMessageBytes);

        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, symmetricKey);
            byte[] decryptedMessageBytes = cipher.doFinal(encryptedMessageBytes);
            String decryptedMessage = new String(decryptedMessageBytes);
            System.out.println("Received encrypted message (" + encryptedMessageLen + " bytes) from client.");
            System.out.println("Decrypted message: " + decryptedMessage);
            return decryptedMessage;
        } catch (Exception e) {
            System.err.println("Error decrypting message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean sendEncryptedMessage(Socket clientSocket, SecretKey symmetricKey, String message) {
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
            byte[] encryptedMessageBytes = cipher.doFinal(message.getBytes());
            DataOutputStream clientOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            clientOutputStream.writeInt(encryptedMessageBytes.length);
            clientOutputStream.write(encryptedMessageBytes);
            clientOutputStream.flush();
            return true;
        } catch (Exception e) {
            System.err.println("Error encrypting/sending message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Secure server started on " + HOST + ":" + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                try {
                    sendPublicKey(clientSocket);

                    SecretKey symmetricKey = receiveAndDecryptSymmetricKey(clientSocket);
                    if (symmetricKey == null) {
                        System.err.println("Failed to receive/decrypt symmetric key. Closing connection.");
                        serverSocket.close();
                        continue;
                    }

                    System.out.println("\nSecure communication established. Type 'quit' to end the session.");
                    BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

                    while (true) {
                        String message = receiveAndDecryptMessage(clientSocket, symmetricKey);

                        if (message == null) {
                            System.err.println("Failed to receive/decrypt message. Closing connection.");
                            break;
                        }
                        
                        System.out.println("\n[Client]: " + message);

                        if (message.equalsIgnoreCase("quit")) {
                            System.out.println("Client requested to end the session. Closing connection.");
                            break;
                        }

                        System.out.print("[Server]: ");
                        String serverResponse = consoleReader.readLine();
                        if (serverResponse == null || serverResponse.toLowerCase().equals("quit")) {
                            sendEncryptedMessage(clientSocket, symmetricKey, serverResponse != null ? serverResponse : "Server is closing the connection.");
                            break;
                        }

                        sendEncryptedMessage(clientSocket, symmetricKey, serverResponse);
                    }
                } catch (Exception e) {
                    System.err.println("Error during communication with client: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try {
                        clientSocket.close();
                        System.out.println("Client connection closed.");
                    } catch (Exception e) {
                        System.err.println("Error closing client socket: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
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
        SecureServer secureServer = new SecureServer();
        secureServer.start();
    }
}
