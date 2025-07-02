#include "stdlib.h"
#include "stdio.h"

int main()
{
    char letter;

    printf("Type in: ");
    scanf("%c", &letter); // ampersand

    if (letter >= '0' && letter <= '9')
    {
        printf("This is a number.");
    }
    else if ((letter >= 'A' && letter <= 'Z') || (letter >= 'a') && (letter <= 'z'))
    {
        if (letter == 'a' || letter == 'e' || letter == 'i' || letter == 'o' || letter == 'u' 
        || letter == 'A' || letter == 'E' || letter == 'I' || letter == 'O' || letter == 'U')
        {
            printf("This character is a vowel.");
        }
        else{
            printf("This is a consonant.");
        }
    }
    else{
        printf("This is a special character.");
    }
    
    return 0;
}