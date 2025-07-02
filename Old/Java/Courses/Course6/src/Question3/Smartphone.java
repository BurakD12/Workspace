package Question3;

import java.time.LocalDate;

public class Smartphone extends Electronics{
    private double screenSize;
    private double cameraMegapixels;
    private boolean dualSim;

    Smartphone(String brand, double price, String serialNumber, LocalDate purchaseDate, double screenSize, double cameraMegapixels, boolean dualSim){
        super(brand, price, serialNumber, purchaseDate);
        this.screenSize = screenSize;
        this.cameraMegapixels = cameraMegapixels;
        this.dualSim = dualSim;
    }

    public double getScreenSize() {
        return screenSize;
    }

    public double getCameraMegapixels() {
        return cameraMegapixels;
    }

    public boolean isDualSim() {
        return dualSim;
    }
    
}
