package Question2;

public class Wallet {
    private String walletId;
    private String ownerName;
    private double balance;

    public Wallet(String walletId, String ownerName, double balance){
        this.walletId = walletId;
        this.ownerName = ownerName;
        this.balance = balance;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return this.walletId + ", " + this.ownerName + ", " + this.balance;
    }
}
