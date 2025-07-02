// 9. Develop a C function `void mergeArrays(int arr1[], int n1, int arr2[], int n2, int mergedArr[])` that merges two sorted arrays `arr1` and `arr2` of sizes `n1` and `n2` into a sorted array `mergedArr`.

#include "stdlib.h"
#include "stdio.h"

void mergeArrays(int arr1[], int n1, int arr2[], int n2, int mergedArr[]);

int main()
{

    // int arr1[] = {5, 15, 25, 35, 45}, arr2[] = {10, 20, 30, 40, 50}, mergedArr[10];
    // int arr3[] = {5, 15}, arr4[] = {10, 20, 30, 40, 50};
    // int arr5[] = {5, 15, 25, 35, 45}, arr6[] = {10, 20, 30};
    int arr7[] = {5, 6, 7, 8, 9, 10, 11, 12}, arr8[] = {6, 7, 8, 9};
    int n1 = sizeof(arr7) / sizeof(int);
    int n2 = sizeof(arr8) / sizeof(int);
    int *mergedArr = malloc(sizeof(int) * (n1 + n2));
    mergeArrays(arr7, n1, arr8, n2, mergedArr);
    for (int n = 0; n < n1 + n2; n++)
    {
        printf("%d ", mergedArr[n]);
    }
    return 0;
}

void mergeArrays(int arr1[], int n1, int arr2[], int n2, int mergedArr[])
{
    int a = 0, b = 0, k = 0;
    while (a < n1 && b < n2)
    {
        if (arr1[a] <= arr2[b])
        {
            mergedArr[k] = arr1[a];
            a = a + 1;
            k = k + 1;
        }
        else
        {
            mergedArr[k] = arr2[b];
            b = b + 1;
            k = k + 1;
        }
    }
    while (a < n1)
    {
        mergedArr[k] = arr1[a];
        a = a + 1;
        k = k + 1;
    }
    while (b < n2)
    {
        mergedArr[k] = arr2[b];
        b = b + 1;
        k = k + 1;
    }
}