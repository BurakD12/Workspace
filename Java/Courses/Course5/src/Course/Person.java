package Course;

public class Person {
    // Attributes
    String name;
    int age;
    double height;

    // Constructors
    // 1. Default Constructor
    Person() {
        this.name = "UNDEFINED";
        this.age = 0;
        this.height = 0.0;
    }

    // 2. Parameterised Constructor
    Person(String name, int age, double height) {
        this.name = name;
        this.age = age;
        this.height = height;
    }

    // 3. Copy Constructor
    Person(Person obj) {
        this.name = obj.name;
        this.age = obj.age;
        this.height = obj.height;
    }

    // Methods
    void introduceMyself() {
        System.out.println("Hello, I am " + this.name + ".");
        System.out.println("I am " + this.age + " years old!");
        System.out.println("I am " + this.height + " tall!");
        System.out.println();
    }
}
