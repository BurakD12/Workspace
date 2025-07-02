package Question3;

import java.time.LocalDate;

public class Tablet extends Electronics{
    private double screenSize;
    private boolean hasCellular;
    private boolean canDock;

    Tablet(String brand, double price, String serialNumber, LocalDate purchaseDate, double screenSize, boolean hasCellular, boolean canDock){
        super(brand, price, serialNumber, purchaseDate);
        this.screenSize = screenSize;
        this.hasCellular = hasCellular;
        this.canDock = canDock;
    }

    public double getScreenSize() {
        return screenSize;
    }

    public boolean hasCellular() {
        return hasCellular;
    }

    public boolean canDock() {
        return canDock;
    }
    
}
