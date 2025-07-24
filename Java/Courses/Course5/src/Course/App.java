package Course;
public class App {
    public static void main(String[] args) throws Exception {
        Dog dog = new Dog("Buddy", 5, "Golden Retriever");

        System.out.println("Dog Name: " + dog.getName());
        System.out.println("Dog Age: " + dog.getAge());
        System.out.println("Dog Breed: " + dog.getBreed());

        System.out.println(dog);
    }
}
