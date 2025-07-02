Question: School Management System

Task: You are required to create a basic school management system using Java. Your system will involve multiple classes representing different components of the school system: Student, Teacher, and School.

Student Class:

Attributes: The Student class should have the following private attributes:
    name (String)
    grade (int)
    studentId (String)
Constructor: Define a parameterized constructor to initialize the name, grade, and studentId.
Getters and Setters: Implement getter and setter methods for each attribute.

--------------------------------------------------------------------------

Teacher Class:

Attributes: The Teacher class should have the following private attributes:
    name (String)
    subject (String)
    teacherId (String)
Constructor: Define a parameterized constructor to initialize the name, subject, and teacherId.
Getters and Setters: Implement getter and setter methods for each attribute.

--------------------------------------------------------------------------

School Class:

Attributes: The School class should have the following private attributes:
    students (a list of Student objects)
    teachers (a list of Teacher objects)
Methods:
    addStudent(Student student): Adds a new student to the school.
    removeStudent(String studentId): Removes the student with the given ID.
    addTeacher(Teacher teacher): Adds a new teacher to the school.
    removeTeacher(String teacherId): Removes the teacher with the given ID.
    listAllStudents(): Prints details of all the students in the school.
    listAllTeachers(): Prints details of all the teachers in the school.

--------------------------------------------------------------------------

Implementation in Main:

In your main method, create an instance of School.
Add a few Student and Teacher instances to the school.
Demonstrate the use of listAllStudents and listAllTeachers methods.
Remove a student and a teacher, then show the updated lists.

--------------------------------------------------------------------------

Example Output:

Adding students and teachers to the school...
Listing all students:
Name: Alice, Grade: 5, Student ID: S001
Name: Bob, Grade: 6, Student ID: S002

Listing all teachers:
Name: Mr. Smith, Subject: Mathematics, Teacher ID: T001
Name: Ms. Johnson, Subject: English, Teacher ID: T002

Removing Alice and Mr. Smith...
Updated student list:
Name: Bob, Grade: 6, Student ID: S002

Updated teacher list:
Name: Ms. Johnson, Subject: English, Teacher ID: T002
