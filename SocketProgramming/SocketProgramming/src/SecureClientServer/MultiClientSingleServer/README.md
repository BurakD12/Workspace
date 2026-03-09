# Secure Multi-Client Single Server — Project Guide

## Overview

In this project you will combine **two things you have already built**:

| Already done | What you add |
|---|---|
| Multi-client group chat (`Multi_Client_Single_Server/Ayberk/`) | Security layer (RSA + AES) |
| Secure single-client chat (`SecureSocketProgramming/`) | Multi-client / broadcast support |

The result is a **group chat server where every message on the wire is AES-encrypted**, and the AES key for each client is exchanged securely using RSA.

---

## Security Model

### Why two algorithms?

| Algorithm | Role | Why |
|---|---|---|
| **RSA 2048-bit** (asymmetric) | Key exchange only | Safe to send over an untrusted channel; no shared secret needed upfront |
| **AES 256-bit** (symmetric) | Every chat message | Fast enough for real-time chat; the key never travels in plain text |

### Handshake Flow (per client)

```
Client                                    Server
  |                                          |
  |  <--- [RSA Public Key (bytes)]  ------   |   Server sends its public key
  |                                          |
  |  --- [AES Key encrypted with RSA] --->   |   Client generates AES key, encrypts it
  |                                          |
  |  <========= AES-encrypted chat =======>  |   All further messages use AES
```

This is the same handshake you implemented in `SecureSocketProgramming/`. The difference here is that:
- The server handles **many clients concurrently** (one thread per client).
- Every client gets its **own unique AES key** (the server stores each key in the `SecureClientHandler` for that client).
- Broadcast messages are **re-encrypted for each recipient** using their individual AES key before sending.

---

## Project Structure

```
SecureClientServer/MultiClientSingleServer/
├── README.md                  ← this file
├── SecureMultiServer.java     ← server (you implement)
└── SecureMultiClient.java     ← client (you implement)
```

---

## Class: `SecureMultiServer`

**File:** `SecureMultiServer.java`
**Purpose:** Starts the server, generates a single RSA key pair, and spawns one `SecureClientHandler` thread for every connecting client.

### Constants / Fields

| Field | Type | Value | Description |
|---|---|---|---|
| `PORT` | `int` | `8888` | TCP port the server listens on |
| `RSA_ALGORITHM` | `String` | `"RSA"` | Algorithm name for `KeyPairGenerator` |
| `RSA_TRANSFORMATION` | `String` | `"RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING"` | Cipher transformation for RSA encryption/decryption |
| `AES_ALGORITHM` | `String` | `"AES"` | Algorithm name for `SecretKeySpec` |
| `AES_TRANSFORMATION` | `String` | `"AES/ECB/PKCS5Padding"` | Cipher transformation for AES encryption/decryption |
| `RSA_KEY_SIZE` | `int` | `2048` | RSA key length in bits |
| `AES_KEY_SIZE` | `int` | `256` | AES key length in bits |
| `keyPair` | `KeyPair` | — | Holds the server's RSA public + private key |
| `serverSocket` | `ServerSocket` | — | The listening socket |
| `clients` | `Set<SecureClientHandler>` | `Collections.synchronizedSet(new HashSet<>())` | Thread-safe set of all connected handlers |
| `timeFormatter` | `DateTimeFormatter` | `"HH:mm:ss"` | For timestamped console logs |

> **Note:** `keyPair` is created **once** in the constructor and reused for every client connection. Each client sends its own AES key encrypted with this same RSA public key.

### Constructor: `SecureMultiServer()`

Calls `generateKeyPair()` immediately so the key is ready before the first client connects.

### Methods

---

#### `generateKeyPair()` — `private void`

Generates the RSA key pair and stores it in `this.keyPair`.

| | |
|---|---|
| **Input** | none |
| **Output** | `void` — sets `this.keyPair` as a side effect |
| **Throws** | Catches `NoSuchAlgorithmException` internally, prints error |

**Steps:**
1. `KeyPairGenerator.getInstance(RSA_ALGORITHM)`
2. `keyGen.initialize(RSA_KEY_SIZE)`
3. `this.keyPair = keyGen.generateKeyPair()`

---

#### `start()` — `public void`

Accepts client connections in an infinite loop and delegates each one to a new `SecureClientHandler` thread.

| | |
|---|---|
| **Input** | none |
| **Output** | `void` — runs until the process is killed |
| **Throws** | `IOException` caught internally |

