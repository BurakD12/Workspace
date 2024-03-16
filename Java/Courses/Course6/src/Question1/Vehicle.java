package Question1;

public class Vehicle {
    private String name;

    Vehicle(String name){
        this.name = name;
    }

    void displayInfo(){
        System.out.println("Vehicle name: " + this.name);
    }

    public String getName() {
        return name;
    }

    
}
