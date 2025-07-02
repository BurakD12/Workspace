// Implement a C function `int binarySearch(int arr[], int n, int x)` that performs a binary search for `x` in a sorted array `arr` of size `n`. Return the index of `x` if found, else -1.
#include "stdlib.h"
#include "stdio.h"
int binarySearch(int arr[], int n, int x);
int main()
{
    int arr[] = {1, 3, 5, 7, 9, 11, 13};
    int result = binarySearch(arr, 7, 2);

    if (result == -1)
    {
        printf("The number can`t be found in the array!");
    }
    else 
    {
        printf("The number can be found in the index: %d", result);
    }
    return 0;
}
int binarySearch(int arr[], int n, int x)
{
    int right = n-1, left = 0;

    while(left <= right){
        int mid = (right + left) / 2;

        if(arr[mid] == x){
            return mid;
        }
        else if(arr[mid] < x){
            left = mid + 1;
        }
        else{
            right = mid - 1;
        }
    }

    return -1;
}