**Steps:**
1. Create `ServerSocket` on `PORT`.
2. Loop: `serverSocket.accept()` → blocks until a client connects.
3. Create `SecureClientHandler handler = new SecureClientHandler(clientSocket, keyPair, clients)`.
4. Add `handler` to `clients`.
5. Start `new Thread(handler)`.
6. Log the new connection and current client count.

---

#### `getCurrentTime()` — `public static String`

| | |
|---|---|
| **Input** | none |
| **Output** | `String` — current time formatted as `HH:mm:ss` |

---

#### `main(String[] args)` — `public static void`

Entry point. Creates a `SecureMultiServer` instance and calls `start()`.

---

## Class: `SecureClientHandler`

**File:** `SecureMultiServer.java` (inner class in the same file)
**Implements:** `Runnable`
**Purpose:** Manages the full lifecycle for a single connected client — RSA handshake, AES messaging, broadcast, and cleanup.

### Fields

| Field | Type | Description |
|---|---|---|
| `socket` | `Socket` | The client's TCP socket |
| `keyPair` | `KeyPair` | Reference to the server's RSA key pair (shared, read-only) |
| `symmetricKey` | `SecretKey` | AES key negotiated with **this specific client** |
| `allClients` | `Set<SecureClientHandler>` | Shared set used for broadcasting |
| `clientName` | `String` | Auto-generated from the client's remote address (e.g. `Client_127.0.0.1_54321`) |
| `running` | `boolean` | Controls the receive loop; set to `false` to stop gracefully |
| `timeFormatter` | `static DateTimeFormatter` | Shared formatter for log timestamps |

### Constructor: `SecureClientHandler(Socket socket, KeyPair keyPair, Set<SecureClientHandler> allClients)`

| Parameter | Type | Description |
|---|---|---|
| `socket` | `Socket` | Accepted client socket |
| `keyPair` | `KeyPair` | Server's RSA key pair |
| `allClients` | `Set<SecureClientHandler>` | The shared clients set |

Stores all parameters and derives `clientName` from `socket.getRemoteSocketAddress()`.

> **Tip:** Derive `clientName` the same way as in `Multi_Client_Single_Server`: `"Client_" + socket.getRemoteSocketAddress().toString().substring(1).replace(":", "_")`.

### Methods

---

#### `run()` — `public void` (from `Runnable`)

The full per-client lifecycle.

| | |
|---|---|
| **Input** | none (uses instance fields) |
| **Output** | `void` |

**Steps:**
1. Call `sendPublicKey()` — sends RSA public key to the client.
2. Call `receiveAndDecryptSymmetricKey()` — stores AES key in `this.symmetricKey`.
3. If either step fails → close connection and return.
4. Send an encrypted welcome message: `sendEncryptedMessage("Welcome to Secure Group Chat! Type 'exit' to leave.")`.
5. Call `broadcastEncryptedMessage(clientName + " has joined the chat.", this)`.
6. Enter the main receive loop:
   - Call `receiveAndDecryptMessage()` to get the next plaintext message.
   - If `null` → client disconnected, break.
   - If `"exit"` (case-insensitive) → break.
   - Log the message on the server console.
   - Call `broadcastEncryptedMessage("[" + clientName + "] " + message, this)`.
7. Broadcast that the client has left.
8. Call `closeConnection()` and remove `this` from `allClients`.

---

#### `sendPublicKey()` — `private void`

Serialises the RSA public key and sends it length-prefixed to the client.

| | |
|---|---|
| **Input** | none (uses `this.socket`, `this.keyPair`) |
| **Output** | `void` |
| **Throws** | `IOException` — propagated to caller |

**Wire format:**
```
[4 bytes: int length][length bytes: DER-encoded public key]
```

**Steps:**
1. `byte[] publicKeyBytes = keyPair.getPublic().getEncoded()`
2. `DataOutputStream out = new DataOutputStream(socket.getOutputStream())`
3. `out.writeInt(publicKeyBytes.length)` — sends 4-byte length
4. `out.write(publicKeyBytes)` — sends key bytes
5. `out.flush()`

---

#### `receiveAndDecryptSymmetricKey()` — `private SecretKey`

Reads the RSA-encrypted AES key from the client and decrypts it.

| | |
|---|---|
| **Input** | none (uses `this.socket`, `this.keyPair`) |
| **Output** | `SecretKey` — the decrypted AES key, or `null` on failure |
| **Throws** | `IOException` — propagated to caller |

