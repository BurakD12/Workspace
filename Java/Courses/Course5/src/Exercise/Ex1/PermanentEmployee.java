package Exercise.Ex1;

public class PermanentEmployee extends Employee{
    double baseSalary;
    double bonus;

    PermanentEmployee(int ID, String name, String department, double baseSalary, double bonus){
        super(ID, name, department);
        this.baseSalary = baseSalary;
        this.bonus = bonus;
    }

    @Override
    double calculateSalary(){
        double finalSalary = this.baseSalary * 12 + this.bonus;
        return finalSalary;
    }

    @Override
    public void displayDetails() {
        super.displayDetails();
        System.out.print("the employees base salary is: " + this.baseSalary +
        "£\nthe employees bonus is: " + this.bonus + "£");
    }
}
