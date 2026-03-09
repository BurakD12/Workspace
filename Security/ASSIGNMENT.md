# Secure Multi-Client Chat Application — Programming Assignment

## Overview

In this assignment you will build a **secure, socket-based, multi-client chat application** in Java. A single server acts as a relay hub; multiple clients connect to it and exchange messages. Every message must satisfy the three pillars of information security — **Confidentiality, Integrity, and Authentication** — using the cryptographic primitives you have already studied: Symmetric Encryption (AES), Asymmetric Encryption (RSA), Message Digest (SHA-256), and Digital Signatures (SHA256withRSA).

No code or solution is provided. You are expected to design and implement every class and method described below.

---

## Learning Objectives

- Apply hybrid encryption (symmetric + asymmetric) in a real networked system.
- Distribute and manage public keys at runtime without a pre-shared secret.
- Guarantee message integrity with a cryptographic digest and authenticity with a digital signature.
- Handle concurrent client connections using threads.
- Design a clean, serializable message protocol.

---

## Security Requirements

Your implementation **must** satisfy all of the following properties for every message transmitted over the network.

| Property | Mechanism | Notes |
|---|---|---|
| **Confidentiality** | Hybrid encryption | Message body encrypted with a fresh AES-256 key; that key wrapped (encrypted) with the receiver's RSA-2048 public key |
| **Integrity** | Message Digest (SHA-256) | Digest computed over the plaintext; receiver recomputes and compares |
| **Authentication** | Digital Signature (SHA256withRSA) | Sender signs the plaintext with their RSA private key; receiver verifies with sender's public key |
| **Key Distribution** | Public key broadcast | On connection, each client's public key is distributed to the server and all currently connected clients |

> **Important:** The server must never see the plaintext of any message. It only routes ciphertext payloads between clients.

---

## System Architecture

```
          ┌──────────────────────────────────────────────────────┐
          │                        SERVER                        │
          │  ┌────────────────┐    ┌──────────────────────────┐  │
          │  │  ClientHandler │    │  PublicKeyRegistry       │  │
          │  │  (one per      │    │  username → PublicKey    │  │
          │  │   connection)  │    └──────────────────────────┘  │
          │  └────────────────┘                                  │
          └──────────────────────────────────────────────────────┘
                  ▲ Socket           ▲ Socket          ▲ Socket
                  │                  │                  │
          ┌───────┴───────┐  ┌───────┴──────┐  ┌───────┴──────┐
          │   Client A    │  │   Client B   │  │   Client C   │
          │ (KeyPair, UI) │  │ (KeyPair, UI)│  │ (KeyPair, UI)│
          └───────────────┘  └──────────────┘  └──────────────┘
```

**Communication flows:**

1. **Key Exchange** — When a client connects it sends its username and public key to the server. The server stores it and broadcasts it to all other connected clients. The server also sends the new client all public keys it already holds.
2. **Broadcast** — A client encrypts the message *individually* for each other connected client (hybrid encryption), bundles all per-recipient encrypted key slots together, signs the entire payload, and sends it to the server. The server forwards each recipient's slice to the correct client.
3. **Whisper** — Like broadcast but targeted at a single recipient. Only that recipient receives and can decrypt the message.

---

## Message Protocol

All objects sent over the sockets must implement `java.io.Serializable`.

### `MessageType` (enum)

Defines the type of every object exchanged over the network.

```
Values: KEY_EXCHANGE, KEY_DISTRIBUTION, BROADCAST, WHISPER, SERVER_NOTIFICATION
```

| Value | Direction | Purpose |
|---|---|---|
| `KEY_EXCHANGE` | Client → Server | Client registers username and public key |
| `KEY_DISTRIBUTION` | Server → Client | Server distributes a (possibly new) peer's public key |
| `BROADCAST` | Client → Server → Clients | Encrypted broadcast message |
| `WHISPER` | Client → Server → Client | Encrypted private message |
| `SERVER_NOTIFICATION` | Server → Client | Informational text (e.g., "User X has joined") |

---

### `KeyExchangeMessage` (implements `Serializable`)

Sent by a newly connected client to announce its identity and public key.

**Fields:**

| Field | Type | Description |
|---|---|---|
| `type` | `MessageType` | Always `KEY_EXCHANGE` |
| `username` | `String` | The connecting client's chosen username |
| `publicKeyBytes` | `byte[]` | The client's RSA public key in encoded form (`PublicKey.getEncoded()`) |

**Methods:**