**Wire format (what client sends):**
```
[4 bytes: int length][length bytes: RSA-encrypted AES key bytes]
```

**Steps:**
1. `DataInputStream in = new DataInputStream(socket.getInputStream())`
2. `int encryptedKeyLength = in.readInt()`
3. `byte[] encryptedKey = new byte[encryptedKeyLength]; in.readFully(encryptedKey)`
4. `Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION)`
5. `cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate())`
6. `byte[] decryptedKeyBytes = cipher.doFinal(encryptedKey)`
7. `return new SecretKeySpec(decryptedKeyBytes, AES_ALGORITHM)`

---

#### `receiveAndDecryptMessage()` — `private String`

Reads one AES-encrypted message from the client and returns the plaintext.

| | |
|---|---|
| **Input** | none (uses `this.socket`, `this.symmetricKey`) |
| **Output** | `String` — plaintext message, or `null` if the connection closed or decryption failed |
| **Throws** | `IOException` caught internally |

**Wire format (what client sends):**
```
[4 bytes: int length][length bytes: AES-encrypted UTF-8 message]
```

**Steps:**
1. `DataInputStream in = new DataInputStream(socket.getInputStream())`
2. `int len = in.readInt()`
3. `byte[] encrypted = new byte[len]; in.readFully(encrypted)`
4. `Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION)`
5. `cipher.init(Cipher.DECRYPT_MODE, symmetricKey)`
6. `byte[] plain = cipher.doFinal(encrypted)`
7. `return new String(plain, "UTF-8")`

---

#### `sendEncryptedMessage(String message)` — `private synchronized void`

Encrypts `message` with this client's AES key and sends it length-prefixed.

| Parameter | Type | Description |
|---|---|---|
| `message` | `String` | Plaintext to send |

| | |
|---|---|
| **Output** | `void` |
| **Throws** | catches all exceptions internally; calls `closeConnection()` on failure |

> **Why `synchronized`?** The broadcast method iterates over all clients and calls `sendEncryptedMessage` on each. Making it `synchronized` prevents two threads from writing to the same client's socket output stream at the same time (same reason as in the plain `MultiServer`).

**Wire format (what server sends):**
```
[4 bytes: int length][length bytes: AES-encrypted UTF-8 message]
```

**Steps:**
1. `Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION)`
2. `cipher.init(Cipher.ENCRYPT_MODE, symmetricKey)`
3. `byte[] encrypted = cipher.doFinal(message.getBytes("UTF-8"))`
4. `DataOutputStream out = new DataOutputStream(socket.getOutputStream())`
5. `out.writeInt(encrypted.length)`
6. `out.write(encrypted)`
7. `out.flush()`

---

#### `broadcastEncryptedMessage(String message, SecureClientHandler sender)` — `private void`

Sends `message` to every connected client **except** the sender.

| Parameter | Type | Description |
|---|---|---|
| `message` | `String` | Plaintext message to broadcast |
| `sender` | `SecureClientHandler` | The client who sent it (excluded from the broadcast); pass `null` for server announcements |

| | |
|---|---|
| **Output** | `void` |

**Key insight:** Each recipient has its own `symmetricKey`. The loop calls `client.sendEncryptedMessage(message)` for each, so each copy is **independently encrypted** with that recipient's AES key. The plaintext is the same; the ciphertext is different for each recipient.

**Steps:**
```java
synchronized (allClients) {
    for (SecureClientHandler client : allClients) {
        if (sender == null || !client.equals(sender)) {
            client.sendEncryptedMessage(message);
        }
    }
}
```

---

#### `closeConnection()` — `private void`

Closes the client socket.

| | |
|---|---|
| **Input** | none |
| **Output** | `void` |

---

#### `getCurrentTime()` — `public static String`

| | |
|---|---|
| **Input** | none |
| **Output** | `String` — current time as `HH:mm:ss` |

---

## Class: `SecureMultiClient`

**File:** `SecureMultiClient.java`
**Purpose:** Connects to the server, performs the RSA/AES handshake, starts a background thread to receive broadcast messages, and lets the user type messages to send.

### Constants / Fields

