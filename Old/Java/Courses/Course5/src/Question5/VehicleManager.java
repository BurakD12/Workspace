package Question5;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class VehicleManager {
    private ArrayList<Object> vehicles;

    VehicleManager(){
        vehicles = new ArrayList<>();
    }

    void addVehicle(Object vehicle){
        vehicles.add(vehicle);
    }

    void displayAllVehicles(){
        for (Object x : vehicles){
            if (x instanceof Car) {
                ((Car)x).displayInfo();
            }
            else {
                ((Truck)x).displayInfo();
            }
        }
    }

    void startAllVehicles(){
        for (Object x : vehicles){
            if (x instanceof Car) {
                ((Car)x).start();
            }
            else{
                ((Truck)x).start();
            }
        }
    }

    void stopAllVehicles(){
        for (Object x : vehicles){
            if (x instanceof Car){
                ((Car)x).stop();
            }
            else{
                ((Truck)x).stop();
            }
        }
    }

    ArrayList<Object> findVehiclesByMake(String make){
        ArrayList<Object> foundVehicles = new ArrayList<>();
        for (Object x : vehicles){
            if (x instanceof Car && ((Car)x).make.equals(make)) {
                foundVehicles.add(x);
            }
            else if (x instanceof Truck && ((Truck)x).make.equals(make)){
                foundVehicles.add(x);
            }
        }
        
        return foundVehicles;
    }

    public ArrayList<Object> getVehicles() {
        return vehicles;
    }

}
