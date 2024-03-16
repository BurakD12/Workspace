package Course;

public class Person {
    private String name;
    private String surname;
    private int age;
    private int height;
    private String gender;

    Person(String name, String surname, int age, int height, String gender){
        this.name = name;
        this.surname = surname;
        this.age = age;
        this.height = height;
        this.gender = gender;
    }

    void displayInfo(){
        System.out.println("Name: " + this.name + ", Surname: " + this.surname + ", Age: " + this.age + ", Height: " + this.height + " cm, Gender: " + this.gender);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    
}
