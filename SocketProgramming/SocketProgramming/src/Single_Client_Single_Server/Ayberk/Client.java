
package Single_Client_Single_Server.Ayberk;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;

    public static void main(String[] args) {
        Socket socket = null;
        Scanner scanner = null;

        try {
            // Establish connection to the server
            System.out.println("Connecting to server at " + SERVER_HOST + ":" + SERVER_PORT);
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            System.out.println("Connected to successfully.");

            // Scanner for user input
            scanner = new Scanner(System.in);

            // Get output stream to send data to the server
            OutputStream outputStream = socket.getOutputStream();

            System.out.print("Enter messages to send to the server (type 'exit' to quit):");
            String userInput;
            while (true) {
                // Read user input
                userInput = scanner.nextLine();

                // Convert string to bytes
                byte[] data = userInput.getBytes();

                // Send message to the server
                outputStream.write(data);
                outputStream.flush();

                // Check if user wants to exit
                if ("exit".equalsIgnoreCase(userInput)) {
                    System.out.println("Exiting...");
                    break;
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Error: Unknown host " + SERVER_HOST);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close resources
            if (scanner != null) {
                scanner.close();
            }
            if (socket != null) {
                try {
                    socket.close();
                    System.out.println("Connection closed.");
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            }
        }

    }
}