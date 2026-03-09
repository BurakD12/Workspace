# E2E Chat Server - Implementation Documentation

## Project Overview

This project implements a **multi-client chat server** using Java Socket Programming. The server supports multiple clients connecting simultaneously and provides various communication features including private messaging (whispering), broadcasting, user management, and command-based interactions.

## Requirements

### Functional Requirements

1. **Multi-Client Support**: The server must handle multiple clients connecting simultaneously using threads.

2. **Command-Based Communication**: The system must support the following commands:
   - `\w <username> <message>` - Send a private message (whisper) to a specific user
   - `\b <message>` - Broadcast a message to all connected users
   - `\list` or `\users` - List all currently online users
   - `\name <username>` - Change the client's display name
   - `\help` - Display available commands
   - `exit` - Disconnect from the server

3. **Default Behavior**: Messages that don't start with `\` should be automatically broadcast to all users.

4. **User Management**:
   - Each client must have a unique display name
   - Clients should be assigned a default name based on their IP address
   - Names can be changed using the `\name` command
   - The system must prevent duplicate names

5. **Thread Safety**: All shared data structures must be thread-safe to handle concurrent access from multiple client threads.

6. **Connection Management**: 
   - Proper cleanup when clients disconnect
   - Remove clients from active lists when they leave
   - Handle connection errors gracefully

### Technical Requirements

- Use Java Socket Programming (`java.net.ServerSocket`, `java.net.Socket`)
- Use `InputStream` and `OutputStream` for communication
- Implement multi-threading for handling multiple clients
- Use synchronized collections for thread safety
- Handle exceptions appropriately
- Use byte arrays for message transmission

---

## Architecture

The project consists of two main components:

1. **Server** (`Server.java`): Handles client connections and message routing
2. **Client** (`Client.java`): Connects to the server and sends/receives messages

---

## Class Documentation

### 1. Server Class

**Package**: `E2E_Though_Server`

**Purpose**: Main server class that accepts client connections and manages the chat system.

#### Attributes

| Attribute | Type | Access Modifier | Description |
|-----------|------|----------------|-------------|
| `PORT` | `int` | `private static final` | Port number on which the server listens (8888) |
| `clients` | `Set<ClientHandler>` | `private static` | Thread-safe set containing all connected client handlers |
| `clientMap` | `Map<String, ClientHandler>` | `private static` | Maps display names to client handlers for quick lookup |
| `timeFormatter` | `DateTimeFormatter` | `private static final` | Formats time as HH:mm:ss for logging |

#### Methods

##### `main(String[] args)`

**Purpose**: Entry point of the server application. Initializes the server socket and accepts incoming client connections.

**Input**: Command-line arguments (not used)

**Output**: None (void)

**Process**:
1. Creates a `ServerSocket` on the specified port
2. Prints server startup information and available commands
3. Enters an infinite loop to accept client connections
4. For each new connection:
   - Creates a `ClientHandler` instance
   - Adds it to the `clients` set
   - Starts a new thread to handle the client
5. Handles exceptions and ensures proper cleanup

**Key Operations**:
- `serverSocket.accept()` - Blocks until a client connects
- `new Thread(clientHandler).start()` - Starts a new thread for each client

##### `getCurrentTime()`

**Purpose**: Returns the current time formatted as HH:mm:ss.

**Input**: None

**Output**: `String` - Formatted time string

**Process**: Uses `LocalTime.now()` and formats it using `timeFormatter`.

---

### 2. ClientHandler Class

**Package**: `E2E_Though_Server`

**Purpose**: Handles individual client connections. Each instance runs in a separate thread and manages communication with one client.

**Implements**: `Runnable` interface to allow execution in a separate thread.

#### Attributes

