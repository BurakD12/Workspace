// Reverse a string using recursion

#include <stdio.h>
#include <string.h>

void swap(char* x, char* y){
    char temp = *x;
    *x = *y;
    *y = temp;
}

void reverse(char* str, int start, int end){
    if(start >= end){
        return;
    }

    swap(&str[start], &str[end]);
    reverse(str, start + 1, end - 1);
}

int main(int argc, char const *argv[])
{
    char str[] = "BURAK_DIKER";
    int length = strlen(str);

    printf("Original: %s\n", str);
    reverse(str, 0, length - 1);
    printf("After: %s", str);

    return 0;
}