- `KeyExchangeMessage(String username, PublicKey publicKey)`
  Constructor. Stores username and converts the `PublicKey` to its byte-encoded form for serialization.

- `String getUsername()`
  Returns the username.

- `PublicKey getPublicKey()`
  Reconstructs and returns a `PublicKey` object from the stored byte array using `KeyFactory` and `X509EncodedKeySpec`. Throws an appropriate exception if reconstruction fails.

---

### `KeyDistributionMessage` (implements `Serializable`)

Sent by the server to existing clients to announce a peer's public key, or to the new client to deliver all currently registered keys.

**Fields:**

| Field | Type | Description |
|---|---|---|
| `type` | `MessageType` | Always `KEY_DISTRIBUTION` |
| `username` | `String` | The peer whose public key is being distributed |
| `publicKeyBytes` | `byte[]` | RSA public key bytes of that peer |

**Methods:**

- `KeyDistributionMessage(String username, PublicKey publicKey)`
  Constructor. Encodes the public key to bytes.

- `String getUsername()`

- `PublicKey getPublicKey()`
  Same reconstruction logic as in `KeyExchangeMessage`.

---

### `SecureMessage` (implements `Serializable`)

The main payload for both broadcast and whisper messages.

**Fields:**

| Field | Type | Description |
|---|---|---|
| `type` | `MessageType` | `BROADCAST` or `WHISPER` |
| `senderUsername` | `String` | Who sent this message |
| `targetUsername` | `String` | Only used for `WHISPER`; `null` for broadcast |
| `ciphertext` | `byte[]` | The AES-encrypted message body |
| `encryptedKeys` | `Map<String, byte[]>` | Maps each recipient's username to the AES secret key encrypted with *that* recipient's RSA public key |
| `digest` | `byte[]` | SHA-256 digest of the original plaintext |
| `digitalSignature` | `byte[]` | RSA digital signature over the original plaintext, produced by the sender's private key |

**Methods:**

- `SecureMessage(MessageType type, String senderUsername, String targetUsername, byte[] ciphertext, Map<String, byte[]> encryptedKeys, byte[] digest, byte[] digitalSignature)`
  Full constructor.

- Getters for all fields.

- `byte[] getEncryptedKeyFor(String username)`
  Convenience method that returns `encryptedKeys.get(username)`, i.e., the AES key slot intended for the given recipient.

---

### `ServerNotification` (implements `Serializable`)

A plain text notification sent by the server (e.g., join/leave events).

**Fields:**

| Field | Type | Description |
|---|---|---|
| `type` | `MessageType` | Always `SERVER_NOTIFICATION` |
| `message` | `String` | Human-readable text |

**Methods:**

- `ServerNotification(String message)` — Constructor.
- `String getMessage()` — Returns the notification text.

---

## Cryptographic Utility Class

### `CryptoUtils`

A stateless utility class containing only `static` methods. No instance should ever be created.

**RSA Key Generation**

- `static KeyPair generateRSAKeyPair()`
  Generates a 2048-bit RSA key pair using `KeyPairGenerator`. Returns the `KeyPair` containing both the public and private key. Uses `SecureRandom` for entropy.

**AES Key Generation**

- `static SecretKey generateAESKey()`
  Generates a 256-bit AES secret key using `javax.crypto.KeyGenerator`.

**Symmetric Encryption / Decryption**

- `static byte[] encryptAES(byte[] plaintext, SecretKey key)`
  Encrypts the given plaintext bytes with the provided AES key. Uses `Cipher.getInstance("AES")` initialized in `ENCRYPT_MODE`.

- `static byte[] decryptAES(byte[] ciphertext, SecretKey key)`
  Decrypts the given ciphertext with the provided AES key. Uses `Cipher` in `DECRYPT_MODE`.

**Symmetric Key Wrapping and Unwrapping (using RSA)**

- `static byte[] wrapAESKey(SecretKey aesKey, PublicKey rsaPublicKey)`
  Encrypts (wraps) the raw bytes of the AES secret key (`aesKey.getEncoded()`) using the receiver's RSA public key and `Cipher.getInstance("RSA")`. Returns the encrypted key bytes.

- `static SecretKey unwrapAESKey(byte[] encryptedKeyBytes, PrivateKey rsaPrivateKey)`
  Decrypts the encrypted key bytes using the receiver's RSA private key, then reconstructs a `SecretKey` from the raw bytes using `new SecretKeySpec(rawKeyBytes, "AES")`.

**Message Digest**

- `static byte[] computeDigest(byte[] data)`
  Computes and returns the SHA-256 hash of `data` using `java.security.MessageDigest`.