| Field | Type | Value | Description |
|---|---|---|---|
| `HOST` | `String` | `"localhost"` | Server hostname |
| `PORT` | `int` | `8888` | Server port |
| `RSA_ALGORITHM` | `String` | `"RSA"` | Same as server |
| `RSA_TRANSFORMATION` | `String` | `"RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING"` | Same as server |
| `AES_ALGORITHM` | `String` | `"AES"` | Same as server |
| `AES_TRANSFORMATION` | `String` | `"AES/ECB/PKCS5Padding"` | Same as server |
| `AES_KEY_SIZE` | `int` | `256` | AES key length in bits |
| `publicKey` | `PublicKey` | — | Server's RSA public key (received during handshake) |
| `symmetricKey` | `SecretKey` | — | This client's AES key |
| `socket` | `Socket` | — | TCP connection to the server |
| `running` | `boolean` | `true` | Controls background listener; set to `false` to stop |

### Methods

---

#### `receivePublicKey()` — `private PublicKey`

Reads and reconstructs the server's RSA public key.

| | |
|---|---|
| **Input** | none (uses `this.socket`) |
| **Output** | `PublicKey` on success, `null` on failure |
| **Throws** | `IOException` — propagated to caller |

**Steps:**
1. `DataInputStream in = new DataInputStream(socket.getInputStream())`
2. `int len = in.readInt()`
3. `byte[] keyBytes = new byte[len]; in.readFully(keyBytes)`
4. `X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes)`
5. `KeyFactory factory = KeyFactory.getInstance(RSA_ALGORITHM)`
6. `return factory.generatePublic(spec)`

---

#### `generateAndSendSymmetricKey()` — `private boolean`

Generates a fresh 256-bit AES key, encrypts it with the server's RSA public key, and sends it.

| | |
|---|---|
| **Input** | none (uses `this.socket`, `this.publicKey`) |
| **Output** | `true` on success, `false` on failure |

**Steps:**
1. `KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM)`
2. `keyGen.init(AES_KEY_SIZE)`
3. `this.symmetricKey = keyGen.generateKey()`
4. `Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION)`
5. `cipher.init(Cipher.ENCRYPT_MODE, publicKey)`
6. `byte[] encryptedKey = cipher.doFinal(symmetricKey.getEncoded())`
7. `DataOutputStream out = new DataOutputStream(socket.getOutputStream())`
8. `out.writeInt(encryptedKey.length)`
9. `out.write(encryptedKey); out.flush()`

---

#### `sendEncryptedMessage(String message)` — `private boolean`

Encrypts `message` with `this.symmetricKey` and sends it to the server.

| Parameter | Type | Description |
|---|---|---|
| `message` | `String` | Plaintext chat message |

| | |
|---|---|
| **Output** | `true` on success, `false` on failure |

**Steps:**
1. `Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION)`
2. `cipher.init(Cipher.ENCRYPT_MODE, symmetricKey)`
3. `byte[] encrypted = cipher.doFinal(message.getBytes("UTF-8"))`
4. `DataOutputStream out = new DataOutputStream(socket.getOutputStream())`
5. `out.writeInt(encrypted.length)`
6. `out.write(encrypted); out.flush()`

---

#### `receiveAndDecryptMessage()` — `private String`

Reads one AES-encrypted message from the server and returns the plaintext.

| | |
|---|---|
| **Input** | none (uses `this.socket`, `this.symmetricKey`) |
| **Output** | `String` plaintext, or `null` if the connection closed or an error occurred |

**Steps:**
Mirror of `SecureClientHandler.receiveAndDecryptMessage()` but reads from `this.socket`.

---

#### `listenForMessages()` — `private void`

Starts a **daemon thread** that continuously calls `receiveAndDecryptMessage()` and prints received messages to the console.

| | |
|---|---|
| **Input** | none |
| **Output** | `void` — launches a background thread as a side effect |

**Steps:**
1. Create `Thread listenThread = new Thread(() -> { ... })`.
2. Inside the lambda: loop while `running` is `true`.
3. Call `receiveAndDecryptMessage()`. If `null` → server disconnected, set `running = false`, break.
4. Print `"\n[SERVER/other client]: " + message`.
5. `listenThread.setDaemon(true)` — so the JVM can exit when the main thread finishes.
6. `listenThread.start()`.

> **Compare with `Multi_Client_Single_Server`:** The `MessageReceiver` runnable there reads raw bytes. Here you replace that raw read with a call to `receiveAndDecryptMessage()` — the rest of the logic is identical.

---

#### `connect()` — `public void`

Top-level method that runs the full client lifecycle.

| | |
|---|---|
| **Input** | none |
| **Output** | `void` — blocks until user types `exit` or the connection drops |

