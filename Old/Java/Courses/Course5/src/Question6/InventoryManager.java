package Question6;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class InventoryManager {
    ArrayList<Object> inventory;

    InventoryManager(){
        inventory = new ArrayList<>();
    }

    
    void addProduct(Object product){
        if (product instanceof Product || product instanceof Electronics || product instanceof FoodItem){
            inventory.add(product);
        }
    }

    void displayAllProducts(){
        for(Object x : inventory){
            if(x instanceof Product){
                ((Product)x).displayInfo();
            }
            else if (x instanceof Electronics){
                ((Electronics)x).displayInfo();
            }
            else {
                ((FoodItem)x).displayInfo();
            }
        }
    }

    double getTotalInventoryValue(){
        double sum = 0;
        for (Object x : inventory) {
            if (x instanceof Product) {
                sum += ((Product) x).getQuantity() * ((Product) x).getPrice();
            } else if (x instanceof Electronics) {
                sum +=((Electronics) x).getQuantity() * ((Electronics) x).getPrice();
            } else {
                sum += ((FoodItem) x).getQuantity() * ((FoodItem) x).getPrice();
            }
        }
        return sum;
    }


	public ArrayList<Object> getProducts() {
		return inventory;
	}
}
