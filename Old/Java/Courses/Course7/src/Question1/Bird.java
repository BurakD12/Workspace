package Question1;

public class Bird extends Animal implements AnimalBehaviour{

    Bird(String name, int age){
        super(name, age);
    }

    @Override
    String move() {
        return "Flies";
    }

    @Override
    public String eat() {
        return "Seeds";
    }

    @Override
    public String makeSound() {
        return "Chirp";
    }
    
}