- `static boolean verifyDigest(byte[] data, byte[] expectedDigest)`
  Recomputes the SHA-256 hash of `data` and compares it byte-by-byte with `expectedDigest`. Returns `true` only if they are identical. Use `Arrays.equals` for a constant-time comparison.

**Digital Signature**

- `static byte[] sign(byte[] data, PrivateKey privateKey)`
  Signs `data` with the sender's RSA private key using `Signature.getInstance("SHA256withRSA")`. Calls `initSign`, `update`, and `sign` in sequence.

- `static boolean verify(byte[] data, byte[] signature, PublicKey senderPublicKey)`
  Verifies the signature against the data using the sender's public key. Uses `Signature` initialized with `initVerify`, then `update` and `verify`. Returns `true` if valid.

---

## Server-Side Classes

### `Server`

The main server class. Owns a `ServerSocket`, accepts incoming connections, and manages all connected clients.

**Fields (suggested):**

- `ServerSocket serverSocket` — Listening socket.
- `Map<String, ClientHandler> connectedClients` — Maps username to its handler thread. Must be thread-safe (use `ConcurrentHashMap`).
- `Map<String, PublicKey> publicKeyRegistry` — Maps username to public key. Must be thread-safe.

**Methods:**

- `void start(int port)`
  Opens a `ServerSocket` on the given port and enters an infinite accept loop. For each accepted `Socket`, spawns a new `ClientHandler` thread and starts it.

- `void registerClient(String username, PublicKey publicKey, ClientHandler handler)`
  Adds the client to both maps. Then calls `broadcastPublicKeyToAll(username, publicKey)` to inform all *other* connected clients. Also calls `sendExistingKeys(handler)` to deliver all currently held public keys to the new client.

- `void sendExistingKeys(ClientHandler newClientHandler)`
  Iterates over `publicKeyRegistry` and sends a `KeyDistributionMessage` for each known peer to the new client handler.

- `void broadcastPublicKeyToAll(String username, PublicKey publicKey)`
  Sends a `KeyDistributionMessage` to every connected client *except* the one being registered.

- `void routeBroadcast(SecureMessage message)`
  Iterates over all connected clients except the sender, retrieves the per-recipient slice of the `SecureMessage` (using `getEncryptedKeyFor`), and calls `sendToClient` for each. The full `SecureMessage` object is forwarded as-is — the server does **not** decrypt anything.

- `void routeWhisper(SecureMessage message)`
  Looks up the `targetUsername` in `connectedClients` and forwards the `SecureMessage` to that handler only. If the target is not found, sends a `ServerNotification` back to the sender indicating the user is offline.

- `void removeClient(String username)`
  Removes the client from both maps and broadcasts a `ServerNotification` to all remaining clients announcing the disconnection.

- `PublicKey getPublicKey(String username)`
  Returns the stored public key for the given username, or `null` if not found.

---

### `ClientHandler`

A `Runnable` (or `Thread` subclass) created by the server — one per connected client. Owns the I/O streams for one socket connection.

**Fields (suggested):**

- `Socket socket` — The client's socket.
- `ObjectInputStream in` — Deserialization stream.
- `ObjectOutputStream out` — Serialization stream.
- `Server server` — Reference to the owning server for routing.
- `String username` — Set after the initial key exchange.

**Methods:**

- `void run()`
  The main loop. Reads objects from `in` in a loop. Dispatches each object to the appropriate handler method based on its `MessageType`. Catches `IOException` and `ClassNotFoundException`; on error, calls `server.removeClient(username)` and closes the socket.

- `void handleKeyExchange(KeyExchangeMessage msg)`
  Extracts the username and public key from the message. Calls `server.registerClient(...)` to complete registration.

- `void handleBroadcast(SecureMessage msg)`
  Delegates to `server.routeBroadcast(msg)`.

- `void handleWhisper(SecureMessage msg)`
  Delegates to `server.routeWhisper(msg)`.

- `void sendMessage(Object obj)`
  Writes `obj` to `out` using `writeObject` and flushes. Must be `synchronized` to prevent concurrent write corruption if multiple threads call this method for the same client.

- `void close()`
  Gracefully closes `in`, `out`, and `socket`.

---

## Client-Side Classes

### `Client`

The main client class. Manages connection to the server, the local RSA key pair, the peer public key registry, and user interaction.

**Fields (suggested):**