| Attribute | Type | Access Modifier | Description |
|-----------|------|----------------|-------------|
| `socket` | `Socket` | `private` | The socket connection to the client |
| `inputStream` | `InputStream` | `private` | Input stream to read messages from the client |
| `outputStream` | `OutputStream` | `private` | Output stream to send messages to the client |
| `allClients` | `Set<ClientHandler>` | `private` | Reference to the set of all connected clients |
| `clientMap` | `Map<String, ClientHandler>` | `private` | Reference to the map of display names to clients |
| `clientName` | `String` | `private` | Default name based on IP address |
| `displayName` | `String` | `private` | Current display name (can be changed by user) |
| `timeFormatter` | `DateTimeFormatter` | `private static final` | Time formatter for logging |

#### Constructor

##### `ClientHandler(Socket socket, Set<ClientHandler> allClients, Map<String, ClientHandler> clientMap)`

**Purpose**: Initializes a new client handler with the client's socket and references to shared data structures.

**Input**:
- `socket`: The socket connection to the client
- `allClients`: Set of all connected clients
- `clientMap`: Map of display names to client handlers

**Output**: None (constructor)

**Process**:
1. Stores the socket and shared data structure references
2. Gets input and output streams from the socket
3. Generates a default client name from the IP address
4. Sets `displayName` to the default `clientName`

**Exception**: Throws `Exception` if socket operations fail

#### Methods

##### `run()`

**Purpose**: Main execution method called when the thread starts. Handles all communication with the client.

**Input**: None (implements `Runnable.run()`)

**Output**: None (void)

**Process**:
1. Sends welcome messages to the client
2. Registers the client in `clientMap` with default name
3. Broadcasts join message to all other clients
4. Enters a loop to read messages from the client:
   - Reads bytes from input stream
   - Converts to string and trims whitespace
   - Logs the message on server side
   - Processes "exit" command
   - Routes to command processor or broadcasts message
5. On exit or error:
   - Broadcasts leave message
   - Removes client from collections
   - Closes connection

**Key Operations**:
- `inputStream.read(buffer)` - Reads bytes from client (blocks until data available)
- `message.startsWith("\\")` - Checks if message is a command
- `processCommand(message)` - Handles command processing
- `broadcastMessage(...)` - Sends message to all other clients

##### `processCommand(String command)`

**Purpose**: Parses and executes client commands.

**Input**: 
- `command`: The command string from the client (e.g., "\\w Alice Hello")

**Output**: None (void)

**Process**:
1. Splits the command into parts using whitespace (max 3 parts)
2. Converts command to lowercase for case-insensitive matching
3. Uses switch statement to route to appropriate handler:
   - `\\w` or `\\whisper`: Calls `whisper()` method
   - `\\b` or `\\broadcast`: Calls `broadcastMessage()` with broadcast prefix
   - `\\list` or `\\users`: Calls `listUsers()` method
   - `\\help`: Calls `showHelp()` method
   - `\\name`: Calls `changeName()` method
   - Default: Sends error message for unknown commands

**Validation**: Checks for correct number of arguments for each command

##### `whisper(String targetUser, String message)`

**Purpose**: Sends a private message to a specific user.

**Input**:
- `targetUser`: Display name of the target user
- `message`: The message content to send

**Output**: None (void)

**Process**:
1. Looks up the target user in `clientMap`
2. Validates:
   - If target exists and is not the sender: sends message to both sender and receiver
   - If target is the sender: sends error (cannot whisper to self)
   - If target doesn't exist: sends error with suggestion to use `\list`
3. Logs the whisper on server side

**Key Operations**:
- `clientMap.get(targetUser)` - Retrieves target client handler
- `target.sendMessage(...)` - Sends message to target
- `this.sendMessage(...)` - Sends confirmation to sender

##### `listUsers()`

**Purpose**: Sends a list of all online users to the requesting client.

**Input**: None

**Output**: None (void)

**Process**:
1. Creates a `StringBuilder` to build the user list
2. Iterates through `allClients` set (synchronized)
3. For each client:
   - Appends display name
   - Marks current client as "(you)"
4. Appends total user count
5. Sends the formatted list to the client

**Thread Safety**: Uses `synchronized (allClients)` to prevent concurrent modification

##### `showHelp()`

**Purpose**: Displays available commands to the client.

