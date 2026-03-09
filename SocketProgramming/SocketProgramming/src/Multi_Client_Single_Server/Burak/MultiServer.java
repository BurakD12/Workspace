package Multi_Client_Single_Server.Burak;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MultiServer {

    private static final int PORT = 8888;
    private static final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("[SERVER] Server started on port " + PORT);
            System.out.println("[SERVER] Waiting for clients to connect...");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                ClientHandler clientHandler = new ClientHandler(clientSocket, clients);
                clients.add(clientHandler);

                new Thread(clientHandler).start();
                System.out.println("[SERVER] " + getCurrentTime() + " - New client connected: " + clientSocket.getInetAddress() + ". Total clients: " + clients.size());


            }
        } catch (Exception e) {
            System.err.println("[SERVER] Error: " + e.getMessage());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()){
                try {
                    serverSocket.close();
                } catch (Exception e) {
                    System.err.println("[SERVER] Error closing server socket: " + e.getMessage());
                }
            }
        }
    }
    public static String getCurrentTime(){
        return LocalTime.now().format(timeFormatter);
    }
}

class ClientHandler implements Runnable{
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Set<ClientHandler> allClients;
    private String clientName;
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ClientHandler(Socket socket, Set<ClientHandler> allClients) throws Exception {
        this.socket = socket;
        this.allClients = allClients;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
        this.clientName = "Client_" + socket.getInetAddress().toString().substring(1).replace(":", "_");
    }
    
    @Override
    public void run() {
        try {
            sendMessage(" Welcome to Group Chat!, Type 'exit' to leave the chat.\n");
            broadcastMessage(" " + clientName + " has joined the chat.\n", this);

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] messageBytes = new byte[bytesRead];
                System.arraycopy(buffer, 0, messageBytes, 0, bytesRead);

                String message = new String(messageBytes).trim();
                System.out.println("[" + getCurrentTime() + "] " + clientName + ": " + message);

                if (message.equalsIgnoreCase("exit")){
                    System.out.println("[SERVER] " + clientName + " requested to exit.");
                    break;
                }
                broadcastMessage("[" + clientName + "] " + message + "\n", this);
            }

            broadcastMessage("[" + clientName + "] has left the chat.\n", this);
            System.out.println("[SERVER] " + getCurrentTime() + " - " + clientName + " disconnected.");
        } catch (Exception e) {
            System.err.println("[CLIENT HANDLER ERROR] Error with " + clientName + ": " + e.getMessage());  
        } finally {
            closeConnection();
            allClients.remove(this);
        }
    }

    private synchronized void sendMessage(String message){
        try {
            byte[] messageBytes = message.getBytes();
            outputStream.write(messageBytes);
            outputStream.flush();
        } catch (Exception e) {
            System.err.println("[CLIENT HANDLER ERROR] Error sending message to " + clientName + ": " + e.getMessage());
            closeConnection();
        }
    }

    private void broadcastMessage(String message, ClientHandler sender){
        synchronized(allClients){
            for (ClientHandler client : allClients) {
                if (sender != null && !client.equals(sender)) {
                    client.sendMessage(message);
                }
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

    public static String getCurrentTime() {
        return LocalTime.now().format(timeFormatter);
    }
}