**Steps:**
1. `socket = new Socket(HOST, PORT)`
2. `publicKey = receivePublicKey()` — if `null`, close and return.
3. `generateAndSendSymmetricKey()` — if `false`, close and return.
4. Print "Secure connection established."
5. Call `listenForMessages()` — starts the background receive thread.
6. Main loop with `Scanner(System.in)`:
   - Read a line.
   - Call `sendEncryptedMessage(line)`.
   - If `"exit"` (case-insensitive) → set `running = false`, break.
7. `finally` block: close `socket`.

---

#### `main(String[] args)` — `public static void`

Entry point. Creates a `SecureMultiClient` and calls `connect()`.

---

## Class: `SecureMessageReceiver` *(optional alternative)*

**File:** `SecureMultiClient.java` (same file)
**Implements:** `Runnable`

You can extract the listener lambda from `listenForMessages()` into a separate `Runnable` class — the same pattern used in `Multi_Client_Single_Server/Ayberk/MultiClient.java` with `MessageReceiver`.

### Fields

| Field | Type | Description |
|---|---|---|
| `symmetricKey` | `SecretKey` | AES key to decrypt incoming messages |
| `socket` | `Socket` | Connection to the server |
| `running` | reference / `AtomicBoolean` | Shared flag to signal the outer client to stop |

### `run()` — `public void`

Same logic as the lambda in `listenForMessages()`: loop, decrypt, print, stop on `null` or `"exit"`.

---

## Wire Protocol Summary

All messages after the handshake follow this exact framing (both directions):

```
┌──────────────────────────────────────────────────────┐
│  4 bytes (int, big-endian)  │  N bytes                │
│  N = length of payload      │  AES-encrypted UTF-8    │
└──────────────────────────────────────────────────────┘
```

Using `DataInputStream.readInt()` / `DataOutputStream.writeInt()` handles the 4-byte length automatically and correctly across platforms.

---

## Step-by-Step Implementation Checklist

- [ ] Create `SecureMultiServer.java`
  - [ ] Copy constant definitions from `SecureServer.java` + add `clients` set from `MultiServer.java`
  - [ ] Implement `generateKeyPair()` (identical to `SecureServer`)
  - [ ] Implement `start()` — accept loop spawning `SecureClientHandler` threads
  - [ ] Implement `SecureClientHandler` — merge `ClientHandler` logic with RSA/AES methods from `SecureServer`
    - [ ] `sendPublicKey()`
    - [ ] `receiveAndDecryptSymmetricKey()`
    - [ ] `receiveAndDecryptMessage()`
    - [ ] `sendEncryptedMessage()` — mark `synchronized`
    - [ ] `broadcastEncryptedMessage()` — replaces plain `broadcastMessage()`
    - [ ] `run()` — handshake then chat loop
    - [ ] `closeConnection()`

- [ ] Create `SecureMultiClient.java`
  - [ ] Copy constant definitions from `SecureClient.java`
  - [ ] Implement `receivePublicKey()` (identical to `SecureClient`)
  - [ ] Implement `generateAndSendSymmetricKey()` (identical to `SecureClient`)
  - [ ] Implement `sendEncryptedMessage()` (identical to `SecureClient`)
  - [ ] Implement `receiveAndDecryptMessage()` (identical to `SecureClient`)
  - [ ] Implement `listenForMessages()` — replace raw-byte read with `receiveAndDecryptMessage()`
  - [ ] Implement `connect()` — handshake then send loop

- [ ] Test
  - [ ] Start `SecureMultiServer`
  - [ ] Connect two or more `SecureMultiClient` instances
  - [ ] Verify broadcast messages arrive at all other clients
  - [ ] Use a network sniffer (Wireshark) to confirm the payload is ciphertext

---

## Common Mistakes to Avoid

| Mistake | Fix |
|---|---|
| Forgetting `out.flush()` after writes | Always call `flush()` on `DataOutputStream` |
| Reading from a new `DataInputStream` each time | Wrap once and reuse, or use `socket.getInputStream()` directly — but be consistent |
| Not synchronizing `sendEncryptedMessage` | Two threads can write to the same socket concurrently during broadcast; `synchronized` prevents corruption |
| Re-using the same `Cipher` object from multiple threads | Always create a new `Cipher` instance per operation — `Cipher` is **not** thread-safe |
| Broadcasting the `keyPair` object to the handler instead of the `KeyPair` | Only the private key is needed to decrypt; never expose the private key beyond the server |
| Using a plain `byte[]` buffer read instead of `DataInputStream.readFully()` | A single `read()` may not return all bytes; `readFully()` guarantees the full payload is received |
