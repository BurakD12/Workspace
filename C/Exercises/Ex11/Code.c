#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void myStrUpr(char *str){
    while (*str){
        if (*str >= 'a' && *str <= 'z'){
            *str = *str - 32;
        }
        str++;
    }
}

int main(){
    char str[] = "burak diker"; 
    myStrUpr(str);
    printf("%s", str);
}