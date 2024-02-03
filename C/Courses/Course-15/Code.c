#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// struct Person{
//     char name[20];
//     int age;
//     float salary;
// };

typedef struct
{
    int cylinderCount;
    float volume;
} Engine;

typedef struct
{
    Engine engine;
    int horsePower;
} Car;

int main()
{
    system("cls");

    // dataType varName;
    // struct Person burak;

    // strcpy(burak.name, "Burak");
    // burak.age = 20;
    // burak.salary = 10000;

    // printf("Name: %s\nAge: %d\nSalary: %f", burak.name, burak.age, burak.salary);

    // struct Person burak;
    // struct Person *burakPtr = &burak;

    // strcpy(burakPtr->name, "Burak");
    // burakPtr->age = 20;
    // burakPtr->salary = 10000;

    // printf("Name: %s\nAge: %d\nSalary: %f", burakPtr->name, burakPtr->age, burakPtr->salary);

    Car *car = (Car *)malloc(sizeof(Car));
    car->engine = *((Engine *)malloc(sizeof(Engine)));

    car->horsePower = 200;
    car->engine.cylinderCount = 6;
    car->engine.volume = 1.3;

    printf("%d\n", car->horsePower);
    printf("%d\n", car->engine.cylinderCount);
    printf("%f\n", car->engine.volume);

    printf("\n\n");
    return 0;
}