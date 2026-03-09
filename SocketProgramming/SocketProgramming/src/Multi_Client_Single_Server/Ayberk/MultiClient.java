package Multi_Client_Single_Server.Ayberk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class MultiClient {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;

    public static void main(String[] args) {
        Socket socket = null;
        Scanner scanner = null;

        try {
            System.out.println("[CLIENT] Connecting to server at " + SERVER_HOST + ":" + SERVER_PORT);
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            System.out.println("[CLIENT] Connected to server.");

            // Get streams
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            // Create and start a thread to receive messages from the server
            Thread receiveThread = new Thread(new MessageReceiver(inputStream));
            receiveThread.setDaemon(true); // Set as daemon so it doesn't block JVM exit
            receiveThread.start();
            boolean isServerReplied = true;

            // Main thread: handle user input and send messages to the server
            scanner = new Scanner(System.in);
            String userInput;

            while (true) {
                if (!isServerReplied) {
                    System.out.print("[CLIENT] > ");
                }
                isServerReplied = false;

                // Read user input
                userInput = scanner.nextLine();

                // Convert input to bytes and send to server
                byte[] message = userInput.getBytes("UTF-8");

                // Send message length first
                outputStream.write(message);
                outputStream.flush();

                // Check if user wants to exit
                if (userInput.equalsIgnoreCase("exit")) {
                    System.out.println("[CLIENT] Closing connection.");
                    break;
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("[CLIENT] Unknown host: " + SERVER_HOST);
        } catch (IOException e) {
            System.err.println("[CLIENT] I/O error: " + e.getMessage());
        } finally {
            // Clean up resources
            try {
                if (scanner != null) {
                    scanner.close();
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("[CLIENT] Error closing resources: " + e.getMessage());
            }
        }
    }
}

class MessageReceiver implements Runnable {
    private InputStream inputStream;

    public MessageReceiver(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] messageBytes = new byte[bytesRead];
                System.arraycopy(buffer, 0, messageBytes, 0, bytesRead);

                String message = new String(messageBytes, "UTF-8");
                System.out.println("\n[RECEIVER] Message from server: " + message);
                System.out.print("[CLIENT] > ");

                System.out.flush();
            }
        } catch (IOException e) {
            if (inputStream != null) {
                System.err.println("[RECEIVER] I/O error: " + e.getMessage());
            }
        }
    }
}