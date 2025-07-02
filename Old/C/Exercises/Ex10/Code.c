#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void myStrCat(char *str1, char *str2){
    while(*str1){
        str1++;
    }
    while (*str2){
        *str1 = *str2;
        str1++;
        str2++;
    }
    *str1 = '\0';
}

int main()
{
    char str1[50] = "Burak ";
    char *str2 = "Diker";

    myStrCat(str1, str2);

    printf("%s", str1);
    return 0;
}

