#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void myStrCpy(char *dest, char *src);

int main(){
    char source[] = "Burak Diker!";
    char destination[50];
    myStrCpy(destination, source);
    printf("%s", destination);
    return 0;
}
// Burak
// Burak\0...

void myStrCpy(char *dest, char *src)
{
    while (*src){
        *dest = *src;
        dest++;
        src++;
    }
    *dest = '\0';
}