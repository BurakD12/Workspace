package Question5;

public class Car {
    String make;
    String model;
    int year;
    
    Car(String make, String model, int year){
        this.make = make;
        this.model = model;
        this.year = year;
    }

    void start(){
        System.out.print("Car is starting.\n");
    }

    void stop(){
        System.out.print("Car is stopping.\n");
    }

    void displayInfo(){
        System.out.print("Car: " + this.make + " " + this.model + " " + this.year + "\n");        
    }
}