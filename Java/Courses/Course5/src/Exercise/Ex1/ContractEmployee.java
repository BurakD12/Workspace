package Exercise.Ex1;

public class ContractEmployee extends Employee{
    double hourlyRate;
    int hoursWorked;

    ContractEmployee(int ID, String name, String department, double hourlyRate, int hoursWorked){
        super(ID, name, department);
        this.hourlyRate = hourlyRate;
        this.hoursWorked = hoursWorked;
    }

    @Override
    double calculateSalary(){
        double finalSalary = this.hourlyRate * this.hoursWorked;
        return finalSalary;
    }

    @Override
    public void displayDetails() {
        super.displayDetails();
        System.out.print("the employees hourly rate is: " + this.hourlyRate + 
        "£ per hour\nthe employee has worked: " + this.hoursWorked + " hours");
    }

}
