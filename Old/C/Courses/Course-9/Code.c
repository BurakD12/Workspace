// void fillArray(int arr[], int n);
// void displayArray(int arr[], int n);
// int findMin(int arr[], int n);
// int findMax(int arr[], int n);
// int findAvg(int arr[], int n);
#include "stdio.h"
#include "stdlib.h"

void fillArray(int arr[], int n);
void dispArray(int arr[], int n);
int findMin(int arr[], int n);
int findMax(int arr[], int n);
int findAvg(int arr[], int n);

int main(){
    int arr[5];
    fillArray(arr, 5);
    dispArray(arr, 5);
    
    int min = findMin(arr, 5);
    int max = findMax(arr, 5);
    int avg = findAvg(arr, 5);

    printf("\nThe minimum value is: %d\nThe maximum value is: %d\nThe average of the numbers inside is: %d", min, max, avg);

    return 0;
}
void fillArray(int arr[], int n){
    for (int a = 0; a < n; a++){
        printf("Enter the value: ");
        scanf("%d", &arr[a]);
    }
}
void dispArray(int arr[], int n){
    for (int b = 0; b < n; b++){
        printf("%d ", arr[b]);
    }
}
int findMin(int arr[], int n){
    int ans = arr[0];
    for (int c = 0; c < n; c++){
        if(ans > arr[c]){
            ans = arr[c];
        }
    }
    return ans;
}
int findMax(int arr[], int n){
    int ans = arr[0];
    for (int d = 0; d < n; d++)
    {
        if (ans < arr[d])
        {
            ans = arr[d];
        }
    }
    return ans;
}
int findAvg(int arr[], int n){
    int sum = 0;
    for(int e = 0; e < n; e++){
        sum = sum + arr[e];
    }
    return sum / 5;
}
