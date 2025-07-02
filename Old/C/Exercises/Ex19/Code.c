/*

Question: Implement a University Course Registration System
Background:
You are tasked to develop a dynamic university course registration system in C. This system should allow students
to register for courses. Each course can have multiple students, and each student can register for multiple courses.

Structures:
Student Structure:

* id (integer)
* name (string)
* registered_courses (pointer to an array of Course pointers)

Course Structure:

* code (string)
* title (string)
* enrolled_students (pointer to an array of Student pointers)
* enrollment_count (integer)

Function Signatures:

1. Student* create_student(int id, const char* name)
Creates a new student with the given id and name. Initializes an empty course registration list for the student.

2. Course* create_course(const char* code, const char* title)
Creates a new course with the given code and title. Initializes an empty student list for the course.

3. void register_student_for_course(Student* student, Course* course)
Registers a student for a course. The student is added to the course's enrolled students list, and the course is added to the student's registered courses list. Handle memory allocation dynamically.

4. void unregister_student_from_course(Student* student, Course* course)
Unregisters a student from a course. The student is removed from the course's enrolled students list, and the course is removed from the student's registered courses list. Handle memory reallocation dynamically.

5. void print_course_info(const Course* course)
Prints all information about the course, including a list of enrolled students.

6. void free_student(Student* student)
Frees all memory allocated for the student, including their registered courses list.

7. void free_course(Course* course)
Frees all memory allocated for the course, including its enrolled students list.

*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct Student Student;
typedef struct Course Course;

typedef struct
{
    int ID;
    char name[100];
    Course **registered_courses;
    int course_count;
} Student;

typedef struct
{
    char code[10];
    char title[10];
    Student **enrolled_students;
    int enrollment_count;
} Course;

Student *create_student(int ID, const char *name)
{
    Student *new_student = (Student *)malloc(sizeof(Student));
    new_student->ID = ID;
    strcpy(new_student->name, name);
    new_student->course_count = 0;
    new_student->registered_courses = NULL;
    return;
}

Course *create_course(const char *code, const char *title)
{
    Course *new_course;
    strcpy(new_course->code, code);
    strcpy(new_course->title, title);
    new_course->enrollment_count = 0;
    new_course->enrolled_students = NULL;
    return;
}

void register_student_for_course(Student *student, Course *course)
{
    student->registered_courses = (Student *)realloc(student->registered_courses, (student->course_count + 1) * sizeof(Course));
    if (student->registered_courses == NULL)
    {
        printf("Error Allocating Memory!");
        return;
    }
    student->registered_courses[student->course_count] = course;

    course->enrolled_students = (Course *)realloc(course->enrolled_students, (course->enrollment_count + 1) * sizeof(Student));
    if (course->enrolled_students == NULL)
    {
        printf("Error Allocating Memory!");
        return;
    }
    course->enrolled_students[course->enrollment_count] = student;
    course->enrollment_count++;
    student->course_count++;
}

void unregister_student_from_course(Student *student, Course *course)
{
    for (int i = 0; i < course->enrollment_count; i++)
    {
        if (student->ID == course->enrolled_students[i]->ID)
        {
            for (int j = i; j < course->enrollment_count - 1; j++)
            {
                course->enrolled_students[j] = course->enrolled_students[j + 1];
            }
            course->enrolled_students = (Course *)realloc(course->enrolled_students, sizeof(Student) * (--course->enrollment_count));
            break;
        }
    }
    for (int i = 0; i < student->course_count; i++)
    {
        if (strcmp((student->registered_courses[i])->code, course->code) == 0)
        {
        
        }
    }
}
