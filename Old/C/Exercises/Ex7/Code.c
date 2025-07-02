// 10. Write a C function `int areArraysEqual(int arr1[], int n1, int arr2[], int n2)` that compares two arrays `arr1` and `arr2` of sizes `n1` and `n2`.Return 1 if they are equal, else 0.

#include "stdio.h"
#include "stdlib.h"
int areArraysEqual(int arr1[], int n1, int arr2[], int n2);

int main()
{
    int arr1[] = {2, 4, 6, 8}, arr2[] = {2, 4, 6, 8, 10};
    if (areArraysEqual(arr1, 4, arr2, 5) == 1)
    {
        printf("They are equal!");
    }
    else
    {
        printf("They are not equal!");
    }
    return 0;
}

int areArraysEqual(int arr1[], int n1, int arr2[], int n2)
{
    if (n1 != n2)
    {
        return 0;
    }
    for (int a = 0; a < n1; a++)
    {
        if (arr1[a] != arr2[a])
        {
            return 0;
        }
    }
    
    return 1;
}