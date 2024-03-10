package Question4;

public class Main {
    public static void main(String[] args) {
        School school = new School();
        school.add_Student(new Student("Alice", "S001", 5));
        school.add_Student(new Student("Bob", "S002", 6));
        school.add_Teacher(new Teacher("Mr. Smith", "Mathematics", "T001"));
        school.add_Teacher(new Teacher("Ms. Johnson", "English", "T002"));

        System.out.println("Listing all students:");
        school.list_All_Students();
        System.out.println();
        System.out.println("Listing all teachers:");
        school.list_All_Teachers();
        System.out.println();

        System.out.println("Removing Alice and Mr. Smith...");
        school.remove_Student("S001");
        school.remove_Teacher("T001");

        System.out.println("Updated student list:");
        school.list_All_Students();
        System.out.println();
        System.out.println("Updated teacher list:");
        school.list_All_Teachers();
    }
}
