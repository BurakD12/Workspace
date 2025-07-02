package Question3;

import java.time.LocalDate;

public class Laptop extends Electronics{
    private int storageSize;
    private int batteryLife;

    Laptop(String brand, double price, String serialNumber, LocalDate purchaseDate, int storageSize, int batteryLife){
        super(brand, price, serialNumber, purchaseDate);
        this.storageSize = storageSize;
        this.batteryLife = batteryLife;
    }

    public int getStorageSize() {
        return storageSize;
    }

    public int getBatteryLife() {
        return batteryLife;
    }
    @Override
    boolean warrantyCheck() {
        return LocalDate.now().isBefore(this.getPurchaseDate().plusYears(2));
    }
}