**Input**: None

**Output**: None (void)

**Process**: 
1. Creates a formatted help string with all available commands
2. Sends the help message to the client using `sendMessage()`

##### `changeName(String newName)`

**Purpose**: Changes the client's display name.

**Input**:
- `newName`: The new display name requested by the client

**Output**: None (void)

**Process**:
1. Validates that the new name is not empty
2. Checks if the name is already taken by another client
3. If valid:
   - Removes old name from `clientMap`
   - Updates `displayName`
   - Adds new name to `clientMap`
   - Sends confirmation to client
   - Broadcasts name change to all other clients
4. If invalid:
   - Sends appropriate error message

**Key Operations**:
- `clientMap.remove(oldName)` - Removes old mapping
- `clientMap.put(displayName, this)` - Adds new mapping
- `broadcastMessage(...)` - Notifies all clients of name change

##### `sendMessage(String message)`

**Purpose**: Sends a message to this specific client.

**Input**:
- `message`: The message string to send

**Output**: None (void)

**Process**:
1. Converts message string to byte array
2. Writes bytes to `outputStream`
3. Flushes the stream to ensure immediate transmission
4. Handles exceptions and closes connection on error

**Thread Safety**: Method is `synchronized` to prevent concurrent writes to the same output stream

**Key Operations**:
- `message.getBytes()` - Converts string to bytes
- `outputStream.write(messageBytes)` - Writes bytes to stream
- `outputStream.flush()` - Forces data transmission

##### `broadcastMessage(String message, ClientHandler sender)`

**Purpose**: Sends a message to all connected clients except the sender.

**Input**:
- `message`: The message to broadcast
- `sender`: The client handler that sent the message (to exclude from broadcast)

**Output**: None (void)

**Process**:
1. Synchronizes on `allClients` set for thread safety
2. Iterates through all clients
3. For each client that is not the sender:
   - Calls `sendMessage()` to deliver the message

**Thread Safety**: Uses `synchronized (allClients)` to prevent concurrent modification during iteration

##### `closeConnection()`

**Purpose**: Closes the socket connection to the client.

**Input**: None

**Output**: None (void)

**Process**:
1. Checks if socket exists and is not already closed
2. Closes the socket
3. Handles any exceptions during closure

##### `getCurrentTime()`

**Purpose**: Returns the current time formatted as HH:mm:ss.

**Input**: None

**Output**: `String` - Formatted time string

**Process**: Uses `LocalTime.now()` and formats it using `timeFormatter`.

---

### 3. Client Class

**Package**: `E2E_Though_Server`

**Purpose**: Client application that connects to the server and allows users to send/receive messages.

#### Attributes

| Attribute | Type | Access Modifier | Description |
|-----------|------|----------------|-------------|
| `SERVER_HOST` | `String` | `private static final` | Server hostname (localhost) |
| `SERVER_PORT` | `int` | `private static final` | Server port number (8888) |

#### Methods

##### `main(String[] args)`

**Purpose**: Entry point of the client application. Establishes connection and handles user input.

**Input**: Command-line arguments (not used)

**Output**: None (void)

**Process**:
1. Creates a socket connection to the server
2. Gets input and output streams from the socket
3. Creates and starts a `MessageReceiver` thread to handle incoming messages
4. Enters a loop to read user input from console:
   - Prompts user with "[YOU] > "
   - Reads line from scanner
   - Skips empty inputs
   - Converts to UTF-8 bytes and sends to server
   - Exits on "exit" command
5. Handles exceptions and ensures proper cleanup

**Key Operations**:
- `new Socket(SERVER_HOST, SERVER_PORT)` - Establishes connection
- `new Thread(new MessageReceiver(inputStream)).start()` - Starts receiver thread
- `scanner.nextLine()` - Reads user input
- `outputStream.write(message)` - Sends message to server

---

### 4. MessageReceiver Class

**Package**: `E2E_Though_Server`

**Purpose**: Handles incoming messages from the server in a separate thread, allowing the client to receive messages while the user is typing.

