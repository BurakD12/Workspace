package E2E_Though_Server.Ayberk;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class Server {

    private static final int PORT = 8888;
    private static final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, ClientHandler> clientMap = new ConcurrentHashMap<>();
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("[SERVER] Server started on port " + PORT);
            System.out.println("[SERVER] Waiting for clients to connect...");
            System.out.println("[SERVER] Available commands:");
            System.out.println("[SERVER]   \\w <username> <message> - Send private message (whisper)");
            System.out.println("[SERVER]   \\b <message> - Broadcast message to all users");
            System.out.println("[SERVER]   \\list - List all online users");
            System.out.println("[SERVER]   \\help - Show available commands");
            System.out.println("[SERVER]   \\name <username> - Set your display name");
            System.out.println("[SERVER]   exit - Leave the chat\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                ClientHandler clientHandler = new ClientHandler(clientSocket, clients, clientMap);
                clients.add(clientHandler);

                new Thread(clientHandler).start();
                System.out.println("[SERVER] " + getCurrentTime() + " - New client connected: " + 
                    clientSocket.getInetAddress() + ". Total clients: " + clients.size());
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

    public static String getCurrentTime() {
        return LocalTime.now().format(timeFormatter);
    }
}

class ClientHandler implements Runnable {
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

    @Override
    public void run() {
        try {
            sendMessage("Welcome to E2E Chat Server!\n");
            sendMessage("Type '\\help' to see available commands.\n");
            sendMessage("Set your name with '\\name <username>' or start chatting!\n\n");
            
            // Register client with default name
            clientMap.put(displayName, this);
            
            broadcastMessage("[" + displayName + "] has joined the chat.\n", this);

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] messageBytes = new byte[bytesRead];
                System.arraycopy(buffer, 0, messageBytes, 0, bytesRead);

                String message = new String(messageBytes).trim();
                System.out.println("[" + getCurrentTime() + "] " + displayName + ": " + message);

                if (message.equalsIgnoreCase("exit")) {
                    System.out.println("[SERVER] " + displayName + " requested to exit.");
                    break;
                }

                // Process commands
                if (message.startsWith("\\")) {
                    processCommand(message);
                } else {
                    // Default: broadcast message
                    broadcastMessage("[" + displayName + "] " + message + "\n", this);
                }
            }

            broadcastMessage("[" + displayName + "] has left the chat.\n", this);
            System.out.println("[SERVER] " + getCurrentTime() + " - " + displayName + " disconnected.");
        } catch (Exception e) {
            System.err.println("[CLIENT HANDLER ERROR] Error with " + displayName + ": " + e.getMessage());
        } finally {
            closeConnection();
            allClients.remove(this);
            clientMap.remove(displayName);
        }
    }

    private void processCommand(String command) {
        String[] parts = command.split("\\s+", 3);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "\\w":
            case "\\whisper":
                if (parts.length < 3) {
                    sendMessage("[ERROR] Usage: \\w <username> <message>\n");
                    return;
                }
                String targetUser = parts[1];
                String whisperMessage = parts[2];
                whisper(targetUser, whisperMessage);
                break;

            case "\\b":
            case "\\broadcast":
                if (parts.length < 2) {
                    sendMessage("[ERROR] Usage: \\b <message>\n");
                    return;
                }
                String broadcastMessage = parts[1];
                if (parts.length == 3) {
                    broadcastMessage = parts[1] + " " + parts[2];
                }
                broadcastMessage("[BROADCAST from " + displayName + "] " + broadcastMessage + "\n", this);
                break;

            case "\\list":
            case "\\users":
                listUsers();
                break;

            case "\\help":
                showHelp();
                break;

            case "\\name":
                if (parts.length < 2) {
                    sendMessage("[ERROR] Usage: \\name <username>\n");
                    return;
                }
                changeName(parts[1]);
                break;

            default:
                sendMessage("[ERROR] Unknown command: " + cmd + ". Type '\\help' for available commands.\n");
        }
    }

    private void whisper(String targetUser, String message) {
        ClientHandler target = clientMap.get(targetUser);
        if (target != null && !target.equals(this)) {
            target.sendMessage("[WHISPER from " + displayName + "] " + message + "\n");
            sendMessage("[WHISPER to " + targetUser + "] " + message + "\n");
            System.out.println("[SERVER] " + displayName + " whispered to " + targetUser + ": " + message);
        } else if (target != null && target.equals(this)) {
            sendMessage("[ERROR] You cannot whisper to yourself!\n");
        } else {
            sendMessage("[ERROR] User '" + targetUser + "' is not online. Use '\\list' to see online users.\n");
        }
    }

    private void listUsers() {
        StringBuilder userList = new StringBuilder("\n[ONLINE USERS]\n");
        synchronized (allClients) {
            for (ClientHandler client : allClients) {
                userList.append("  - ").append(client.displayName);
                if (client.equals(this)) {
                    userList.append(" (you)");
                }
                userList.append("\n");
            }
        }
        userList.append("[Total: ").append(allClients.size()).append(" users]\n\n");
        sendMessage(userList.toString());
    }

    private void showHelp() {
        String help = "\n[AVAILABLE COMMANDS]\n" +
                "  \\w <username> <message>  - Send private message (whisper)\n" +
                "  \\b <message>             - Broadcast message to all users\n" +
                "  \\list                    - List all online users\n" +
                "  \\name <username>         - Change your display name\n" +
                "  \\help                    - Show this help message\n" +
                "  exit                     - Leave the chat\n\n";
        sendMessage(help);
    }

    private void changeName(String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            sendMessage("[ERROR] Name cannot be empty!\n");
            return;
        }
        
        String oldName = displayName;
        
        // Check if name is already taken
        if (clientMap.containsKey(newName) && !clientMap.get(newName).equals(this)) {
            sendMessage("[ERROR] Name '" + newName + "' is already taken!\n");
            return;
        }
        
        // Update name
        clientMap.remove(oldName);
        displayName = newName;
        clientMap.put(displayName, this);
        
        sendMessage("[INFO] Your name changed from '" + oldName + "' to '" + displayName + "'\n");
        broadcastMessage("[INFO] " + oldName + " is now known as " + displayName + "\n", this);
        System.out.println("[SERVER] " + oldName + " changed name to " + displayName);
    }

    private synchronized void sendMessage(String message) {
        try {
            byte[] messageBytes = message.getBytes();
            outputStream.write(messageBytes);
            outputStream.flush();
        } catch (Exception e) {
            System.err.println("[CLIENT HANDLER ERROR] Error sending message to " + displayName + ": " + e.getMessage());
            closeConnection();
        }
    }

    private void broadcastMessage(String message, ClientHandler sender) {
        synchronized (allClients) {
            for (ClientHandler client : allClients) {
                if (sender != null && !client.equals(sender)) {
                    client.sendMessage(message);
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
            System.err.println("[CLIENT HANDLER ERROR] Error closing connection for " + displayName + ": " + e.getMessage());
        }
    }

    public static String getCurrentTime() {
        return LocalTime.now().format(timeFormatter);
    }
}
