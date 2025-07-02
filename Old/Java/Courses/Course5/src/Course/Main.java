package Course;

public class Main {
    public static void main(String[] args) {
        Student Burak = new Student();
        Student Ayberk = new Student("Ayberk", 3, 24);
        Student Murat = new Student(Ayberk);

        Burak.introduceStudent();
        Ayberk.introduceStudent();
        Murat.introduceStudent();

    }
}