**Implements**: `Runnable` interface to run in a separate thread.

#### Attributes

| Attribute | Type | Access Modifier | Description |
|-----------|------|----------------|-------------|
| `inputStream` | `InputStream` | `private` | Input stream to read messages from the server |

#### Constructor

##### `MessageReceiver(InputStream inputStream)`

**Purpose**: Initializes the message receiver with the server's input stream.

**Input**:
- `inputStream`: The input stream connected to the server

**Output**: None (constructor)

**Process**: Stores the input stream reference.

#### Methods

##### `run()`

**Purpose**: Main execution method that continuously reads and displays messages from the server.

**Input**: None (implements `Runnable.run()`)

**Output**: None (void)

**Process**:
1. Enters a loop to read bytes from the input stream
2. Converts received bytes to a string using UTF-8 encoding
3. Displays the message to the console
4. Re-prompts the user with "[YOU] > "
5. Continues until the stream is closed (client disconnects or server closes connection)

**Key Operations**:
- `inputStream.read(buffer)` - Reads bytes from server (blocks until data available)
- `new String(messageBytes, "UTF-8")` - Converts bytes to string
- `System.out.print(...)` - Displays message
- `System.out.flush()` - Ensures output is displayed immediately

**Exception Handling**: Catches `IOException` when connection is closed

---

## Data Structures

### Collections Used

1. **`Set<ClientHandler> clients`**:
   - Type: `Collections.synchronizedSet(new HashSet<>())`
   - Purpose: Stores all connected client handlers
   - Thread Safety: Wrapped in `synchronizedSet` for thread-safe operations
   - Usage: Iterated when broadcasting messages or listing users

2. **`Map<String, ClientHandler> clientMap`**:
   - Type: `ConcurrentHashMap`
   - Purpose: Maps display names to client handlers for quick lookup
   - Thread Safety: `ConcurrentHashMap` is thread-safe
   - Usage: Used to find specific users for whispering

### Why These Collections?

- **SynchronizedSet**: Ensures thread-safe iteration and modification when multiple threads access the client set
- **ConcurrentHashMap**: Provides thread-safe key-value operations without explicit synchronization, better performance for concurrent access

---

## Command Protocol

### Command Format

