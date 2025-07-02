package Question4;

public class Teacher {
    private String name;
    private String subject;
    private String teacher_ID;

    Teacher(String name, String subject, String teacher_ID){
        this.name = name;
        this.subject = subject;
        this.teacher_ID = teacher_ID;
    }

    String getName(){
        return this.name;
    }

    String getSubject(){
        return this.subject;
    }

    String getTeacherID(){
        return this.teacher_ID;
    }
}
