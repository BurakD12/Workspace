package Question3;

import java.time.LocalDate;

public class Electronics {
    private String brand;
    private double price;
    private String serialNumber;
    private LocalDate purchaseDate;

    Electronics(String brand, double price, String serialNumber, LocalDate purchaseDate){
        this.brand = brand;
        this.price = price;
        this.serialNumber = serialNumber;
        this.purchaseDate = purchaseDate;
    }

    public String getBrand() {
        return brand;
    }
    
    public double getPrice() {
        return price;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }
    
    double discountPrice(double discountRate){
        return this.price -= this.price * discountRate;
    }

    boolean warrantyCheck(){
        return LocalDate.now().isBefore(purchaseDate.plusYears(1));
    }
}
