#include "stdio.h"
#include "stdlib.h"

int main(){
    int grades[5];
    
    for (int i = 0; i < 5;i++){
        printf("Enter the grade number-%d: ", i+1);
        scanf("%d",&grades[i]);
    }

    printf("-----------------------\n");
    
    for (int i = 0; i < 5; i++){
        printf("Grade-%d = %d \n", i+1, grades[i]);
    }
    
    return 0;
}