package Question2;

public class BankAccount {
    String account_Number;
    String account_Holder;
    double balance;

    BankAccount(String account_Number, String account_Holder, double balance){
        this.account_Number = account_Number;
        this.account_Holder = account_Holder;
        this.balance = balance;
    }

    void deposit(double amount){
        this.balance += amount;
    }

    void withdraw(double amount){
        if (this.balance >= amount){
            this.balance -= amount;
        }
        else {
            System.out.println("There aren't sufficient funds in the account!");
        }
    }

    void get_Account_Details(){
        System.out.println("The account number is: " + this.account_Number);
        System.out.println("The account holder is: " + this.account_Holder);
        System.out.println("The balance on this account is: " + this.balance);
        System.out.println();
    }
}


