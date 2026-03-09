package SecureClientServer.MultiClientSingleServer.Burak;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SecureMultiServer {
    private final static int PORT = 8888;
    private final static String RSA_ALGORITHM = "RSA";
    private final static String RSA_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
    private final static String AES_ALGORITHM = "AES";
    private final static String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private final static int RSA_KEY_SIZE = 2048;
    private final static int AES_KEY_SIZE = 256;
    private KeyPair keyPair;
    private ServerSocket serverSocket;
    private Set<SecureClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public SecureMultiServer() {
        generateKeyPair();
    }

    private void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyGen.initialize(RSA_KEY_SIZE);
            keyPair = keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error generating RSA keyPair.");
            e.printStackTrace();
        }

    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("[" + getCurrentTime() + "] Secure server listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                SecureClientHandler handler = new SecureClientHandler(clientSocket, keyPair, clients);

                clients.add(handler);
                new Thread(handler).start();
                System.out.println("[" + getCurrentTime() + "] New client connected. Total clients: " + clients.size());
            }
        } catch (Exception e) {
            System.err.println("[" + getCurrentTime() + "] Server error: " + e.getMessage());
        }
    }

    public static String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public static void main(String[] args) {
        SecureMultiServer secureMultiServer = new SecureMultiServer();
        secureMultiServer.start();
    }

    public class SecureClientHandler implements Runnable {
        private Socket socket;
        private KeyPair keyPair;
        private SecretKey symmetricKey;
        private Set<SecureClientHandler> allClients;
        private String clientName;
        private boolean running = true;
        private static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        public SecureClientHandler(Socket socket, KeyPair keyPair, Set<SecureClientHandler> allClients) {
            this.socket = socket;
            this.keyPair = keyPair;
            this.allClients = allClients;

            this.clientName = socket.getRemoteSocketAddress().toString().substring(1).replace(":", "_");
        }

        @Override
        public void run() {
            try {
                sendPublicKey();
                this.symmetricKey = receiveAndDecryptSymmetricKey();
                if (symmetricKey == null) {
                    closeConnection();
                    return;
                }

                sendEncryptedMessage("Welcome to Secure Group Chat! Type 'exit' to leave.");
                broadcastEncryptedMessage(clientName + " has joined the chat.", this);

                while (running) {
                    String message = receiveAndDecryptMessage();
                    if (message == null) {
                        System.out.println("Client has disconnected.");
                        break;
                    }
                    if (message.equalsIgnoreCase("exit")) {
                        System.out.println("Client has requested to exit.");
                        break;
                    }
                    System.out.println(getCurrentTime() + " [" + clientName + "] " + message);
                    broadcastEncryptedMessage(getCurrentTime() + " [" + clientName + "] " + message, this);
                }

                broadcastEncryptedMessage("The client: [" + clientName + "] has left the chat.", this);

            } catch (Exception e) {
                System.err.println(getCurrentTime() + " [" + clientName + "] " + e.getMessage());
                e.printStackTrace();
            } finally {
                closeConnection();
                allClients.remove(this);
            }
        }

        private void sendPublicKey() throws IOException {
            byte[] pubKeyBytes = this.keyPair.getPublic().getEncoded();
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(pubKeyBytes.length);
            out.write(pubKeyBytes);
            out.flush();
        }

        private SecretKey receiveAndDecryptSymmetricKey() {
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                int encryptedKeyLength = in.readInt();
                byte[] encryptedKey = new byte[encryptedKeyLength];
                in.readFully(encryptedKey);

                Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
                byte[] decryptedKeyBytes = cipher.doFinal(encryptedKey);
    
                return new SecretKeySpec(decryptedKeyBytes, AES_ALGORITHM);
            } catch (Exception e) {
                System.err.println("Failed to receive or decrypt symmetric key: " + e.getMessage());
                return null;
            }
        }

        private String receiveAndDecryptMessage() {
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                int messageLen = in.readInt();
                byte[] encryptedMessage = new byte[messageLen];
                in.readFully(encryptedMessage);
                Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, symmetricKey);
                byte[] message = cipher.doFinal(encryptedMessage);
                return new String(message, "UTF-8");
            } catch (Exception e) {
                System.err.println("Failed to receive or decrypt the message: " + e.getMessage());
                return null;
            }
        }

        private synchronized void sendEncryptedMessage(String message) {
            try {
                Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
                cipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
                byte[] encryptedMessage = cipher.doFinal(message.getBytes("UTF-8"));
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.writeInt(encryptedMessage.length);
                out.write(encryptedMessage);
                out.flush();
            } catch (Exception e) {
                System.err.println("Failed to encrpt or send the message." + e.getMessage());
                closeConnection();
            }
        }

        private void broadcastEncryptedMessage(String message, SecureClientHandler sender) {
            synchronized (allClients) {
                for (SecureClientHandler client : allClients) {
                    if (sender == null || !client.equals(sender)) {
                        client.sendEncryptedMessage(message);
                    }
                }
            }
        }

        private void closeConnection() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e) {
                System.err.println("Failed to close the connection. " + e.getMessage());
            }
        }

        public static String getCurrentTime() {
            return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
    }
}
