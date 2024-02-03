// Implement a C function `int isSorted(int arr[], int n)` that checks if an array `arr` of size `n` is sorted in ascending order. Return 1 if sorted, else 0.
#include "stdlib.h"
#include "stdio.h"

int isSorted(int arr[], int n);

int main()
{
    int arr[] = {1, 3, 5, 7, 9, 11};
    int ans = isSorted(arr, 6);
    if (ans == 1)
    {
        printf("The array is sorted!");
    }
    else
    {
        printf("The array isn`t sorted!");
    }
}

int isSorted(int arr[], int n)
{

    for (int b = 0; b < n - 1; b++)
    {
        if (arr[b] > arr[b + 1])
        {
            return 0;
        }
    }
    return 1;
}