package Exercise.Ex1;

import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        ArrayList<Employee> employees = new ArrayList<Employee>();
        EmployeeManager employeeManager = new EmployeeManager(employees);

        while (true) {

            int choice = menu();
            switch (choice) {
                case 1 -> {
                    System.out.println("What type of employee will be added: Press 1 for Permanent, Press 2 for Contract.");
                    int selector = scan.nextInt();

                    if (selector == 1) {
                        System.out.println("Enter the employees ID: ");
                        int ID = scan.nextInt();
                        System.out.println("Enter the new employees name: ");
                        String name = scan.nextLine();
                        name = scan.nextLine();
                        System.out.println("Enter the department of the new employee: ");
                        String department = scan.nextLine();
                        System.out.println("Enter the base salary of the new employee in £: ");
                        double baseSalary = scan.nextDouble();
                        System.out.println("Enter the bonus of the new employee in £: ");
                        double bonus = scan.nextDouble();

                        employeeManager.addEmployee(new PermanentEmployee(ID, name, department, baseSalary, bonus));
                    } else if (selector == 2) {
                        System.out.println("Enter the employees ID: ");
                        int ID = scan.nextInt();
                        System.out.println("Enter the new employees name: ");
                        String name = scan.nextLine();
                        name = scan.nextLine();
                        System.out.println("Enter the department of the new employee: ");
                        String department = scan.nextLine();
                        System.out.println("Enter the hourly rate of the new employee in £: ");
                        double hourlyRate = scan.nextDouble();
                        System.out.println("Enter the amount of hours worked by the employee: ");
                        int hoursWorked = scan.nextInt();

                        employeeManager
                                .addEmployee(new ContractEmployee(ID, name, department, hourlyRate, hoursWorked));
                    } else {
                        System.out.println("Invalid input please try again!");
                    }
                }
                case 2 -> {
                    System.out.println("Enter the ID of the employee to be updated: ");
                    int ID = scan.nextInt();
                    int flag = 1;
                    for (int i = 0; i < employeeManager.employees.size(); i++) {
                        if (ID == employeeManager.employees.get(i).ID) {
                            System.out.println(
                                    "What type of employee will the updated employee be: Press 1 for Permanent, Press 2 for Contract. ");
                            int selector = scan.nextInt();
                            flag = 0;
                            if (selector == 1) {
                                System.out.println("Enter the updated employee data: ");
                                System.out.println("Their name: ");
                                String name = scan.nextLine();
                                name = scan.nextLine();
                                System.out.println("Their department: ");
                                String department = scan.nextLine();
                                System.out.println("their base salary in £: ");
                                double baseSalary = scan.nextDouble();
                                System.out.println("Their bonus in £: ");
                                double bonus = scan.nextDouble();

                                employeeManager.deleteEmployee(ID);
                                employeeManager
                                        .addEmployee(new PermanentEmployee(ID, name, department, baseSalary, bonus));

                            } else if (selector == 2) {
                                System.out.println("Enter the updated employee data: ");
                                System.out.println("Their name: ");
                                String name = scan.nextLine();
                                name = scan.nextLine();
                                System.out.println("Their department: ");
                                String department = scan.nextLine();
                                System.out.println("Their hourly rate in £: ");
                                double hourlyRate = scan.nextDouble();
                                System.out.println("The amount oh hours to be worked by the employee: ");
                                int hoursWorked = scan.nextInt();

                                employeeManager.deleteEmployee(ID);
                                employeeManager.addEmployee(
                                        new ContractEmployee(ID, name, department, hourlyRate, hoursWorked));

                            } else {
                                System.out.println("Invalid input please try again!");
                            }
                        }
                    }
                    if (flag == 1) {
                        System.out.println("Unable to find the employee. Please try again.");
                    }

                }
                case 3 -> {
                    System.out.println("Enter the ID of the employee to be removed: ");
                    int ID = scan.nextInt();

                    int selector = 1;
                    for (int i = 0; i < employeeManager.employees.size(); i++) {

                        if (employees.get(i).ID == ID) {
                            employeeManager.deleteEmployee(ID);
                            selector = 0;
                        }
                    }
                    if (selector == 1) {
                        System.out.println("Employee not found.");
                    }
                }
                case 4 -> {
                    employeeManager.viewAllEmployees();
                }
                default -> {
                    System.out.println("Invalid input please try again!");
                }
            }
        }
    }

    public static int menu() {
        Scanner scan = new Scanner(System.in);
        System.out.println("Press 1 to add an Employee.\nPress 2 to update an employee.\nPress 3 to remove an employee.\nPress 4 to view all employees.");

        return scan.nextInt();
    }
}
