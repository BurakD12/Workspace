#include "stdlib.h"
#include "stdio.h"

int main()
{
    int a;

    printf("Enter your score: ");
    scanf("%d", &a);

    if(a % 100 == 0){
        if(a % 400 == 0){
            printf("Year is a leap year!");
        }
        else {
            printf("Year is not a leap year!");
        }
    }
    else{
        if(a % 4 == 0){
            printf("Year is a leap year!");
        }
        else {
            printf("Year is not a leap year!");
        }
    }

    return 0;
}