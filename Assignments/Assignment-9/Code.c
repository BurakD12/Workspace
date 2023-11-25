#include "stdio.h"
#include "stdlib.h"

int main()
{
    system("cls");
    
    int x;
    printf("Please Enter the rank of the current day: ");
    scanf("%d", &x);

    switch (x)
    {
    case 1:
        printf("Monday");
        break;
    case 2:
        printf("Tuesday");
        break;
    case 3:
        printf("Wednesday");
        break;
    case 4:
        printf("Thursday");
        break;
    case 5:
        printf("Friday");
        break;
    case 6:
        printf("Saturday");
        break;
    case 7:
        printf("Sunday");
        break;
    default:
        printf("Invalid input! Try again.");
    }

    return 0;
}