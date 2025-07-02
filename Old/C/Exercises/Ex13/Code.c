#include <stdio.h>
#include <stdlib.h>
#include <string.h>

struct Employee{
    int id;
    char name[25];
    float salary;
};

int main()
{
    system("cls");
    
    struct Employee random;

    printf("Enter your Employees details:\nID: ");
    scanf("%d", &random.id);

    fgets(random.name, 25, stdin);

    printf("Name: ");
    fgets(random.name, 25, stdin);
    
    printf("salary: ");
    scanf("%f", &random.salary);

    printf("\n\nEmployee Details:");
    printf("\nID: %d\nName: %sSalary: %f", random.id, random.name, random.salary);
    

    printf("\n\n");
    return 0;
}