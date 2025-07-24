package Question;

public class BankAccount {
    private String accountNumber;
    private String accountHolder;
    private double balance;

    public BankAccount(String accountNumber, String accountHolder, double balance){
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.balance = balance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void deposit(double amount){
        this.balance += amount;
    }

    public void withdrawal(double amount) throws InsufficientBalanceException {
        if (amount > this.balance) {
            throw new InsufficientBalanceException("Insufficient funds to make this transaction!");
        }
        else {
            this.balance -= amount;
        }
    }

    public String toFileString(){
        // 101,John Doe,1500.00
        return this.accountNumber + "," + this.accountHolder + "," + this.balance;
    }

    public static BankAccount fromFileString(String line){
        String[] parts = line.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid file format for BankAccount");
        }

        String accountNumber = parts[0];
        String accountHolder = parts[1];
        double balance;
        try {
            balance = Double.parseDouble(parts[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid balance format in BankAccount file string");
        }

        return new BankAccount(accountNumber, accountHolder, balance);
    }
}
