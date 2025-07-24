package Question;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.text.SimpleDateFormat;


public class BankManager {
    private List<BankAccount> accounts;

    public BankManager() {
        this.accounts = new ArrayList<BankAccount>();
    }

    public List<BankAccount> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<BankAccount> accounts) {
        this.accounts = accounts;
    }

    public void loadAccountFromFile(String filePath) throws IOException {
        File accountFile = new File(filePath);   
        Scanner scan = new Scanner(accountFile);   
        while (scan.hasNextLine()) {
            String line = scan.nextLine(); // 101,John Doe,1500.00
            // Process the line to create a BankAccount object
            // Save the object to the accounts list
            String[] parts = line.split(",");
            String accountNumber = parts[0];
            String accountHolder = parts[1];
            double balance = Double.parseDouble(parts[2]);
            
            BankAccount newAccount = new BankAccount(accountNumber, accountHolder, balance);
            this.accounts.add(newAccount);
        }
    }

    public void saveAccountsToFile(String filePath) throws IOException {
        File accountFile = new File(filePath);

        FileWriter fileWriter = new FileWriter(accountFile, false);
        for (BankAccount bankAccount : this.accounts) {
            String accountData = bankAccount.toFileString();
            fileWriter.write(accountData + "\n");
        }
        fileWriter.close();
        System.out.println("Accounts saved to file successfully.");
    }

    public BankAccount findAccount(String accountNumber) throws AccountNotFoundException {
        for (BankAccount bankAccount : this.accounts) {
            if (bankAccount.getAccountNumber().equals(accountNumber)){
                return bankAccount;    
            }
        }
        throw new AccountNotFoundException("Account with number " + accountNumber + " not found.");
    }

    public void createAccount(String accountNumber, String name, double initialBalance)
            throws AccountAlreadyExistsException {
        try {
            BankAccount existingAccount = findAccount(accountNumber);
            throw new AccountAlreadyExistsException("Account with number " + accountNumber + " already exists.");
        } catch (AccountNotFoundException e) {
            accounts.add(new BankAccount(accountNumber,name,initialBalance));
            System.out.println("New account successfully added.");
        }
    } 

    public void depositToAccount(String accountNumber, double amount) throws AccountNotFoundException {
        BankAccount account = findAccount(accountNumber);
        account.deposit(amount);
        System.out.println("Deposited " + amount + " to account " + accountNumber);
        logTransaction("Deposited " + amount + " to account " + accountNumber, "src/Question/transactions.txt");
    }

    public void withdrawFromAccount(String accountNumber, double amount)
            throws AccountNotFoundException, InsufficientBalanceException {
        BankAccount account = findAccount(accountNumber);
        account.withdrawal(amount);
        System.out.println("Withdrawn " + amount + " from account " + accountNumber);
        logTransaction("Withdrawn " + amount + " from account " + accountNumber, "src/Question/transactions.txt");
    }

    public void logTransaction(String message, String filePath) {
        try {
            File transactionFile = new File(filePath);
            FileWriter fileWriter = new FileWriter(transactionFile, true);
            
            // Format: DD:MM:YYYY HH:MM:SS - Transaction message
            Date today = new Date();
            SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd:MM:yyyy 'T' HH:mm:ss");
            String date = DATE_FORMAT.format(today);
            fileWriter.write("[" + date + "]" + " - " + message + "\n");
            fileWriter.close();
        } catch (IOException e) {
            System.out.println("Error logging transaction: " + e.getMessage());
        }
    }
}
