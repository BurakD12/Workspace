package Question2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;

public class WalletManager {
    private HashMap<String, Wallet> walletMap = new HashMap<>();

    public HashMap<String, Wallet> getWalletMap() {
        return walletMap;
    }

    public void setWalletMap(HashMap<String, Wallet> walletMap) {
        this.walletMap = walletMap;
    }

    public void createWallet(String walletId, String ownerName, double initialBalance){
        if (!walletMap.containsKey(walletId)) {
            walletMap.put(walletId, new Wallet(walletId, ownerName, initialBalance));
            LogManager.logInfo("New wallet with ID: " + walletId + " has been created.");
        }
        else {
            LogManager.logError("Wallet could not be created: Wallet with ID " + walletId + " already exists.");
        }
    }

    public Wallet getWallet(String walletId){
        if (walletMap.containsKey(walletId)){
            return walletMap.get(walletId);
        }
        else{
            LogManager.logError("Wallet could not be retrieved: Wallet with ID " + walletId + " not found.");
            return null;
        }
    }

    public void updateWalletOwner(String walletId, String newOwnerName){
        if (walletMap.containsKey(walletId)){
            walletMap.get(walletId).setOwnerName(newOwnerName);
        }
        else {
            LogManager.logError("Wallet could not be updated: Wallet with ID " + walletId + " not found.");
        }
    }

    public void deposit(String walletId, double amount){
        if (walletMap.containsKey(walletId)){
            double newBalance = walletMap.get(walletId).getBalance() + amount;
            walletMap.get(walletId).setBalance(newBalance);
            LogManager.logTransaction("Deposited " + amount + " to the wallet with ID: " + walletId + ".");
        }
        else {
            LogManager.logError("Wallet could not be updated: Wallet with ID " + walletId + " not found.");
        }
    }

    public void withdraw(String walletId, double amount){
        if (walletMap.containsKey(walletId)){
            if (walletMap.get(walletId).getBalance() > amount){
                double newBalance = walletMap.get(walletId).getBalance() + amount;
                walletMap.get(walletId).setBalance(newBalance);
                LogManager.logTransaction("Withdrawn " + amount + " from the wallet with ID: " + walletId + ".");
            }
            else {
                LogManager.logError("Insufficient funds to make the transaction.");
            }
        }
        else {
            LogManager.logError("Wallet could not be updated: Wallet with ID " + walletId + " not found.");
        }
    }

    public void printAllWallets(){
        for (Entry<String, Wallet> wallet : walletMap.entrySet()){
            StringBuilder sb = new StringBuilder();
            sb.append(wallet.getValue().toString()).append("\n");
            System.out.println(sb);
        } 
    }

    public void loadAccountsFromFile(String filePath) throws IOException{
        File walletFile = new File(filePath);
        Scanner scan = new Scanner(walletFile);
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            String[] parts = line.split(", ");
            String walletId = parts[0];
            String walletOwner = parts[1];
            double balance = Double.parseDouble(parts[2]);

            this.walletMap.put(walletId, new Wallet(walletId, walletOwner, balance));
            LogManager.logInfo("Successfully loaded the accounts.");
        }
    }

    public void saveAccountsToFile(String filePath) throws IOException{
        File walletsFile = new File(filePath);
        FileWriter fileWriter = new FileWriter(walletsFile, false);

        for (Entry<String, Wallet>  wallet: this.walletMap.entrySet()) {
            fileWriter.write(wallet.getValue().toString());
        }
        fileWriter.close();
        LogManager.logInfo("Successfully saved the accounts.");
    }
}
