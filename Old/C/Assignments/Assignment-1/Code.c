#include "stdlib.h"
#include "stdio.h"

int main(){
    system("cls");
    
    int num;
    printf("Enter a number: ");
    scanf("%d", &num); 

    if (num % 2 == 0){
        printf("The number is even.");

    }
    else{
        printf("The number is odd.");
    }
    

    return 0;
}