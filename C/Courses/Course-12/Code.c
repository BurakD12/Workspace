// swap (a, b)
#include "stdio.h"
#include "stdlib.h"

void swap(int *a, int *b){
    int x = *a;
    *a = *b;
    *b = x;
}

int main(){
    int a = 5, b = 10;
    printf("a = %d, b = %d\n", a, b);
    swap(&a, &b);
    printf("a = %d, b = %d", a, b); 
    
    return 0;
}