# Question 3 — Banking System (Hard)

**Difficulty:** Hard  
**Topic:** JDBC Transactions, Custom Exceptions, Connection Pooling (HikariCP), Multi-Table ACID Operations  
**Estimated Time:** 3–5 hours

---

## Project Description

Build a console-based **Banking System** that manages bank accounts and tracks every financial transaction (deposit, withdrawal, transfer). All money movement operations must be **ACID-compliant** — they use explicit transactions, and any failure causes a full rollback. The application uses **HikariCP connection pooling** instead of creating new connections for each operation.

This question introduces:
- A custom checked exception class
- Connection pooling via HikariCP
- Multi-row locking with `SELECT ... FOR UPDATE` to prevent race conditions during transfers
- Audit logging (every operation is recorded in a `transaction_log` table)

---

## Dependencies (add to `pom.xml`)

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
</dependency>

<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>
```

---

## Database Setup

**Step 1 — Create the database:**
```sql
CREATE DATABASE bank_db;
```

**Step 2 — Connect to `bank_db` and create tables (in this exact order):**

```sql
CREATE TABLE IF NOT EXISTS accounts (
    id             SERIAL PRIMARY KEY,
    account_number VARCHAR(20)    NOT NULL UNIQUE,
    holder_name    VARCHAR(100)   NOT NULL,
    balance        NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    account_type   VARCHAR(10)    NOT NULL CHECK (account_type IN ('CHECKING', 'SAVINGS')),
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS transaction_log (
    id                SERIAL PRIMARY KEY,
    transaction_type  VARCHAR(20)    NOT NULL,
    from_account_id   INTEGER        REFERENCES accounts(id),
    to_account_id     INTEGER        REFERENCES accounts(id),
    amount            NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    balance_after     NUMERIC(15, 2),
    description       TEXT,
    executed_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

> `from_account_id` is NULL for DEPOSIT operations.  
> `to_account_id` is NULL for WITHDRAWAL operations.  
> Both are set for TRANSFER operations.

---

## Required Classes

---

### 1. `Account` (Model / POJO)

**Package:** `com.bank.model`  
**File:** `Account.java`

Represents one row in the `accounts` table.

#### Attributes

| Field | Type | Column |
|-------|------|--------|
| `id` | `int` | `id` |
| `accountNumber` | `String` | `account_number` |
| `holderName` | `String` | `holder_name` |
| `balance` | `BigDecimal` | `balance` |
| `accountType` | `String` | `account_type` — either `"CHECKING"` or `"SAVINGS"` |
| `createdAt` | `java.sql.Timestamp` | `created_at` |
| `active` | `boolean` | `is_active` |

#### Constructors

| Constructor | Parameters | Purpose |
|-------------|------------|---------|
| `Account()` | none | Default |
| `Account(String accountNumber, String holderName, BigDecimal initialBalance, String accountType)` | 4 params | For creating new accounts; `id` defaults to `0`, `active` defaults to `true` |

#### Methods

All standard getters and setters, plus:

| Method | Return Type | Description |
|--------|-------------|-------------|
| `toString()` | `String` | `Account{id=1, number='ACC001', holder='Ali Yılmaz', balance=1500.00, type=CHECKING, active=true}` |

---

### 2. `TransactionRecord` (Model / POJO)

**Package:** `com.bank.model`  
**File:** `TransactionRecord.java`

Represents one row in the `transaction_log` table.

#### Attributes

| Field | Type | Column |
|-------|------|--------|
| `id` | `int` | `id` |
| `transactionType` | `String` | `transaction_type` — `"DEPOSIT"`, `"WITHDRAWAL"`, or `"TRANSFER"` |
| `fromAccountId` | `Integer` | `from_account_id` (nullable — use `Integer`, not `int`) |
| `toAccountId` | `Integer` | `to_account_id` (nullable — use `Integer`, not `int`) |
| `amount` | `BigDecimal` | `amount` |
| `balanceAfter` | `BigDecimal` | `balance_after` (nullable) |
| `description` | `String` | `description` |
| `executedAt` | `java.sql.Timestamp` | `executed_at` |

#### Constructors

| Constructor | Parameters | Purpose |
|-------------|------------|---------|
| `TransactionRecord()` | none | Default |
| `TransactionRecord(String type, Integer fromId, Integer toId, BigDecimal amount, String description)` | 5 params | For creating new log entries |

#### Methods

All standard getters and setters, plus `toString()`.

---

### 3. `InsufficientFundsException` (Custom Checked Exception)

**Package:** `com.bank.exception`  
**File:** `InsufficientFundsException.java`

A **checked exception** (extends `Exception`, not `RuntimeException`) thrown when a withdrawal or transfer would bring the balance below zero.

#### Constructors

| Constructor | Parameters | Description |
|-------------|------------|-------------|
| `InsufficientFundsException(String accountNumber, BigDecimal available, BigDecimal requested)` | 3 params | Calls `super(...)` with the message below |

**Message format:**
```
Insufficient funds in account [accountNumber]. Available: [available], Requested: [requested]
```

**Example:**
```
Insufficient funds in account ACC001. Available: 150.00, Requested: 500.00
```

> Because this is a **checked** exception, every method that can throw it must declare `throws InsufficientFundsException` in its signature.

---

### 4. `AccountInactiveException` (Custom Checked Exception)

**Package:** `com.bank.exception`  
**File:** `AccountInactiveException.java`

Thrown when an operation is attempted on a deactivated account.

#### Constructor

| Constructor | Parameters |
|-------------|------------|
| `AccountInactiveException(String accountNumber)` | `String accountNumber` |

**Message format:**
```
Account [accountNumber] is inactive and cannot perform transactions.
```

---

### 5. `ConnectionPool` (Utility — HikariCP)

**Package:** `com.bank.util`  
**File:** `ConnectionPool.java`

Configures and holds a single static `HikariDataSource`. Private constructor — cannot be instantiated.

#### Static Initializer Configuration

Set these values in the `static { }` block:

| Config Key | Value | Description |
|------------|-------|-------------|
| `jdbcUrl` | `"jdbc:postgresql://localhost:5432/bank_db"` | Connection URL |
| `username` | `"postgres"` | DB user |
| `password` | `"your_password"` | DB password |
| `maximumPoolSize` | `10` | Max concurrent connections |
| `minimumIdle` | `2` | Minimum idle connections kept alive |
| `idleTimeout` | `300000` | 5 minutes — close idle connections after this |
| `connectionTimeout` | `20000` | 20 seconds to wait for a connection from pool |
| `maxLifetime` | `1200000` | 20 minutes — max connection lifetime |
| `poolName` | `"BankPool"` | Name shown in logs |

#### Methods

| Method | Return Type | Throws | Description |
|--------|-------------|--------|-------------|
| `getConnection()` | `Connection` | `SQLException` | Returns a connection from the pool |
| `shutdown()` | `void` | — | Closes the data source; call once when the application exits |

---

### 6. `AccountDAO` (Interface)

**Package:** `com.bank.dao`  
**File:** `AccountDAO.java`

| Method | Parameters | Return Type | Description |
|--------|------------|-------------|-------------|
| `insert(Account account)` | `Account` | `void` | Inserts account; sets generated id on the object |
| `findById(int id)` | `int` | `Account` | Returns account or `null` |
| `findByAccountNumber(String number)` | `String` | `Account` | Returns account or `null` |
| `findAll()` | — | `List<Account>` | All accounts ordered by `account_number` |
| `findAllActive()` | — | `List<Account>` | Only rows where `is_active = true`, ordered by `account_number` |
| `updateBalance(int id, BigDecimal newBalance, Connection conn)` | `int id, BigDecimal, Connection` | `void` | Updates only `balance`. Accepts an **external** `Connection` so it can participate in a transaction managed by `BankService`. |
| `deactivate(int id)` | `int` | `void` | Sets `is_active = false` where `id = ?`. Does not delete. |

> **Why `updateBalance` takes a `Connection`?**  
> `BankService` opens one connection, disables auto-commit, and passes that same connection to both `updateBalance` calls during a transfer. This ensures both updates happen in the same transaction.

---

### 7. `AccountDAOImpl` (DAO Implementation)

**Package:** `com.bank.dao`  
**File:** `AccountDAOImpl.java`

Implements `AccountDAO`. Uses `ConnectionPool.getConnection()` for all methods **except** `updateBalance`, which uses the provided `Connection` parameter.

#### Private Helper

| Method | Return | Description |
|--------|--------|-------------|
| `mapRow(ResultSet rs)` | `Account` | Maps ResultSet row → `Account` |

#### SQL Queries

| Method | SQL |
|--------|-----|
| `insert` | `INSERT INTO accounts (account_number, holder_name, balance, account_type) VALUES (?, ?, ?, ?)` with `RETURN_GENERATED_KEYS` |
| `findById` | `SELECT * FROM accounts WHERE id = ?` |
| `findByAccountNumber` | `SELECT * FROM accounts WHERE account_number = ?` |
| `findAll` | `SELECT * FROM accounts ORDER BY account_number` |
| `findAllActive` | `SELECT * FROM accounts WHERE is_active = true ORDER BY account_number` |
| `updateBalance` | `UPDATE accounts SET balance = ? WHERE id = ?` |
| `deactivate` | `UPDATE accounts SET is_active = false WHERE id = ?` |

---

### 8. `TransactionDAO` (Interface)

**Package:** `com.bank.dao`  
**File:** `TransactionDAO.java`

| Method | Parameters | Return Type | Description |
|--------|------------|-------------|-------------|
| `insert(TransactionRecord record, Connection conn)` | `TransactionRecord, Connection` | `void` | Inserts a log entry using the **provided connection** (participates in outer transaction) |
| `findByAccountId(int accountId)` | `int` | `List<TransactionRecord>` | All log rows where `from_account_id = ? OR to_account_id = ?`, ordered by `executed_at DESC` |

---

### 9. `TransactionDAOImpl` (DAO Implementation)

**Package:** `com.bank.dao`  
**File:** `TransactionDAOImpl.java`

Implements `TransactionDAO`.

#### SQL Queries

| Method | SQL |
|--------|-----|
| `insert` | `INSERT INTO transaction_log (transaction_type, from_account_id, to_account_id, amount, balance_after, description) VALUES (?, ?, ?, ?, ?, ?)` — use `pstmt.setNull(index, Types.INTEGER)` for nullable ID fields |
| `findByAccountId` | `SELECT * FROM transaction_log WHERE from_account_id = ? OR to_account_id = ? ORDER BY executed_at DESC` |

---

### 10. `BankService` (Service / Core Business Logic)

**Package:** `com.bank.service`  
**File:** `BankService.java`

This is the central class of the application. All financial operations are handled here. Every method that modifies balances must:

1. Open a connection from `ConnectionPool.getConnection()`
2. Set `conn.setAutoCommit(false)`
3. Perform all SQL operations using that same connection
4. Commit on success
5. Roll back in the catch block on any failure
6. Close the connection in a `finally` block

#### Constructor

```java
public BankService(AccountDAO accountDAO, TransactionDAO transactionDAO)
```

Stores both DAOs as private fields.

---

#### Methods

---

**`deposit(String accountNumber, BigDecimal amount)`**

- **Return type:** `void`
- **Throws:** `AccountInactiveException`, `IllegalArgumentException`, `RuntimeException`
- **Steps:**
  1. Validate `amount > 0`. If not, throw `IllegalArgumentException("Deposit amount must be positive.")`.
  2. Look up the account by `accountNumber`. If not found, throw `IllegalArgumentException("Account not found: " + accountNumber)`.
  3. If `!account.isActive()`, throw `AccountInactiveException(accountNumber)`.
  4. Begin transaction (`autoCommit = false`).
  5. Lock the row: `SELECT balance FROM accounts WHERE id = ? FOR UPDATE` (prevents concurrent modification).
  6. Calculate `newBalance = currentBalance + amount`.
  7. Call `accountDAO.updateBalance(account.getId(), newBalance, conn)`.
  8. Create a `TransactionRecord`:
     - `type = "DEPOSIT"`
     - `fromAccountId = null`
     - `toAccountId = account.getId()`
     - `amount = amount`
     - `balanceAfter = newBalance`
     - `description = "Deposit to " + accountNumber`
  9. Call `transactionDAO.insert(record, conn)`.
  10. Commit.
- **Prints on success:** `"Deposited [amount] to account [accountNumber]. New balance: [newBalance]"`

---

**`withdraw(String accountNumber, BigDecimal amount)`**

- **Return type:** `void`
- **Throws:** `AccountInactiveException`, `InsufficientFundsException`, `IllegalArgumentException`, `RuntimeException`
- **Steps:**
  1. Validate `amount > 0`. If not, throw `IllegalArgumentException("Withdrawal amount must be positive.")`.
  2. Look up account. If not found, throw `IllegalArgumentException`.
  3. If inactive, throw `AccountInactiveException`.
  4. Begin transaction.
  5. Lock the row with `SELECT balance FROM accounts WHERE id = ? FOR UPDATE`.
  6. Read the **locked** balance (not the one from step 2 — use the locked result to avoid stale reads).
  7. If `lockedBalance < amount`, throw `InsufficientFundsException(accountNumber, lockedBalance, amount)`.
  8. Calculate `newBalance = lockedBalance - amount`.
  9. Call `accountDAO.updateBalance(...)`.
  10. Insert a `TransactionRecord`:
      - `type = "WITHDRAWAL"`
      - `fromAccountId = account.getId()`
      - `toAccountId = null`
      - `balanceAfter = newBalance`
      - `description = "Withdrawal from " + accountNumber`
  11. Commit.
- **Prints on success:** `"Withdrew [amount] from account [accountNumber]. New balance: [newBalance]"`

> **Note on rollback:** If `InsufficientFundsException` is thrown **before** any SQL modification happens, you still call `conn.rollback()` for safety, then re-throw.

---

**`transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount)`**

- **Return type:** `void`
- **Throws:** `AccountInactiveException`, `InsufficientFundsException`, `IllegalArgumentException`, `RuntimeException`
- **Steps:**
  1. Validate `amount > 0`.
  2. Look up both accounts. Throw `IllegalArgumentException` if either is not found.
  3. If source account is inactive, throw `AccountInactiveException(fromAccountNumber)`.
  4. If destination account is inactive, throw `AccountInactiveException(toAccountNumber)`.
  5. Prevent self-transfer: if `fromAccountNumber.equals(toAccountNumber)`, throw `IllegalArgumentException("Cannot transfer to the same account.")`.
  6. Begin transaction.
  7. Lock **both** rows — always lock the lower id first to prevent deadlock:
     ```
     int firstId  = Math.min(fromAccount.getId(), toAccount.getId());
     int secondId = Math.max(fromAccount.getId(), toAccount.getId());
     Lock firstId row with SELECT ... FOR UPDATE
     Lock secondId row with SELECT ... FOR UPDATE
     ```
  8. Read locked balances for both accounts.
  9. If sender's locked balance < amount, throw `InsufficientFundsException`.
  10. Calculate new balances.
  11. Call `accountDAO.updateBalance(fromId, newFromBalance, conn)`.
  12. Call `accountDAO.updateBalance(toId, newToBalance, conn)`.
  13. Insert **one** `TransactionRecord`:
      - `type = "TRANSFER"`
      - `fromAccountId = fromAccount.getId()`
      - `toAccountId = toAccount.getId()`
      - `balanceAfter = newFromBalance` (sender's new balance)
      - `description = "Transfer from " + fromAccountNumber + " to " + toAccountNumber`
  14. Commit.
- **Prints on success:** `"Transferred [amount] from [fromAccountNumber] to [toAccountNumber]. Sender's new balance: [newFromBalance]"`

---

**`getAccountStatement(String accountNumber)`**

- **Return type:** `void`
- **Throws:** `IllegalArgumentException`
- **Description:** Prints a full account statement to the console.
- **Steps:**
  1. Look up account. Throw `IllegalArgumentException` if not found.
  2. Call `transactionDAO.findByAccountId(account.getId())`.
  3. Print the statement in the format shown in the Sample I/O below.

---

**`createAccount(String accountNumber, String holderName, BigDecimal initialBalance, String accountType)`**

- **Return type:** `Account`
- **Throws:** `IllegalArgumentException`
- **Validation:**
  - `accountType` must be `"CHECKING"` or `"SAVINGS"`. If not, throw `IllegalArgumentException("Account type must be CHECKING or SAVINGS.")`.
  - `initialBalance` must be `>= 0`. If not, throw `IllegalArgumentException("Initial balance cannot be negative.")`.
- **Description:** Creates a new `Account` object, calls `accountDAO.insert(account)`, and returns it.
- No transaction needed — a single INSERT is atomic by default.

---

**`deactivateAccount(String accountNumber)`**

- **Return type:** `void`
- **Throws:** `IllegalArgumentException`
- **Description:** Looks up account, throws `IllegalArgumentException` if not found, otherwise calls `accountDAO.deactivate(account.getId())`.
- **Prints on success:** `"Account [accountNumber] has been deactivated."`

---

### 11. `BankApp` (Main / Entry Point)

**Package:** `com.bank`  
**File:** `BankApp.java`

Contains `main`. Creates `AccountDAOImpl`, `TransactionDAOImpl`, and `BankService`. Displays a numbered menu.

#### Menu

```
=== Banking System ===

1.  Open New Account
2.  List All Active Accounts
3.  Find Account by Number
4.  Deposit
5.  Withdraw
6.  Transfer
7.  View Account Statement
8.  Deactivate Account
0.  Exit
```

#### Exception Handling in `main`

Each menu action must be wrapped in a try-catch that prints user-friendly messages:

| Exception | Print |
|-----------|-------|
| `InsufficientFundsException` | `"ERROR: " + e.getMessage()` |
| `AccountInactiveException` | `"ERROR: " + e.getMessage()` |
| `IllegalArgumentException` | `"INPUT ERROR: " + e.getMessage()` |
| `RuntimeException` | `"SYSTEM ERROR: " + e.getMessage()` |

Never print a stack trace to the user.

#### On Exit

Before `main` returns, call `ConnectionPool.shutdown()` to cleanly close the HikariCP pool.

---

## Program Execution Flow

```
main()
  ├─ Create AccountDAOImpl, TransactionDAOImpl
  ├─ Create BankService(accountDAO, transactionDAO)
  ├─ Loop
  │    ├─ 1 → prompt accountNumber, holderName, initialBalance, accountType
  │    │       → service.createAccount(...) → print result
  │    ├─ 2 → accountDAO.findAllActive() → print table
  │    ├─ 3 → prompt accountNumber → accountDAO.findByAccountNumber(...) → print or "Not found"
  │    ├─ 4 → prompt accountNumber, amount → service.deposit(...)
  │    ├─ 5 → prompt accountNumber, amount → service.withdraw(...)
  │    ├─ 6 → prompt fromNumber, toNumber, amount → service.transfer(...)
  │    ├─ 7 → prompt accountNumber → service.getAccountStatement(...)
  │    ├─ 8 → prompt accountNumber → service.deactivateAccount(...)
  │    └─ 0 → break
  └─ ConnectionPool.shutdown()
  └─ Print "Goodbye!"
```

---

## Sample Console Interaction (Complete Run)

```
=== Banking System ===
1. Open New Account  2. List All Active Accounts  3. Find Account by Number
4. Deposit  5. Withdraw  6. Transfer  7. View Account Statement
8. Deactivate Account  0. Exit

Select option: > 1
Enter account number: > ACC001
Enter holder name: > Ali Yılmaz
Enter initial balance: > 1000.00
Enter account type (CHECKING/SAVINGS): > CHECKING
Account opened: Account{id=1, number='ACC001', holder='Ali Yılmaz', balance=1000.00, type=CHECKING, active=true}

Select option: > 1
Enter account number: > ACC002
Enter holder name: > Ayşe Kaya
Enter initial balance: > 500.00
Enter account type (CHECKING/SAVINGS): > SAVINGS
Account opened: Account{id=2, number='ACC002', holder='Ayşe Kaya', balance=500.00, type=SAVINGS, active=true}

Select option: > 2
Number  | Holder          | Type     | Balance    | Active
--------|-----------------|----------|------------|-------
ACC001  | Ali Yılmaz      | CHECKING | 1000.00    | Yes
ACC002  | Ayşe Kaya       | SAVINGS  | 500.00     | Yes

Select option: > 4
Enter account number: > ACC001
Enter deposit amount: > 250.00
Deposited 250.00 to account ACC001. New balance: 1250.00

Select option: > 5
Enter account number: > ACC001
Enter withdrawal amount: > 300.00
Withdrew 300.00 from account ACC001. New balance: 950.00

Select option: > 6
Enter source account number: > ACC001
Enter destination account number: > ACC002
Enter transfer amount: > 200.00
Transferred 200.00 from ACC001 to ACC002. Sender's new balance: 750.00

Select option: > 5
Enter account number: > ACC002
Enter withdrawal amount: > 10000.00
ERROR: Insufficient funds in account ACC002. Available: 700.00, Requested: 10000.00

Select option: > 7
Enter account number: > ACC001

========================================
  ACCOUNT STATEMENT — ACC001
  Holder:  Ali Yılmaz
  Type:    CHECKING
  Balance: 750.00
========================================
Date                | Type       | Amount    | Balance After | Description
--------------------|------------|-----------|---------------|----------------------------
2025-09-01 10:03:00 | TRANSFER   | -200.00   | 750.00        | Transfer from ACC001 to ACC002
2025-09-01 10:02:00 | WITHDRAWAL | -300.00   | 950.00        | Withdrawal from ACC001
2025-09-01 10:01:00 | DEPOSIT    | +250.00   | 1200.00       | Deposit to ACC001
========================================

Select option: > 6
Enter source account number: > ACC001
Enter destination account number: > ACC001
INPUT ERROR: Cannot transfer to the same account.

Select option: > 8
Enter account number: > ACC002
Account ACC002 has been deactivated.

Select option: > 4
Enter account number: > ACC002
Enter deposit amount: > 100.00
ERROR: Account ACC002 is inactive and cannot perform transactions.

Select option: > 0
Goodbye!
```

> **Statement display note:** The `Type` column shows the operation from the perspective of the viewed account:
> - DEPOSIT → always `+amount`
> - WITHDRAWAL → always `-amount`
> - TRANSFER where this account is the source → `-amount`
> - TRANSFER where this account is the destination → `+amount`
>
> Determine sign inside `getAccountStatement()` by comparing `fromAccountId` vs the current account's id.

---

## Package and File Structure

```
src/
└── main/
    └── java/
        └── com/
            └── bank/
                ├── BankApp.java                   ← main method here
                ├── model/
                │   ├── Account.java
                │   └── TransactionRecord.java
                ├── exception/
                │   ├── InsufficientFundsException.java
                │   └── AccountInactiveException.java
                ├── dao/
                │   ├── AccountDAO.java
                │   ├── AccountDAOImpl.java
                │   ├── TransactionDAO.java
                │   └── TransactionDAOImpl.java
                ├── service/
                │   └── BankService.java
                └── util/
                    └── ConnectionPool.java
```

---

## Key Concepts Practiced

| Concept | Where Applied |
|---------|--------------|
| HikariCP Connection Pooling | `ConnectionPool` — all connections come from the pool |
| ACID Transactions | `deposit()`, `withdraw()`, `transfer()` |
| `SELECT ... FOR UPDATE` (row locking) | All balance-modifying methods |
| Deadlock prevention | `transfer()` locks rows in consistent id order |
| Custom Checked Exceptions | `InsufficientFundsException`, `AccountInactiveException` |
| Nullable `Integer` fields | `TransactionRecord.fromAccountId`, `toAccountId` |
| `setNull()` in PreparedStatement | `TransactionDAOImpl.insert()` for nullable foreign keys |
| Audit Logging | Every operation writes to `transaction_log` in the same transaction |
| External Connection Parameter | `updateBalance(conn)`, `transactionDAO.insert(conn)` |

---

## Checklist Before Submission

- [ ] HikariCP dependency is in `pom.xml`
- [ ] `ConnectionPool` is initialized with all config values in the static block
- [ ] `ConnectionPool.shutdown()` is called before `main` returns
- [ ] `InsufficientFundsException` extends `Exception` (checked), not `RuntimeException`
- [ ] `AccountInactiveException` extends `Exception` (checked)
- [ ] `deposit()`, `withdraw()`, `transfer()` all use `SELECT ... FOR UPDATE`
- [ ] `transfer()` locks the lower id first, then the higher id
- [ ] `transactionDAO.insert()` uses the same connection as the balance update (same transaction)
- [ ] Nullable `fromAccountId`/`toAccountId` use `setNull(index, Types.INTEGER)` when null
- [ ] Account statement displays sign correctly based on direction (+ or -)
- [ ] All exceptions are caught in `main` and printed without stack traces
- [ ] `Scanner` is closed before the program exits