- `Socket socket`
- `ObjectInputStream in`
- `ObjectOutputStream out`
- `String username`
- `KeyPair keyPair` — The client's own RSA key pair.
- `Map<String, PublicKey> peerPublicKeys` — Stores public keys received from the server for all other clients.

**Methods:**

- `void connect(String serverHost, int serverPort, String username)`
  Opens a socket to the server, initialises the I/O streams, generates an RSA key pair via `CryptoUtils.generateRSAKeyPair()`, sends a `KeyExchangeMessage` to the server, and starts a `ClientReceiver` thread to listen for incoming messages.

- `void broadcast(String plaintext)`
  Constructs a `SecureMessage` of type `BROADCAST`. Must:
  1. Generate a fresh AES key with `CryptoUtils.generateAESKey()`.
  2. Encrypt `plaintext` bytes with that AES key.
  3. Compute the SHA-256 digest of the plaintext.
  4. Sign the plaintext with the client's own RSA private key.
  5. For **each** username in `peerPublicKeys`, wrap the AES key with that peer's public key and add it to the `encryptedKeys` map.
  6. Send the assembled `SecureMessage` to the server via `sendToServer`.

- `void whisper(String targetUsername, String plaintext)`
  Constructs a `SecureMessage` of type `WHISPER`. Must:
  1. Look up `targetUsername` in `peerPublicKeys`. Abort with an error message if the key is not found.
  2. Generate a fresh AES key.
  3. Encrypt the plaintext with the AES key.
  4. Compute the SHA-256 digest.
  5. Sign the plaintext with the client's private key.
  6. Wrap the AES key with the target's public key only (single entry in `encryptedKeys`).
  7. Send the `SecureMessage` to the server.

- `void receiveSecureMessage(SecureMessage msg)`
  Called by `ClientReceiver` when a `SecureMessage` arrives. Must:
  1. Retrieve the encrypted AES key slot for this client: `msg.getEncryptedKeyFor(this.username)`.
  2. Unwrap the AES key using this client's RSA private key via `CryptoUtils.unwrapAESKey(...)`.
  3. Decrypt the ciphertext using the recovered AES key.
  4. Verify the SHA-256 digest of the decrypted plaintext against `msg.getDigest()`. If verification fails, discard the message and warn the user of a potential integrity violation.
  5. Look up the sender's public key from `peerPublicKeys`. Verify the digital signature against the plaintext. If verification fails, discard the message and warn of a potential authentication failure.
  6. Only if both digest and signature are valid, display the message to the user.

- `void receiveKeyDistribution(KeyDistributionMessage msg)`
  Called by `ClientReceiver` when a peer's public key arrives. Stores `msg.getPublicKey()` in `peerPublicKeys` under `msg.getUsername()`. Optionally notifies the user that a new peer is available.

- `void sendToServer(Object obj)`
  Writes `obj` to the `out` stream and flushes.

- `void disconnect()`
  Closes streams and socket gracefully.

---

### `ClientReceiver`

A `Runnable` thread started by `Client` to continuously listen for incoming server messages without blocking user input.

**Fields:**

- `ObjectInputStream in` — Shared with the owning `Client`.
- `Client client` — Reference back to the owning client for dispatching.

**Methods:**

- `void run()`
  Loops indefinitely reading objects from `in`. For each received object, inspects its `MessageType` and dispatches:
  - `KEY_DISTRIBUTION` → calls `client.receiveKeyDistribution(...)`
  - `BROADCAST` or `WHISPER` → calls `client.receiveSecureMessage(...)`
  - `SERVER_NOTIFICATION` → prints the notification message to the console
  On `IOException` (e.g., server closed the connection), exits the loop and informs the user.

---

## Suggested Class Diagram Summary

```
Server
 ├── Map<String, ClientHandler>   connectedClients
 ├── Map<String, PublicKey>       publicKeyRegistry
 └── ClientHandler (1..* threads)
      └── (owns Socket in/out, dispatches to Server methods)

Client
 ├── KeyPair                      keyPair
 ├── Map<String, PublicKey>       peerPublicKeys
 └── ClientReceiver (1 thread)
      └── dispatches incoming objects back to Client

CryptoUtils  (all static)
MessageType  (enum)
KeyExchangeMessage      implements Serializable
KeyDistributionMessage  implements Serializable
SecureMessage           implements Serializable
ServerNotification      implements Serializable
```

---

## Message Flow Diagrams

### Key Exchange on Connection

