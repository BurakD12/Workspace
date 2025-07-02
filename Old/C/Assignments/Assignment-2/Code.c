#include "stdio.h"
#include "stdlib.h"

int main(){
    system("cls");

    int num;
    printf("Enter your age: ");
    scanf("%d", &num);

    if(num < 18){
        printf("You are a minor.");
    }
    else if(num == 18 || num == 19){
        printf("You are a teenager.");
    }
    else if(19 < num && num < 65)
    {
        printf("You are an adult.");    
    }
    else{
        printf("You are a senior.");
    }

    return 0;
}