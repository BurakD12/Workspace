package Question5;

public class Truck {
    String make;
    String model;
    int year;
    double payloadCapacity;

    Truck(String make, String model, int year, double payloadCapacity){
        this.make = make;
        this.model = model;
        this.year = year;
        this.payloadCapacity = payloadCapacity;
    }

    void start(){
        System.out.print("Truck is starting.\n");
    }

    void stop(){
        System.out.print("Truck is stopping.\n");
    }

    void displayInfo(){
        System.out.print("Truck: " + this.make + " " + this.model + " " + this.year + " Payload Capacity: " + this.payloadCapacity + "\n");
    }


}
