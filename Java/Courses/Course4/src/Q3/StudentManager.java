package Q3;

import java.util.ArrayList;

public class StudentManager {
    ArrayList<Student> students;

    public StudentManager(){
        this.students = new ArrayList<Student>();
    }

    public void addStudent(Student student){
        for (int i = 0; i < this.students.size(); i++) {
            if (this.students.get(i).studentID == student.studentID){
                return;
            }
        }
        this.students.add(student);
    }

    public Student getStudentByID(String studentID){
        for (int i = 0; i < this.students.size(); i++) {
            if (this.students.get(i).studentID == studentID){
                return this.students.get(i);
            }
        }
        return null;
    }

    public boolean updateStudentName(String studentID, String newName){
        for (int i = 0; i < this.students.size(); i++) {
            if (this.students.get(i).studentID == studentID){
                this.students.get(i).name = newName;
                return true;
            }
        }
        return false;
    }

    public boolean removeStudent(String studentID){
        for (int i = 0; i < this.students.size(); i++) {
            if (this.students.get(i).studentID == studentID){
                this.students.remove(i);
                return true;
            }
        }
        return false;
    }

    public void listAllStudents(){
        System.out.println("The list of all students: ");

        for (int i = 0; i < this.students.size(); i++) {
            System.out.println(this.students.get(i).toString());
            System.out.println();
        }
    }
}
