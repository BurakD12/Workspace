package Secure_Multi_Client_Chat_App;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PublicKey;

public class ClientHandler implements Runnable{
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Server server;
    private String username;


    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        try {
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            Object obj;

            while ((obj = in.readObject()) != null) {
                if (obj instanceof KeyExchangeMessage) {
                    handleKeyExchange((KeyExchangeMessage) obj);
                } else if (obj instanceof SecureMessage) {
                    SecureMessage msg = (SecureMessage) obj;
                    if (msg.getType() == MessageType.BROADCAST) {
                        handleBroadcast(msg);
                    } else if (msg.getType() == MessageType.WHISPER) {
                        handleWhisper(msg);
                    }
                }
            }
        } catch (EOFException e) {
            System.out.println("[SERVER]: Client " + username != null ? username : "unknown client" + " disconnected."); 
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[SERVER]: Connection error with " + username != null ? username : "unknown client" + ": " + e.getMessage());
        } finally {
            if (username != null) {
                server.removeClient(username);
            }
            close();
        }
    }

    public void handleKeyExchange(KeyExchangeMessage msg) {
        username = msg.getUsername();
        try {
            server.registerClient(username, msg.getPublicKey(), this);
            System.out.println("[SERVER]: " + username + " has joined the chat.");
        } catch (Exception e) {
            System.out.println("[SERVER]: Failed to register client: " + e.getMessage());
        }
    }

    public void handleBroadcast(SecureMessage msg) {
        server.routeBroadcast(msg);
    }
    
    public void handleWhisper(SecureMessage msg) {
        server.routeWhisper(msg);
    }

    public synchronized void sendMessage(Object obj) {
        try {
            out.writeObject(obj);
            out.flush();
        } catch (IOException e) {
            System.out.println("[SERVER]: Error sending message from user: " + username + ".");
        }
    }

    public void close() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }            
        } catch (IOException e) {
            System.out.println("[SERVER]: Error while disconnecting the user: " + username + " from server." );
        }
    }
}
