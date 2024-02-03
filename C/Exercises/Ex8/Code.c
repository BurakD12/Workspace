#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int myStrLen(char *str){
    int cnt = 0;
    while (*(str + cnt) != '\0'){
        cnt++;
    }
    return cnt;
}

int main(){
    char str[50];
    fgets(str, 50, stdin);

    printf("%d\n", strlen(str));
    printf("%d\n", myStrLen(str));
    return 0;
}