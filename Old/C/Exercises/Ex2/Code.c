// Develop a C function `int countOccurrences(int arr[], int n, int x)` that counts the number of times `x` appears in the array `arr` of size `n`.
#include "stdio.h"
#include "stdlib.h"

int cntOcc(int arr[], int n, int x);

int main(){
    int arr[] = {1, 1, 1, 2, 1, 5, 7, 1, 9, 1, 6, 5, 1};

    printf("%d", cntOcc(arr, 13, 1));

    return 0;
}

int cntOcc(int arr[], int n, int x){
    int count = 0;
    for (int a = 0; a < n; a++){
        if (arr[a] == x){
            count = count + 1;
        }
    }
    return count;
}