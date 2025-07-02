#include "stdlib.h"
#include "stdio.h"

int main(){
    int a;
    
    printf("Enter your score: ");
    scanf("%d", &a);

    if(a >= 90){
        printf("You've got an A");
    }
    else if (a < 90 && a >= 80){
        printf("You've got a B");
    }
    else  if (a < 80 && a >= 70){
        printf("You've got a C");
    }
    else if (a < 70 && a >= 60){
        printf("You've got a D");
    }
    else{
        printf("You've got an F");
    }

    return 0;
}