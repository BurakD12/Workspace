#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct
{
    int empID;
    char name[20];
    int age;
    int departmentID;
} Employee;

typedef struct
{
    int deptID;
    char deptName[20];
} Department;

void printEmployeeDetails(Employee *employees, Department *departments, int empCount, int deptCount)
{
    for (int i = 0; i < empCount; i++)
    {
        printf("Their employee ID is: %d\nTheir name is: %sTheir age is: %d\n", employees[i].empID, employees[i].name, employees[i].age);
        for (int ii = 0; ii < deptCount; ii++)
        {
            if (employees[i].departmentID == departments[ii].deptID)
            {
                printf("Their department is: %s\n", departments[ii].deptName);
                break;
            }
        }
    }
}

int main()
{
    system("cls");
    int empCount = 3, deptCount = 2;

    Employee *employees = (Employee *)malloc(empCount * sizeof(Employee));
    Department *departments = (Department *)malloc(deptCount * sizeof(Department));

    // Check if memory allocation failed
    if (employees == NULL || departments == NULL)
    {
        printf("Memory allocation failed.\n");
        return 1; // Exit with an error code
    }

    // Example initialization - this can be replaced with user input

    printf("Fill out your department database: \n\n");
    for (int i = 0; i < deptCount; i++)
    {
        printf("The ID of department number %d: ", i + 1);
        scanf("%d", &departments[i].deptID);
        printf("The name of this department is: ");
        fgets(departments[i].deptName, 20, stdin);
        fgets(departments[i].deptName, 20, stdin);
        printf("\n");
        printf("--------------------------------\n");
    }

    printf("\n--------------------------------");

    printf("\n\nFill out your employee database: \n\n");
    for (int i = 0; i < empCount; i++)
    {
        printf("The ID of employee number %d: ", i + 1);
        scanf("%d", &employees[i].empID);

        printf("The name of this employee: ");
        fgets(employees[i].name, 20, stdin);
        fgets(employees[i].name, 20, stdin);

        printf("The age of this employee: ");
        scanf("%d", &employees[i].age);

        printf("Their department ID: ");
        scanf("%d", &employees[i].departmentID);

        printf("\n");
        printf("--------------------------------\n");
    }

    // Print details
    printEmployeeDetails(employees, departments, empCount, deptCount);

    // Free allocated memory
    free(employees);
    free(departments);

    return 0;
}