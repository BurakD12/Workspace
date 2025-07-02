#include <stdio.h>
#include <stdlib.h>
#include <string.h>

struct Student{
    int studentID;
    char name[50];
    float GPA;
};

void inputData(struct Student students[], int count){
    for (int i = 0; i < count; i++)
    {
        printf("\nEnter the ID: ");
        scanf("%d", &students[i].studentID);

        printf("Enter the name: ");
        fgets(students[i].name, 50, stdin);
        fgets(students[i].name, 50, stdin);
        
        printf("Enter the GPA: ");
        scanf("%f", &students[i].GPA);
    }
}

void dispData(struct Student students[], int count){
    for (int i = 0; i < count; i++)
    {
        printf("\nStudent ID: %d\nName: %sGPA: %g\n\n", students[i].studentID, students[i].name, students[i].GPA);
    }
}

void searchByID(struct Student students[], int count, int ID){
    for (int i = 0; i < count; i++)
    {
        if (students[i].studentID == ID)
        {
            printf("Student information:\nID: %d\nName: %sGPA: %g", students[i].studentID, students[i].name, students[i].GPA);
            return;
        }        
    }
    printf("There are no students for the ID number given!");
}

float avgGPA(struct Student students[], int count){
    float sum = 0;
    for (int i = 0; i < count; i++)
    {
        sum = sum + students[i].GPA;
    }
    return sum / count;
}

int main(){
    system("cls");
    int count;
    
    printf("Enter the number of students: ");
    scanf("%d", &count);

    struct Student *students = (struct Student *)malloc(sizeof(struct Student) * count);
    
    inputData(students, count);
    
    dispData(students, count);

    int indID;
    printf("Enter the students ID number: ");
    scanf("%d", &indID);
    searchByID(students, count, indID);

    int avg = avgGPA(students, count);
    
    printf("\n\nThe average of all the students is: %d", avg);

    return 0;
}