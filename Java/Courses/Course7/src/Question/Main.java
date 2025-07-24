package Question;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        boolean readerFlag = false;
        String accountsFilePath = "src/Question/accounts.txt";
        String transactionsFilePath = "src/Question/transactions.txt";
        BankManager bankManager = new BankManager();
        try {
            bankManager.loadAccountFromFile(accountsFilePath);
        } catch (IOException e) {
            System.out.println("Could not load accounts from file: " + e.getMessage());
        }
        boolean running = true;
        System.out.println("Welcome to the Bank Manager System");
        while (running) {
            String choice = menu();
            switch (choice) {
                case "1" -> {
                    System.out.println("Enter the account number for the new account: ");
                    String accountNumber = scan.nextLine();
                    System.out.println("Enter the full name of the account holder: ");
                    String accountName = scan.nextLine();
                    System.out.println("Enter the initial balance of the account holder: ");
                    double balance = scan.nextDouble();
                    scan.nextLine();

                    try {
                        bankManager.createAccount(accountNumber, accountName, balance);
                    } catch (AccountAlreadyExistsException e) {
                        System.out.println(e.getMessage());
                    }
                }
                case "2" -> {
                    System.out.println("Enter the account number that will make the deposit: ");
                    String accountNumber = scan.nextLine();
                    System.out.println("Enter the amount that will be deposited: ");
                    double amount = scan.nextDouble();
                    scan.nextLine();

                    try {
                        bankManager.depositToAccount(accountNumber, amount);
                    } catch (AccountNotFoundException e) {
                        System.out.println(e.getMessage());
                    }
                }
                case "3" -> {
                    System.out.println("Enter the account number that will make the withdrawal: ");
                    String accountNumber = scan.nextLine();
                    System.out.println("Enter the amount that will be withdrawn: ");
                    double amount = scan.nextDouble();
                    scan.nextLine();

                    try {
                        bankManager.withdrawFromAccount(accountNumber, amount);
                    } catch (AccountNotFoundException e) {
                        System.out.println(e.getMessage());
                    } catch (InsufficientBalanceException e) {
                        System.out.println(e.getMessage());
                    }
                }
                case "4" -> {
                    try {
                        bankManager.saveAccountsToFile(accountsFilePath);
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                    running = false;
                }
                default -> {
                    System.out.println("Invalid input, please try again.");
                }
                    
            }
        }
    }

    public static String menu(){
        System.out.println("1. Create Account");
        System.out.println("2. Deposit Money");
        System.out.println("3. Withdraw Money");
        System.out.println("4. Exit");
        System.out.print("Please select an option: ");
        Scanner scan = new Scanner(System.in);
        return scan.nextLine();
    }
}
