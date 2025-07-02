#include "stdio.h"
#include "stdlib.h"
// int arraySum(int *arr, int a);

// int main(){
//     int arr[5];
//     printf("Please fill in the array: \n\n");
   
//     for (int i = 0; i < 5; i++){
//         printf("Value %d: ", i + 1);
//         scanf("%d", arr + i);
//     }
    
//     int sum = arraySum(arr,5);
    
//     printf("The sum of the array is: %d", sum);
//     return 0;
// }

// int arraySum(int *arr, int a){
//     int sum = 0;
//     for (int i = 0; i < a; i++){
//         sum += *(arr + i);
//     }
//     return sum;
// }

int isSorted(int *arr, int a);

int main(){
    int arr[] = {3, 4, 6, 12, 36}, n;
    n = isSorted(arr, 5);
    
    if (n == 1)
    {
        printf("The array is sorted!");
    }
    else
    {
        printf("The array isn't sorted!");
    }
    return 0;
}

int isSorted(int *arr, int a){
    for (int i = 0; i < a - 1; i++){
        if (*(arr + i)> *(arr + i + 1))
        {
            return 0;
        }
    }
    return 1;
}

// kullanicidan 2 sayi al. Bu sayilari swap isminde bir fonksiyona gonder, bu fonksiyon sayilarin yerini degistirsin, Main fonksiyonda degisti mi kontrol et.
// call by pointer
// call by referance
// call by value
