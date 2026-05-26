package Secure_Multi_Client_Chat_App;

import java.io.IOException;
import java.io.ObjectInputStream;

public class ClientReceiver implements Runnable{
    private ObjectInputStream in;
    private Client client;

    public ClientReceiver(ObjectInputStream in, Client client) {
        this.in = in;
        this.client = client;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Object msg = in.readObject();
                if (msg instanceof KeyDistributionMessage) {
                    client.receiveKeyDistribution((KeyDistributionMessage)msg);
                } else if (msg instanceof SecureMessage) {
                    client.receiveSecureMessage((SecureMessage)msg);
                } else if (msg instanceof ServerNotification) {
                    System.out.println("[Server Notification] " + ((ServerNotification)msg).getMessage());
                }
            } catch (ClassNotFoundException e) {
                System.out.println("[Client] Error handling the message type");
                return;
            } catch (IOException e) {
                System.out.println("Server closed the connection. Shutting down client-side.");
                return;
            }
        }
    }
}
