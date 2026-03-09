package Single_Client_Single_Server.Burak;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int PORT = 8888;

    public static void main(String[] args) {
        Socket clientSocket = null;
        Scanner scanner = null;

        try {
            System.out.println("Connecting to server at: " + SERVER_HOST + " : " + PORT);
            clientSocket = new Socket(SERVER_HOST, PORT);
            System.out.println("Successfully connected to the server.");

            scanner = new Scanner(System.in);

            OutputStream outputStream = clientSocket.getOutputStream();

            String message;

            while (true) {
                message = scanner.nextLine();

                byte[] userInput = message.getBytes();

                outputStream.write(userInput);
                outputStream.flush();

                if (message.equalsIgnoreCase("exit")) {
                    System.out.println("Exit command received. Closing connection.");
                    break;
                }
            }
        } catch (UnknownHostException e) {
            System.out.println("Host not found: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            try {
                if (scanner != null) {
                    scanner.close();
                }
            } catch (Exception e) {
                System.out.println("Error closing scanner: " + e.getMessage());
            }

            
            try {
                if (!clientSocket.isClosed() && clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("Error while closing client: " + e.getMessage());
            }
            
        }
    }
}
