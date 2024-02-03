// Create a C function `void reverseArray(int arr[], int n)` that reverses the elements of an array `arr` of size `n`.
#include "stdio.h"
#include "stdlib.h"
void revArr(int arr[], int n);

int main(){
    int arr[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    
    printf("Before: ");
    for(int a = 0; a < 11; a++){
        printf("%d ", arr[a]);
    }
    
    revArr(arr, 11);
    
    printf("\nAfter: ");
    for(int b = 0; b < 11; b++){
        printf("%d ", arr[b]);
    }

    return 0;
}

void revArr(int arr[], int n){
    for(int a = 0; a < n/2; a++){
        int x = arr[n - 1 - a];
        arr[n - 1 - a] = arr[a];
        arr[a] = x;
    }
}
