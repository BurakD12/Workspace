package Q3;

import java.util.ArrayList;

public class Student {
    String studentID;
    String name;
    ArrayList<Course> courses;

    public Student(String studentID, String name, ArrayList<Course> courses){
        this.studentID = studentID;
        this.name = name;
        this.courses = courses;
    }

    public void enrollCourse(Course course){
        for (int i = 0; i < this.courses.size(); i++) {
            if (this.courses.get(i).getCourseID() == course.getCourseID()){
                return;
            }
        }
        this.courses.add(course);
    }

    public boolean dropCourse(String courseID){
        for (int i = 0; i < this.courses.size(); i++) {
            if (this.courses.get(i).getCourseID() == courseID){
                this.courses.remove(i);
                return true;
            }
        }
        return false;
    }

    public Course findCourseByID(String courseID){
        for (int i = 0; i < this.courses.size(); i++){
            if (this.courses.get(i).getCourseID() == courseID){
                return this.courses.get(i);
            }
        }
        return null;
    }

    public void listCourses(){
        System.out.println("The list of all of the enrolled courses: ");
        for (int i = 0; i < this.courses.size(); i++){
            System.out.println(this.courses.get(i).getCourseName());
        }
    }

    public String toString(){
        String finalString = "The students ID is: " + this.studentID 
            + ", The students name is: " + this.name 
            + ", The student is enrolled into the following courses: ";
        for (int i = 0; i < this.courses.size(); i++) {
            finalString.concat(this.courses.get(i).getCourseName() + " ");
        }
        return finalString;
    }
}
