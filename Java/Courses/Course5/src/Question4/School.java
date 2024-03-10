package Question4;

import java.util.ArrayList;

public class School {
    private ArrayList<Student> students;
    private ArrayList<Teacher> teachers;

    School(){
        students = new ArrayList<Student>();
        teachers = new ArrayList<Teacher>(); 
    }

    void add_Student(Student student){
        students.add(student);
    }

    void remove_Student(String studentId){
        for (Student x : students){
            if (x.getStudent_ID().equals(studentId)){
                students.remove(x);
            }
        }    
    }

    void add_Teacher(Teacher teacher){
        teachers.add(teacher);
    }

    void remove_Teacher(String teacherId){
        for (Teacher x : teachers){
            if (x.getTeacherID().equals(teacherId)){
                teachers.remove(x);
            }
        }
    }

    void list_All_Students(){
        for (Student x : students){
            System.out.print("Name: " + x.getName() + ", ");
            System.out.print("Grade: " + x.getGrade() + ", ");
            System.out.print("Student ID: " + x.getStudent_ID());
            System.out.println();
        }
        
    }

    void list_All_Teachers(){
        for (Teacher x : teachers){
            System.out.print("Name: " + x.getName() + ", ");
            System.out.print("Subject: " + x.getSubject() + ", ");
            System.out.print("Teacher ID: " + x.getTeacherID());
            System.out.println();
        }
        
    }

}
