#include "stdlib.h"
#include "stdio.h"

int main(){
    system ("cls");
    float a, b;
    printf("Enter the first number: ");
    scanf("%f",&a);
    printf("Enter the second number: ");
    scanf("%f",&b);

    // fflush(stdin); // UNIX BASED ONLY

    char x;
    printf("Enter the Operation that you want to execute: ");
    scanf("%c", &x);
    scanf("%c", &x);
    switch(x) {
        case '+':
            printf("%g + %g = %g", a, b, a + b);
            break;
        case '-':
            printf("%g - %g = %g", a, b, a - b);
            break;
        case '*':
            printf("%g * %g = %g", a, b, a * b);
            break;
        case '/':
            printf("%g / %g = %g", a, b, a / b);
            break;
        default :
            printf("Your Operation is invalid!");
        
    }
    return 0;
}
