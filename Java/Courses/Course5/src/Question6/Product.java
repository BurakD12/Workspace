package Question6;

public class Product {
    private String productName;
    private double price;
    private int quantity;

    Product(String productName, double price, int quantity){
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
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
        // "Product: Generic Product, Price: 20.0, Quantity: 15\n"
        System.out.print("Product: " + this.productName + ", Price: " + this.price + ", Quantity: " + this.quantity + "\n");
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
}
