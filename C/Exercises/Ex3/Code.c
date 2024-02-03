// Write a C function `void sortArray(int arr[], int n)` that sorts an array `arr` of size `n` in ascending order.

/*
    1 9 4 2 3 5 7
    1 4 9 2 3 5 7
    1 4 2 9 3 5 7
    1 4 2 3 9 5 7
    1 4 2 3 5 9 7
    1 4 2 3 5 7 9
    1 4 2 3 5 7 9
    1 2 4 3 5 7 9
    1 2 3 4 5 7 9


*/
#include "stdlib.h"
#include "stdio.h"

void sortArr(int arr[], int n);

int main()
{
    int array[] = {6, 3, 8, 7, 11, 9, 15};
    sortArr(array, 7);

    for (int n = 0; n < 7; n++)
    {
        printf("%d ", array[n]);
    }
    return 0;
}
void sortArr(int arr[], int n)
{
    for (int c = 0; c < n - 1; c++)
    {
        for (int a = 0; a < n - 1; a++)
        {
            if (arr[a] > arr[a + 1])
            {
                int b = arr[a];
                arr[a] = arr[a + 1];
                arr[a + 1] = b;
            }
        }
    }
}