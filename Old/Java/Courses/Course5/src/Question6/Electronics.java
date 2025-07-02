package Question6;

public class Electronics {
    private String productName;
    private double price;
    private int quantity;
    private int warrantyPeriod;

    Electronics(String productName, double price, int quantity, int warrantyPeriod){
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.warrantyPeriod = warrantyPeriod;
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

    void displayInfo() {
        // "Product: Laptop, Price: 999.99, Quantity: 10, Warranty Period: 24 months\n"
        System.out.print("Product: " + this.productName + ", Price: " + this.price + ", Quantity: " + this.quantity + ", Warranty Period: " + this.warrantyPeriod + " months\n");
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

    public int getWarrantyPeriod() {
        return warrantyPeriod;
    }

    public void setWarrantyPeriod(int warrantyPeriod) {
        this.warrantyPeriod = warrantyPeriod;
    }
    

}
