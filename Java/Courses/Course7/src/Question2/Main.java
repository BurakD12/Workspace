package Question2;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        boolean running = true;
        String walletsFilePath = "/Users/burakdiker/Desktop/Workspace/Java/Courses/Course7/src/Question2/wallets.txt";
        WalletManager walletManager = new WalletManager();

        try {
            walletManager.loadAccountsFromFile(walletsFilePath);
        } catch (IOException e) {
            System.out.println("Couldn't load wallets from file: " + e.getMessage());
        }

        
        while (running) {
            String choice = menu();
            switch (choice) {
                case "1" -> {
                    System.out.println("Please enter the wallet ID for the new wallet: ");
                    String walletId = scan.nextLine();
                    System.out.println("Please enter the name of the wallet holder: ");
                    String ownerName = scan.nextLine();
                    System.out.println("Please enter the initial balance of the wallet: ");
                    double initialBalance = scan.nextDouble();
                    scan.nextLine();
                    walletManager.createWallet(walletId, ownerName, initialBalance);
                }
                case "2" -> {
                    System.out.println("Please enter the wallet ID of which the owner will be updated: ");
                    String walletId = scan.nextLine();
                    System.out.println("Please enter the new name for the owner: ");
                    String newOwner = scan.nextLine();
                    walletManager.updateWalletOwner(walletId, newOwner);
                }
                case "3" -> {
                    System.out.println("Please enter the wallet ID that will make a deposit: ");
                    String walletId = scan.nextLine();
                    System.out.println("Enter the deposit amount: ");
                    double amount = scan.nextDouble();
                    scan.nextLine();
                    walletManager.deposit(walletId, amount);
                }
                case "4" -> {
                    System.out.println("Please enter the wallet ID that will make a withdrawal: ");
                    String walletId = scan.nextLine();
                    System.out.println("Enter the withdrawal amount: ");
                    double amount = scan.nextDouble();
                    scan.nextLine();
                    walletManager.withdraw(walletId, amount);
                }
                case "5" -> {
                    walletManager.printAllWallets();
                }
                case "6" -> {
                    try {
                        walletManager.saveAccountsToFile(walletsFilePath);
                    } catch (IOException e) {
                        System.out.println("Couldn't save the wallets to file: " + e.getMessage());
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
        Scanner scan = new Scanner(System.in);
        System.out.println("1. Create a wallet");
        System.out.println("2. Update the owner of a wallet");
        System.out.println("3. Make a deposit");
        System.out.println("4. Make a withdrawal");
        System.out.println("5. Print all wallets");
        System.out.println("6. Exit the system");
        System.out.println("Please select an option: ");
        return scan.nextLine();
    }
}
