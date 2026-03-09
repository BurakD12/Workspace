package E2E_Though_Server.Burak;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 8888;
    private static Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>()); 
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static Map<String, ClientHandler> clientMap = new ConcurrentHashMap<>();
    
    public static void main(String[] args) {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("[SERVER] Server started on port " + PORT);
            System.out.println("[SERVER] Waiting for clients to connect...");
            System.out.println("[SERVER] Available commands:\n" + 
                "'\\w <username> <message>' to whisper a direct message to a specific user.\n" + 
                "'\\b <message>' to broadcast a message to the server.\n" +
                "'\\list' to list all users.\n" +
                "'\\name <username>' to change your username.\n" +
                "'\\help' to display all commands.\n" +
                "'exit' to disconnect from the server.");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                ClientHandler client = new ClientHandler(clientSocket, clients, clientMap);
                clients.add(client);
                clientMap.put(client.getClientName(), client);

                new Thread(client).start();
                System.out.println("[SERVER] " + getCurrentTime() + " - New client connected: " + clientSocket.getInetAddress() + ". Total clients: " + clients.size()); 


            }
        } catch (Exception e) {
            System.err.println("[SERVER] Error: " + e.getMessage());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    System.err.println("[SERVER] Error closing server socket: " + e.getMessage());
                }
            }
        }
    }

    private static String getCurrentTime() {
        return LocalTime.now().format(timeFormatter);
    }
}

class ClientHandler implements Runnable{
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Set<ClientHandler> allClients;
    private Map<String, ClientHandler> clientMap;
    private String clientName;
    private String displayName;
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ClientHandler(Socket socket, Set<ClientHandler> allClients, Map<String, ClientHandler> clientMap) throws Exception {
        this.socket = socket;
        this.allClients = allClients;
        this.clientMap = clientMap;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
        this.clientName = "Client_" + socket.getInetAddress().toString().substring(1).replace(":", "_");
        this.displayName = this.clientName;
    }

    public String getClientName() {
        return clientName;
    }
    
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public void run() {
        try {
            sendMessage("[SERVER] Welcome to the group chat!");
            broadcastMessage(" " + displayName + " has joined the chat.", this);

            byte[] buffer = new byte[1024];
            int bytesRead;
            boolean isEnded = false;

            while ((bytesRead = inputStream.read(buffer)) != -1 && isEnded == false) {
                byte[] messageBytes = new byte[bytesRead];
                System.arraycopy(buffer, 0, messageBytes, 0, bytesRead);

                String message = new String(messageBytes).trim();
                String[] branchTokens = message.split("\\s+", 2);
                String branch = branchTokens[0];

                switch (branch) {
                    case "\\w" -> {
                        whisper(message);
                    }
                    case "\\b" -> {
                        broadcastMessage("[" + displayName + "] " + message.substring(3).trim(), this);
                        System.out.println("[" + getCurrentTime() + "] " + displayName + ": " + message);
                    }
                    case "\\list" -> {
                        listAll();
                    }
                    case "\\name" -> {
                        changeName(message);
                    }
                    case "\\help" -> {
                        sendMessage("Available commands:\n" +
                            "'\\w <username> <message>' to whisper a direct message to a specific user.\n" +
                            "'\\b <message>' to broadcast a message to the server.\n" +
                            "'\\list' to list all users.\n" +
                            "'\\name <username>' to change your username.\n" + 
                            "'\\help' to list all available commands.\n"+
                            "'\\clear' to clear the chat.\n"+
                            "'exit' to disconnect from server."
                        );
                    }
                    case "exit" -> {
                        System.out.println("[SERVER] " + displayName + " requested to exit.");
                        System.out.println("[SERVER] " + getCurrentTime() + " - " + clientName + " (" + displayName + ")" + " disconnected.");
                        isEnded = true;
                    }
                    default -> {
                        System.out.println("Invalid command please try again.");
                    }
                }
                
            }
        } catch (Exception e) {
            System.err.println("[CLIENT HANDLER ERROR] Error with " + clientName + " (" + displayName + ") " + ": " + e.getMessage());
        } finally {
            closeConnection();
            allClients.remove(this);
        }
    }

    private String getCurrentTime() {
        return LocalTime.now().format(timeFormatter);
    }

    private synchronized void sendMessage(String message) {
        try {
            byte[] messageBytes = message.getBytes();
            outputStream.write(messageBytes);
            outputStream.flush();
        } catch (Exception e) {
            System.err.println("[CLIENT HANDLER ERROR] Error sending message to " + clientName + ": " + e.getMessage());
            closeConnection();
        }
    }

    private void broadcastMessage(String message, ClientHandler sender) {
        synchronized(allClients) {
            for (ClientHandler client : allClients) {
                if (sender != null && !client.equals(sender)) {
                    client.sendMessage(message);
                }
            }
        }
    }

    private void whisper(String message){
        String[] whisperTokens = message.split("\\s", 3);
        String targetName = whisperTokens[1];
        String finalMessage = whisperTokens[2];

        synchronized(allClients) {
            ClientHandler target = findClient(targetName);
            if (target == null) {
                sendMessage("Target user not found please try again. You can look up the available users with '\\list'");
            } else {
                target.sendMessage("[" + displayName + "] " + finalMessage);
            }
        }
    }

    private ClientHandler findClient(String name) {
        for (ClientHandler client : allClients) {
            if (client.displayName.equals(name)) {
                return client;
            }
        }
        return null;
    }

    private void listAll(){
        StringBuilder list = new StringBuilder();
        synchronized(allClients){
            for (ClientHandler client : allClients) {
                list.append(client.displayName + "\n");
            }
            sendMessage(list.toString());
        }
    }

    private void changeName(String message){
        String[] changeTokens = message.split("\\s+", 2);
        if (changeTokens[1].contains(" ")) {
            sendMessage("Invalid username. Please try again, the username cannot inclue spaces!");
        } else {
            synchronized(allClients){
                String requestedName = changeTokens[1];
                for (ClientHandler client : allClients) {
                    if (client.displayName.equals(requestedName)) {
                        sendMessage("Username taken, please try again.");
                        return;
                    }
                }
                this.displayName = requestedName;
            }
        }
    }

    private void closeConnection(){
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            System.err.println("[CLIENT HANDLER ERROR] Error closing connection for " + clientName + ": " + e.getMessage());
        }
    }

    
} 