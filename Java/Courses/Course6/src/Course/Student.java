package Course;

public class Student extends Person {
    private String department;
    private int classLevel;

    Student(String name, String surname, int age, int height, String gender, String department, int classLevel) {
        super(name, surname, age, height, gender);
        this.department = department;
        this.classLevel = classLevel;
    }

    @Override
    void displayInfo() {
        System.out.println("Name: " + this.getName() + ", Surname: " + this.getSurname() + ", Age: " + this.getAge() + ", Height: "
                + this.getHeight() + " cm, Gender: " + this.getGender() + ", Department: " + this.department + ", ClassLevel: " + this.classLevel);
    }
}