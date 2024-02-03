#include "stdlib.h"
#include "stdio.h"

int main()
{
    system("cls");
    int a;
    printf("Enter a digit: ");
    scanf("%d", &a);

    if (a < 10 && a > -1)
    {
        switch (a)
        {
        case 0:
            printf("Zero");
            break;
        case 1:
            printf("One");
            break;
        case 2:
            printf("Two");
            break;
        case 3:
            printf("Three");
            break;
        case 4:
            printf("Four");
            break;
        case 5:
            printf("Five");
            break;
        case 6:
            printf("Six");
            break;
        case 7:
            printf("Seven");
            break;
        case 8:
            printf("Eight");
            break;
        case 9:
            printf("Nine");
            break;
        }
    }
    else
    {
        printf("Invalid input, please enter a digit!");
    }
    return 0;
}