```
Client A                         Server                        Client B
   |                               |                               |
   |-- KeyExchangeMessage -------->|                               |
   |   (username=A, pubKey=A)      |-- store pubKey_A              |
   |                               |                               |
   |<- KeyDistributionMessage -----|  (if B already connected)     |
   |   (username=B, pubKey=B)      |-- KeyDistributionMessage ---->|
   |                               |   (username=A, pubKey=A)      |
```

### Broadcast

```
Client A                         Server                        Client B
   |                               |                               |
   |  [encrypt msg with AES_key]   |                               |
   |  [wrap AES_key with pubKey_B] |                               |
   |-- SecureMessage (BROADCAST) ->|                               |
   |   encryptedKeys={B:wrappedK}  |-- SecureMessage (BROADCAST) ->|
   |   ciphertext, digest, sig     |   (same object forwarded)     |
   |                               |                               |
   |                               |     [unwrap AES_key with privKey_B]
   |                               |     [decrypt ciphertext]
   |                               |     [verify digest]
   |                               |     [verify signature with pubKey_A]
```

### Whisper

```
Client A                         Server                        Client B
   |                               |                               |
   |  [encrypt msg with AES_key]   |                               |
   |  [wrap AES_key with pubKey_B] |                               |
   |-- SecureMessage (WHISPER) --->|                               |
   |   targetUsername=B            |-- SecureMessage (WHISPER) --->|
   |   encryptedKeys={B:wrappedK}  |   (routed to B only)         |
   |   ciphertext, digest, sig     |                               |
```

---

## Implementation Notes

### Threading

- The server must handle many clients concurrently. Each accepted connection must run in its own thread. Use `ConcurrentHashMap` for any map shared across handler threads to avoid race conditions.
- The client must run the receiver loop in a separate `Thread` so that the user can type commands while simultaneously receiving messages.

### Serialization

- Every object sent over an `ObjectOutputStream` must implement `java.io.Serializable`.
- `PublicKey`, `PrivateKey`, and `SecretKey` objects from the JCA are already serializable, but it is safer to transmit them as `byte[]` (encoded form) and reconstruct them on the receiver side — as demonstrated in your `AsymmetricEncryption` exercises.

### Key Sizes

- Use **RSA-2048** for asymmetric operations. The 1024-bit size used in your earlier exercises is considered insecure for production use.
- Use **AES-256** for symmetric encryption.

### Digest vs. Signature — Understanding the Difference

| | Message Digest | Digital Signature |
|---|---|---|
| **What it protects** | Integrity — detects accidental or malicious tampering with the ciphertext/plaintext | Authentication — proves the message was produced by the claimed sender |
| **Key used** | None (hash function only) | Sender's RSA **private** key to sign; verified with **public** key |
| **Can be forged by anyone?** | Yes — anyone can recompute a hash | No — only the private key holder can produce a valid signature |
| **Your implementation** | `CryptoUtils.computeDigest` / `verifyDigest` | `CryptoUtils.sign` / `verify` |

Both must be checked by the receiver before accepting any message.

### Hybrid Encryption — Why Two Keys?

RSA can only encrypt data smaller than its key size. A 2048-bit RSA key can encrypt at most ~245 bytes, making it unsuitable for encrypting arbitrary messages. The solution used in this assignment (and in real-world protocols like TLS and PGP) is **hybrid encryption**:

1. Generate a random, short-lived **AES-256 symmetric key** (the "session key").
2. Encrypt the message body with AES — fast and handles any message length.
3. Encrypt only the small AES key with RSA — slow but secure for small payloads.
4. The receiver decrypts the AES key with their RSA private key, then uses it to decrypt the message.

---

## Deliverables

1. All source files organised under the `src/SecureChat/` package (or equivalent structure).
2. The application must be runnable: start `Server` first, then start two or more `Client` instances in separate terminals.
3. A short **report** (plain text or PDF) explaining:
   - How each security property (CIA + digital signature + message digest) is achieved.
   - Why integrity verification alone is insufficient without authentication (and vice-versa).
   - What would break if the server could read the plaintext of messages.

---

## Grading Criteria

| Criterion | Weight |
|---|---|
| Correct key exchange and public key distribution | 20% |
| Working broadcast with proper hybrid encryption | 20% |
| Working whisper with proper hybrid encryption | 15% |
| Correct digest computation and verification | 15% |
| Correct digital signature and verification | 15% |
| Thread safety and graceful error handling | 10% |
| Report quality and understanding | 5% |

---

*Good luck. Remember: the goal is not just to make messages travel — it is to make sure that no one other than the intended recipient can read them, and that the recipient can be certain of who sent them and that nothing was altered in transit.*
