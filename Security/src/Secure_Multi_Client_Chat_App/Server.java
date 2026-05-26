package Secure_Multi_Client_Chat_App;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private ServerSocket serverSocket;
    private Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    private Map<String, PublicKey> publicKeyRegistry = new ConcurrentHashMap<>();

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[SERVER]: Server started on port " + port);
            System.out.println("[SERVER]: Waiting for clients to connect...");

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER]: New connection from " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            System.out.println("[SERVER]: Server Error: " + e.getMessage());
        }
    }

    public void registerClient(String username, PublicKey publicKey, ClientHandler handler) {
        connectedClients.put(username, handler);
        publicKeyRegistry.put(username, publicKey);

        System.out.println("[SERVER]: Client information has been stored.");
        broadcastPublicKeyToAll(username, publicKey);
        System.out.println("[SERVER]: Clients public key has been distributed.");
        sendExistingKeys(handler, username);
        System.out.println("[SERVER]: Client has received all existing public keys.");
    }

    public void sendExistingKeys(ClientHandler newClientHandler, String username) {
        for (Entry<String, PublicKey> entry : publicKeyRegistry.entrySet()) {
            if (!entry.getKey().equals(username)) {
                KeyDistributionMessage kdm = new KeyDistributionMessage(entry.getKey(), entry.getValue());
                newClientHandler.sendMessage(kdm);
            }
        }
    }

    public void broadcastPublicKeyToAll(String username, PublicKey publicKey) {
        KeyDistributionMessage kdm = new KeyDistributionMessage(username, publicKey);
        for (Entry<String, ClientHandler> entry : connectedClients.entrySet()) {
            if (!entry.getKey().equals(username)) {
                entry.getValue().sendMessage(kdm);
            }
        }
    }

    public void routeBroadcast(SecureMessage message) {
        String sender = message.getSenderUsername();
        for (Entry<String, ClientHandler> entry : connectedClients.entrySet()) {
            if (!entry.getKey().equals(sender)) {
                entry.getValue().sendMessage(message);
            }
        }
    }

    public void routeWhisper(SecureMessage message) {
        String target = message.getTargetUsername();
        ClientHandler targetHandler = connectedClients.get(target);

        if (targetHandler != null) {
            targetHandler.sendMessage(message);
        } else {
            // Target client not found, notify sender
            ClientHandler senderHandler = connectedClients.get(message.getSenderUsername());
            if (senderHandler != null) {
                senderHandler.sendMessage(new ServerNotification("User '" + target + "' not found. Whisper failed."));
            }
        }
    }

    public void removeClient(String username) {
        connectedClients.remove(username);
        publicKeyRegistry.remove(username);
        System.out.println("[SERVER]: Client '" + username + "' has been removed from the server.");

        ServerNotification removeNotification = new ServerNotification("User '" + username + "' has disconnected.");
        for (ClientHandler handler : connectedClients.values()) {
            handler.sendMessage(removeNotification);
        }
    }

    public PublicKey getPublicKey(String username) {
        PublicKey pubKey = publicKeyRegistry.get(username);
        if (pubKey == null) {
            System.out.println("[SERVER]: user could not be found.");
            return null;
        }
        System.out.println("[SERVER]: public key has been retrieved.");
        return pubKey;
    }

    public static void main(String[] args) {
        int port = 12345; // Default port

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("[SERVER]: Invalid port number. Using default port " + port);
            }
        }

        Server server = new Server();
        server.start(port);
    }
}