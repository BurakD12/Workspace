package Secure_Multi_Client_Chat_App;

import java.io.Serializable;
import java.util.Map;

public class SecureMessage implements Serializable{
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String senderUsername;
    private String targetUsername;
    private byte[] ciphertext;
    private Map<String, byte[]> encryptedKeys;
    private byte[] digest;
    private byte[] digitalSignature;

    public SecureMessage(
        MessageType type, 
        String senderUsername, 
        String targetUsername, 
        byte[] ciphertext, 
        Map<String, byte[]> encryptedKeys,
        byte[] digest, 
        byte[] digitalSignature) {
            this.type = type;
            this.senderUsername = senderUsername;
            this.targetUsername = targetUsername;
            this.ciphertext = ciphertext;
            this.encryptedKeys = encryptedKeys;
            this.digest = digest;
            this.digitalSignature = digitalSignature;
        }

    public MessageType getType() {
        return type;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public byte[] getCiphertext() {
        return ciphertext;
    }

    public Map<String, byte[]> getEncryptedKeys() {
        return encryptedKeys;
    }

    public byte[] getDigest() {
        return digest;
    }

    public byte[] getDigitalSignature() {
        return digitalSignature;
    }

    public byte[] getEncryptedKeysFor(String username) {
        return encryptedKeys.get(username);
    }
}
