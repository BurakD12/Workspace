package Exercise.Ex1;

public abstract class Employee {
    int ID;
    String name;
    String department;

    Employee(int ID, String name, String department){
        this.ID = ID;
        this.name = name;
        this.department = department;
    }

    abstract double calculateSalary();

    public void displayDetails(){
        System.out.println("The employees ID: " + this.ID + 
        "\nthe employees name: " + this.name + 
        "\nthe employees department: " + this.department);
    }
}
