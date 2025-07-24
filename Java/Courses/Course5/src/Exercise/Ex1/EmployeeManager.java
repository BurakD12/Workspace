package Exercise.Ex1;

import java.util.ArrayList;

public class EmployeeManager {
    ArrayList<Employee> employees;

    EmployeeManager(ArrayList<Employee> employees){
        this.employees = employees;
    }

    void addEmployee(Employee emp){
        for (int i = 0; i < this.employees.size(); i++) {
            if (emp.ID == this.employees.get(i).ID){
                return;
            }
        }
        this.employees.add(emp);
    }

    void viewAllEmployees(){
        System.out.println("The list of all current employees: ");
        System.out.println();
        for (int i = 0; i < this.employees.size(); i++) {
            
            this.employees.get(i).displayDetails();
            System.out.println();
            System.out.println();
        }
    }

    void updateEmployee(int ID, Employee updatedEmployee){
        for (int i = 0; i < this.employees.size(); i++) {
            if (this.employees.get(i).ID == ID){
                this.employees.remove(i);
                this.employees.add(i, updatedEmployee);
            }
        }
    }

    void deleteEmployee(int ID){
        for (int i = 0; i < this.employees.size(); i++) {
            if (this.employees.get(i).ID == ID){
                this.employees.remove(i);
            }
        }
    }


}
