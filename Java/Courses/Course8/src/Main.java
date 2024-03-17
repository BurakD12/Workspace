import java.util.ArrayList;

public class Main {
    static void makeAnimalSound(Animal animal) {
        animal.makesound();
    }

    public static void main(String[] args) {
        ArrayList<Animal> animals = new ArrayList<>();
        Dog dog = new Dog();
        Cat cat = new Cat();
        Bird bird = new Bird();

        animals.add(dog);
        animals.add(cat);
        animals.add(bird);

        for (Animal animal : animals) {
            makeAnimalSound(animal);
        }
    }
}
