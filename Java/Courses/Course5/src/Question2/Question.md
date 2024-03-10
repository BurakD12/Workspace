Question: Create a Simple Banking System

Task: You are required to create a simple banking system using Java. Your task is to define a BankAccount class and use it in a BankSystem class to perform basic banking operations.

BankAccount Class:

Attributes: The BankAccount class should have the following private attributes:
    accountNumber (String)
    accountHolder (String)
    balance (double)

Constructor: Define a parameterized constructor that initializes the accountNumber, accountHolder, and balance.

Methods: Implement the following public methods:
    * deposit(double amount): Adds the amount to the balance.
    * withdraw(double amount): Deducts the amount from the balance if sufficient funds are available If not, print an error message.
    * getAccountDetails(): Prints the account details, including the account number, account holder, and current balance.

BankSystem Class:

    * In the BankSystem class's main method, create at least two BankAccount objects with initial values.
    * Perform deposit and withdrawal operations on these objects.
    * Display the account details after each operation to show the updated balance.

--------------------------------------------------------------------

Example Output:

Account Number: 12345, Account Holder: Alice, Balance: 1000.0
Account Number: 67890, Account Holder: Bob, Balance: 500.0

Depositing 500.0 to Alice's account...
Account Number: 12345, Account Holder: Alice, Balance: 1500.0

Withdrawing 200.0 from Bob's account...
Account Number: 67890, Account Holder: Bob, Balance: 300.0


Notes:

Ensure that your withdraw method checks if the balance is sufficient before proceeding with the deduction.
Make use of proper encapsulation principles by keeping the class attributes private and providing public methods to interact with those attributes.
Test your BankSystem class thoroughly to ensure that all functionalities are working as expected.