All commands start with a backslash (`\`). The server parses commands by:
1. Splitting on whitespace (max 3 parts)
2. Converting the first part to lowercase
3. Routing to appropriate handler

### Command Details

| Command | Format | Description | Example |
|---------|--------|-------------|---------|
| Whisper | `\w <username> <message>` | Send private message | `\w Alice Hello there!` |
| Broadcast | `\b <message>` | Broadcast to all users | `\b Important announcement!` |
| List Users | `\list` or `\users` | Show online users | `\list` |
| Change Name | `\name <username>` | Change display name | `\name Bob` |
| Help | `\help` | Show commands | `\help` |
| Exit | `exit` | Disconnect | `exit` |

### Default Behavior

Messages that don't start with `\` are automatically broadcast to all users with the format: `[displayName] message`

---

## Thread Safety Considerations

### Synchronization Points

1. **`sendMessage()`**: Synchronized method to prevent concurrent writes to the same output stream
2. **`broadcastMessage()`**: Synchronizes on `allClients` set during iteration
3. **`listUsers()`**: Synchronizes on `allClients` set during iteration
4. **Collections**: Using thread-safe collections (`synchronizedSet`, `ConcurrentHashMap`)

### Why Thread Safety Matters

- Multiple client threads can access shared data simultaneously
- Without synchronization, race conditions could occur:
  - Messages could be corrupted
  - Clients could be added/removed while iterating
  - Name changes could cause inconsistencies

---

## Error Handling

### Server-Side Errors

1. **Connection Errors**: Caught in `main()` and `run()` methods
2. **I/O Errors**: Handled in `sendMessage()` and `closeConnection()`
3. **Invalid Commands**: Validated in `processCommand()` with error messages sent to client
4. **Name Conflicts**: Checked in `changeName()` method

### Client-Side Errors

1. **Connection Failures**: Caught in `main()` with appropriate error messages
2. **I/O Errors**: Handled in `MessageReceiver.run()` when connection closes

---

## Usage Example

### Starting the Server

```bash
cd SocketProgramming/src
javac E2E_Though_Server/Server.java
java E2E_Though_Server.Server
```

**Expected Output**:
```
[SERVER] Server started on port 8888
[SERVER] Waiting for clients to connect...
[SERVER] Available commands:
[SERVER]   \w <username> <message> - Send private message (whisper)
[SERVER]   \b <message> - Broadcast message to all users
...
```

### Starting a Client

```bash
cd SocketProgramming/src
javac E2E_Though_Server/Client.java
java E2E_Though_Server.Client
```

**Expected Output**:
```
[CLIENT] Connecting to server at localhost:8888
[CLIENT] Connected to server.
[CLIENT] Type '\help' to see available commands.

Welcome to E2E Chat Server!
Type '\help' to see available commands.
Set your name with '\name <username>' or start chatting!

[YOU] > 
```

### Example Session

```
[YOU] > \name Alice
[INFO] Your name changed from 'Client_127.0.0.1' to 'Alice'

[YOU] > Hello everyone!
[Alice] Hello everyone!

[YOU] > \list
[ONLINE USERS]
  - Alice (you)
  - Bob
[Total: 2 users]

[YOU] > \w Bob Hi Bob!
[WHISPER to Bob] Hi Bob!

[YOU] > \b Important: Meeting at 3pm
[BROADCAST from Alice] Important: Meeting at 3pm

[YOU] > exit
[CLIENT] Closing connection.
```

---

## Implementation Checklist

When implementing this project, ensure you:

- [ ] Create `Server` class with `main()` method
- [ ] Create `ClientHandler` class implementing `Runnable`
- [ ] Implement thread-safe collections for client management
- [ ] Implement command parsing in `processCommand()`
- [ ] Implement `whisper()` method for private messages
- [ ] Implement `broadcastMessage()` for broadcasting
- [ ] Implement `listUsers()` to show online users
- [ ] Implement `changeName()` with duplicate name checking
- [ ] Implement `showHelp()` to display commands
- [ ] Implement proper error handling and validation
- [ ] Create `Client` class with `main()` method
- [ ] Create `MessageReceiver` class implementing `Runnable`
- [ ] Implement proper connection cleanup
- [ ] Test with multiple clients simultaneously
- [ ] Test all commands and error cases

---

## Key Concepts to Understand

1. **Socket Programming**: Understanding `ServerSocket`, `Socket`, `InputStream`, `OutputStream`
2. **Multi-threading**: Using `Thread` and `Runnable` for concurrent client handling
3. **Thread Safety**: Synchronization, thread-safe collections
4. **I/O Operations**: Reading/writing bytes, converting to/from strings
5. **String Processing**: Parsing commands, splitting strings
6. **Exception Handling**: Try-catch blocks, proper resource cleanup
7. **Design Patterns**: One thread per client pattern

---

## Additional Notes

- The server uses port **8888** by default
- Messages are transmitted as UTF-8 encoded bytes
- Buffer size is **1024 bytes** for reading messages
- Default client names are generated from IP addresses
- The `MessageReceiver` thread is set as daemon to allow JVM exit
- All timestamps use 24-hour format (HH:mm:ss)

---

## Testing Recommendations

1. **Single Client Test**: Connect one client and test all commands
2. **Multi-Client Test**: Connect multiple clients and test broadcasting
3. **Whisper Test**: Test private messaging between specific users
4. **Name Change Test**: Test name changes and duplicate name prevention
5. **Concurrent Test**: Test multiple clients sending messages simultaneously
6. **Disconnect Test**: Test proper cleanup when clients disconnect
7. **Error Test**: Test invalid commands and error messages

---

## Conclusion

This implementation demonstrates a complete multi-client chat server with command-based communication, user management, and proper thread safety. Understanding each component's role and interactions is crucial for successful implementation.
