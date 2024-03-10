package Question4;

public class Student {
    private String name;
    private String student_ID;
    private int grade;

    Student(String name, String student_ID, int grade){
        this.name = name;
        this.student_ID = student_ID;
        this.grade = grade;
    }

    String getName(){
        return this.name;
    }

    String getStudent_ID(){
        return this.student_ID;
    }

    int getGrade(){
        return this.grade;
    }

}
