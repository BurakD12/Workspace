#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct{
    int courseID;
    char courseName[100];
    int credits; 
} Course;

typedef struct{
    int studentID;
    char name[100];
    int age;
    Course *courses;
    int courseCount;
} Student;

typedef struct{
    int deptID;
    char deptName[100];
    Student *students;
    int studentCount;
}Department;

typedef struct {
    char name[100];
    Department *departments;
    int deptCount;
}University;


void initialise(University *uni){
    printf("Enter the name of the University: ");
    fgets(uni->name, sizeof(uni->name), stdin);

    printf("Enter the amount of departments available in this university: ");
    scanf("%d", &uni->deptCount);
    fflush(stdin);

    uni->departments = (Department *)malloc(uni->deptCount * sizeof(Department));

    for (int i = 0; i < uni->deptCount; i++){
        printf("\n\nEnter the ID for department-%d: ", i + 1);
        scanf("%d", &uni->departments[i].deptID);
        fflush(stdin);
        
        printf("\nEnter the name of department-%d: ", i + 1);
        fgets(uni->departments[i].deptName, sizeof(uni->departments[i].deptName), stdin);
        
        printf("Enter the number of students in this department: ");
        scanf("%d", &uni->departments[i].studentCount);
        fflush(stdin);

        uni->departments[i].students = (Student *)malloc(uni->departments[i].studentCount * sizeof(Student));

        for (int j = 0; j < uni->departments[i].studentCount; j++){
            printf("\n\nEnter the ID of student-%d", j + 1);
            scanf("%d", &uni->departments[i].students[j].studentID);
            fflush(stdin);

            printf("\nEnter the name of student-%d", j + 1);
            fgets(uni->departments[i].students[j].name, sizeof(uni->departments[i].students[j].name), stdin);

            printf("Enter the students age: ");
            scanf("%d", &uni->departments[i].students[j].age);
            fflush(stdin);

            printf("\nEnter how courses they are attending: ");
            scanf("%d", &uni->departments[i].students[j].courseCount);
            fflush(stdin);

            uni->departments[i].students[j].courses = (Course *)malloc(uni->departments[i].students[j].courseCount * sizeof(Course));
            
            for (int k = 0; k < uni->departments[i].students[j].courseCount; k++){
                printf("\n\nEnter the ID of course-%d", k);
                scanf("%d", uni->departments[i].students[j].courses[k].courseID);
                fflush(stdin);

                printf("\nEnter the name of this course: ");
                fgets(uni->departments[i].students[j].courses[k].courseName, sizeof(uni->departments[i].students[j].courses[k].courseName), stdin);

                printf("Enter how many credits this course is worth: ");
                scanf("%d", &uni->departments[i].students[j].courses[k].credits);
                fflush(stdin);
            }
        }
    }

}

int main(){
    system ("cls");   

    return 0;
}