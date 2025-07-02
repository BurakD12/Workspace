package Question1;

// Demonstration class
public class AnimalDemo {
    public static void main(String[] args) {
        Bird bird = new Bird("Parrot", 5);
        Fish fish = new Fish("Goldfish", 2);

        System.out.println(bird);
        System.out.println("Sound: " + bird.makeSound());
        System.out.println("Eats: " + bird.eat());
        System.out.println("Movement: " + bird.move());

        System.out.println();

        System.out.println(fish);
        System.out.println("Sound: " + fish.makeSound());
        System.out.println("Eats: " + fish.eat());
        System.out.println("Movement: " + fish.move());
    }
}