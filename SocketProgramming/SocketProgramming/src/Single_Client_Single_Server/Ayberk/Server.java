package Single_Client_Single_Server.Ayberk;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int PORT = 8888;

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
    
        try {
            // Create server socket on specified port
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started and listening on port " + PORT);
            System.out.println("Waiting for client connection...");

            // Accept client connection
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

            // Get input stream to receive data from client
            InputStream inputStream = clientSocket.getInputStream();
            
            // Buffer to hold incoming data
            byte[] buffer = new byte[1024];
            int bytesRead;

            // Read message from client
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // Create byte array of exact length    
                byte[] messageBytes = new byte[bytesRead];
                System.arraycopy(buffer, 0, messageBytes, 0, bytesRead);

                // Convert to string to check for "exit" command
                String message = new String(messageBytes);
                System.out.println("Received message: " + message);

                if (message.trim().equalsIgnoreCase("exit")) {
                    System.out.println("Exit command received. Closing connection.");
                    break;
                }
            }

            // Close resources
            clientSocket.close();
            System.out.println("Client connection closed.");
        } catch (Exception e) {
            System.out.println("Server error: " + e.getMessage());
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
}