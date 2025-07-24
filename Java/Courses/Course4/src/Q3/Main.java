package Q3;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        StudentManager studentManager = new StudentManager();

        Course maths = new Course("0", "Mathematics", 20);
        Course science = new Course("1", "Science", 10);
        Course litt = new Course("2", "Litterature", 20);
        
        Student student1 = new Student("1", "John", new ArrayList<Course>());
        Student student2 = new Student("2", "Mike", new ArrayList<Course>());
        Student student3 = new Student("3", "Sarah", new ArrayList<Course>());
        Student student4 = new Student("4", "Kate", new ArrayList<Course>());

        studentManager.addStudent(student1);
        studentManager.addStudent(student2);
        studentManager.addStudent(student3);
        studentManager.addStudent(student4);

        System.out.println();
        System.out.println("John's courses before any addition: ");
        student1.listCourses();

        student1.enrollCourse(litt);
        student1.enrollCourse(maths);
        
        System.out.println();
        System.out.println("John's courses after adding some courses: ");
        student1.listCourses();
        
        System.out.println();
        student1.dropCourse("0");
        
        System.out.println("John's courses after dropping maths: ");
        student1.listCourses();

        System.out.println();
        System.out.println("John's new name after changing it: ");
        student1.name = "Mark";
        System.out.println(student1.name);
    }
}
