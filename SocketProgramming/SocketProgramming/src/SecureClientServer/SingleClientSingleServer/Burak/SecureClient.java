package SecureClientServer.SingleClientSingleServer.Burak;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class SecureClient {
    private static final int PORT = 12345;
    private static final String HOST = "localhost";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int AES_KEY_SIZE = 256;

    private PublicKey pubKey;
    private SecretKey symmetricKey;
    private Socket socket;
    private boolean running = true;


    private PublicKey reveivePublicKey() throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());

        int publicKeyLength = in.readInt();

        byte[] pubKeyBytes = new byte[publicKeyLength];
        in.readFully(pubKeyBytes);
        System.out.println("Received public key (" + publicKeyLength + " bytes)");

        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PublicKey placeholder = keyFactory.generatePublic(keySpec);
            System.out.println("Public key received successfully");
            return placeholder;
        } catch (Exception e) {
            System.err.println("Error loading public key: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

    }


    private boolean generateAndSendSymmetricKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGen.init(AES_KEY_SIZE);
            symmetricKey = keyGen.generateKey();
            System.out.println("Generated symmetric key");
            Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] encodedSymmetricKey = cipher.doFinal(symmetricKey.getEncoded());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            out.writeInt(encodedSymmetricKey.length);
            out.write(encodedSymmetricKey);
            out.flush();
            System.out.println("Sent encrypted symmetric key (" + encodedSymmetricKey.length + " bytes) to server");
            return true;
        } catch (Exception e) {
            System.err.println("Error generating/sending symmetric key: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private boolean sendEncryptedMessage(String message) {
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
            byte[] encodedMessageBytes = cipher.doFinal(message.getBytes("UTF-8"));

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(encodedMessageBytes.length);
            out.write(encodedMessageBytes);
            out.flush();
            return true;
        } catch (Exception e) {
            System.err.println("Error encrypting/sending message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private String receiveAndDecryptMessage() throws IOException {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            int messageLen = in.readInt();
            byte[] messageBytes = new byte[messageLen];
            in.readFully(messageBytes);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, symmetricKey);
            byte[] decryptedMessageBytes = cipher.doFinal(messageBytes);
            return new String(decryptedMessageBytes, "UTF-8");
        } catch (IOException e){
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
                    if (message.equalsIgnoreCase("quit")) {
                        System.out.println("Server requested to quit");
                        running = false;
                    }
                } catch (IOException e) {
                    if (running) {
                        System.err.println("\nError receiving message: " + e.getMessage());
                    }
                }     
            }
        });
        listenThread.setDaemon(true);
        listenThread.start();
    }

    private void connect() {
        try {
            System.out.println("Connecting to server at " + HOST + ":" + PORT + "...");
            socket = new Socket(HOST, PORT);
            System.out.println("Connected to server!");

            pubKey = reveivePublicKey();
            if (pubKey == null) {
                System.out.println("Failed to receive public key");
                socket.close();
                return;
            }

            if (!generateAndSendSymmetricKey()) {
                System.out.println("Failed to send symmetric key");
                socket.close();
                return;
            }
            System.out.println("\nSecure communication established!");
            System.out.println("You can now send messages. Type 'quit' to exit.\n");

            listenForMessages();
                
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

            while (running) {
                try {
                    System.out.print("\n[Client]: ");
                    String message = consoleReader.readLine();
                    if (message == null || message.trim().isEmpty()) {
                        continue;
                    }

                    if (!sendEncryptedMessage(message)) {
                        System.out.println("Failed to send message");
                        break;
                    }

                    if (message.equalsIgnoreCase("quit")) {
                        System.out.println("Disconnecting from server...");
                        running = false;
                    }
                } catch (Exception e) {
                    if (running) {
                        System.err.println("Error reading input: " + e.getMessage());
                    }
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
                    System.out.println("Socket is closed.");
                }
            } catch (Exception e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SecureClient secureClient = new SecureClient();
        secureClient.connect();
    }
}
