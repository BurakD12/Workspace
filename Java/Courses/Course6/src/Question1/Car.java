package Question1;

public class Car extends Vehicle{
    private String type;

    Car(String name, String type){
        super(name);
        this.type = type;
    }

    @Override
    void displayInfo() {
       System.out.println("Vehicle name : " + this.getName() + ", Type: " + this.type);
    }
}
