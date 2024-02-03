#include "stdlib.h"
#include "stdio.h"

int main(){
    system("cls");

    int x;
    printf("1 ==> January\n2 ==> February\n3 ==> March\n4 ==> April\n5 ==> May\n6 ==> June\n7 ==> July\n8 ==> August\n9 ==> September\n10 ==> October\n11 ==> November\n12 ==> December\n");
    printf("Enter the month you're in: ");
    scanf("%d",&x);
    system("cls");
    switch (x)
    {
        case 1:

            printf("January\n");
        case 2:
            printf("February\n");
        case 3:
            printf("March\n");
        case 4:
            printf("April\n");
        case 5:
            printf("May\n");
        case 6:
            printf("June\n");
        case 7:
            printf("July\n");
        case 8:
            printf("August\n");
        case 9:
            printf("September\n");
        case 10:
            printf("October\n");
        case 11:
            printf("November\n");
        case 12:
            printf("December\n");
            break;
        default :
        printf("invalid month!");
    }

    return 0;
}