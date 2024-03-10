package Question6;

public class FoodItem {
    private String productName;
    private double price;
    private int quantity;
    private String expirationDate;

    FoodItem(String productName, double price, int quantity, String expirationDate){
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
    }

    void restock(int quantity){
        this.quantity += quantity;
    }

    boolean sell(int quantity){
        if (this.quantity >= quantity){
            this.quantity -= quantity;
            return true;
        }
        else {
            System.out.println("Not enough product in stock!");
            return false;
        }
    }

    void displayInfo(){
        System.out.print("Product: " + this.productName + ", Price: " + this.price + ", Quantity: " + this.quantity + ", Expiration Date: " + this.expirationDate + "\n");
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

}
