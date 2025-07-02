package Course;

public class Student {
    String name;
    int numOfClasses;
    int age;


    Student(){
        this.name = "Undefined";
        this.numOfClasses = 0;
        this.age = 0;
    }

    Student(String name,int numOfClasses, int age){
        this.name = name;
        this.numOfClasses = numOfClasses;
        this.age = age;
    }

    Student(Student obj){
        this.name = obj.name;
        this.numOfClasses = obj.numOfClasses;
        this.age = obj.age;
    }

    void introduceStudent(){
        System.out.println("The students name is " + this.name + ".");
        System.out.println("The student has " + this.numOfClasses + " amount of classes.");
        System.out.println("The student is " + this.age + " years old.");
        System.out.println();
    }
}

