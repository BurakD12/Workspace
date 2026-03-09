package Secure_Multi_Client_Chat_App;

import java.io.Serializable;

public class ServerNotification implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String message;

    public ServerNotification(String message) {
        this.type = MessageType.SERVER_NOTIFICATION;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    
}
