package E2E_Though_Server.Burak;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    public static void main(String[] args) {
        Scanner scanner = null;
        Socket socket = null;

        try {
            System.out.println("[CLIENT] Connecting to server at " + SERVER_HOST + ":" + SERVER_PORT);
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            System.out.println("[CLIENT] Connected to server.");

            System.out.println("Available commands:\n" +
                            "'\\w <username> <message>' to whisper a direct message to a specific user.\n" +
                            "'\\b <message>' to broadcast a message to the server.\n" +
                            "'\\list' to list all users.\n" +
                            "'\\name <username>' to change your username.\n" + 
                            "'\\help' to list all available commands.\n"+
                            "'\\clear' to clear the chat.\n"+
                            "'exit' to disconnect from server."
                        );

            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            byte[] commandHelper = "\\help".getBytes();

            Thread receiveThread = new Thread(new MessageReceiver(inputStream));
            receiveThread.setDaemon(true);
            receiveThread.start();

            boolean isServerReplied = true;

            scanner = new Scanner(System.in);
            String userInput;

            
            while (true) {
                if (!isServerReplied) {
                    System.out.print("[CLIENT] > ");
                }
                
                isServerReplied = false;

                userInput = scanner.nextLine();

                byte[] message = userInput.getBytes("UTF-8");

                if (userInput.equals("\\clear")) {
                    clearTerminal();
                    outputStream.write(commandHelper);
                    outputStream.flush();
                } else if (userInput.equals("exit")) {
                    outputStream.write(message);
                    outputStream.flush();
                    System.out.println("[CLIENT] Closing connection.");
                    break;
                } else {
                    outputStream.write(message);
                    outputStream.flush();
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("[CLIENT] Unknown host: " + SERVER_HOST);
        } catch (IOException e) {
            System.err.println("[CLIENT] I/O error: " + e.getMessage());
        } finally {
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

    private static void clearTerminal() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            System.out.println("Error clearing terminal: " + e.getMessage());
        }
    }
}

class MessageReceiver implements Runnable{
    private InputStream inputStream;
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public MessageReceiver(InputStream inputStream){
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
                System.out.println("\n[" + getCurrentTime() + "] " + message);
                System.out.print("[CLIENT] > ");

                System.out.flush();
            }
        } catch (Exception e) {
            if (inputStream != null) {
                System.err.println("[RECEIVER] I/O error: " + e.getMessage());
            }
        }
    }

    private static String getCurrentTime() {
        return LocalTime.now().format(timeFormatter);
    }
}
