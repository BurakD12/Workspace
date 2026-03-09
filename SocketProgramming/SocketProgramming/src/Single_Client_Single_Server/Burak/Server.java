package Single_Client_Single_Server.Burak;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int PORT = 8888;

    public static void main(String[] args) {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port: " + PORT);

            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

            InputStream inputStream = clientSocket.getInputStream();

            byte[] buffer = new byte[1024];
            int readBytes;

            while ((readBytes = inputStream.read(buffer)) != -1) {
                byte[] messageBytes = new byte[readBytes];
                System.arraycopy(buffer, 0, messageBytes, 0, readBytes);

                String message = new String(messageBytes);
                System.out.println("Incoming message: " + message);

                if (message.trim().equals("exit")) {
                    System.out.println("Exit command received. Closing connection.");
                    break;
                }
            }
            
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
            } catch (IOException e) {
                System.out.println("Error closing server socket: " + e.getMessage());
            }
        }
    }
}
