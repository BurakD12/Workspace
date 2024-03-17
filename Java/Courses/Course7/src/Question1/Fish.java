package Question1;

public class Fish extends Animal implements AnimalBehaviour{
    Fish(String name, int age){
        super(name, age);
    }

    @Override
    String move() {
        return "Swims";
    }

    @Override
    public String eat() {
        return "Plankton";
    }

    @Override
    public String makeSound() {
        return "Blub";
    }

    
}
