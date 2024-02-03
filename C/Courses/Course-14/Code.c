#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/*
    struct structName{
        dataType member1;
        dataType member2;
        dataType member3;
    };

    int x;

    struct structName var1;
*/

// struct Person{
//     char name[20];
//     int age;
//     float salary;
// };

// struct Student{
//     char name[40];
//     float score;
// };

// void fillstudents(struct Student *students, int numOfElements)
// {
//     for (int i = 0; i < numOfElements; i++){
//         fgets((students + i)->name, sizeof((students + i)->name), stdin);

//         printf("Enter the students name: ");
//         fgets((students + i)->name, sizeof((students + i)->name), stdin);
        
//         printf("Enter his score: ");
//         scanf("%f", &((students + i)->score));
//         printf("\n");
//     }
// }

// void printsudents(struct Student *students, int numOfElements){
//     printf("=========================\n");
//     for (int i = 0; i < numOfElements; i++)
//     {
//         printf("The name of the student is: %s", (students + i)->name);
//         printf("Their score is: %f\n\n", (students + i)->score);
//     }
// }

// struct Employee{
//     char name[20];
//     int id;
//     float salary;
// };

// void initialiseEmployees(struct Employee *employees,int size){
//     for (int i = 0; i < size; i++)
//     {
//         strcpy(employees[i].name, "Unknown");
//         employees[i].id = -1;
//         employees[i].salary = 0.0;
//     }
// }

// void printEmployee(struct Employee employee){
//     printf("Name: %s\n", employee.name);
//     printf("ID: %d\n", employee.id);
//     printf("Salary: %f\n\n", employee.salary);
// }

int main(){
    system("cls");

    // struct Person burak;
    // strcpy(burak.name, "Burak");
    // burak.age = 22;
    // burak.salary = 20000;

    // printf("Name: %s\n", burak.name);
    // printf("Age: %d\n", burak.age);
    // printf("Salary: %f", burak.salary);

    // --------------------------------------------------------

    // int nstudent;
    // printf("Enter the number of students: ");
    // scanf("%d", &nstudent);

    // struct Student *students = (struct Student *)malloc(nstudent * sizeof(struct Student));
    // fillstudents(students, nstudent);
    // printsudents(students, nstudent);

    // struct Employee employees[3];
    // initialiseEmployees(employees, 3);

    // for (int i = 0; i < 3; i++)
    // {
    //     printEmployee(employees[i]);
    // }    

    printf("\n\n");
    return 0;
}
