package Question2;

public class BankSystem {
    public static void main(String[] args) {
        BankAccount account1 = new BankAccount("12345", "Alice" , 1000.0);
        BankAccount account2 = new BankAccount("67890", "Bob", 500.0);
        account1.get_Account_Details();
        account2.get_Account_Details();

        account1.deposit(500.0);
        account1.get_Account_Details();

        account2.withdraw(200.0);
        account2.get_Account_Details();
    }
}
