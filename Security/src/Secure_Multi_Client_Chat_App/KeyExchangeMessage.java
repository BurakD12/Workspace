package Secure_Multi_Client_Chat_App;

import java.io.Serializable;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public class KeyExchangeMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String username;
    private byte[] publicKeyBytes;

    public KeyExchangeMessage(String username, PublicKey publicKey) {
        this.type = MessageType.KEY_EXCHANGE;
        this.username = username;
        this.publicKeyBytes = publicKey.getEncoded();
    }

    public String getUsername() {
        return username;
    }

    public PublicKey getPublicKey() throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        return keyFactory.generatePublic(keySpec);
    }
}
