#include "stdio.h"
#include "stdlib.h"

/*
        Kullanici 2 sayi girsin. Ardindan yapilmak istenen islemi girsin. Islemin cinsine gore gerekli fonksiyon calisin ve cevap printlensin.

        Sayi 1: xxx
        Sayi 2: xxx
        Operation (+,-,*,/): *

        int addition(int n1, int n2);
        int subtraction(int n1, int n2);
        int multiplicaton(int n1, int n2);
        int division(int n1, int n2);

        Cevap: xxx
*/
// int myAdd(int n1, int n2);
// int myDiff(int n1, int n2);
// int myMtp(int n1, int n2);
// int myDiv(int n1, int n2);


/*

    Kullanici 1 sayi girecek. Girilen sayinin faktoriyelini bulan fonksiyonu tanimla ve dondurdugu cevabi printle.

*/
// int fac(int n);

/*
    Kullanici taban ve kuvvet girsin. Power operasyonu yapan bir fonksiyon yaz.

*/
// int mypow(int n1, int n2);

int main(){
    // int n1, n2, flag = 1, result;
    // char op;

    // printf("Please enter your 1st number: ");
    // scanf("%d", &n1);

    // printf("Please enter your 2nd number: ");
    // scanf("%d", &n2);

    // while (flag == 1){
    //     // fflush(stdin);
    //     scanf("%c", &op);
    //     printf("Please enter a valid operation to execute: ");
    //     scanf("%c", &op);
    //     printf("\n------------\n");

    //     switch (op)
    //     {
    //     case '+':
    //         result = myAdd(n1, n2);
    //         printf("%d", result);
    //         flag = 0;
    //         break;
    //     case '-':
    //         result = myDiff(n1, n2);
    //         printf("%d", result);
    //         flag = 0;
    //         break;
    //     case '*':
    //         result = myMtp(n1, n2);
    //         printf("%d", result);
    //         flag = 0;
    //         break;
    //     case '/':
    //         result = myDiv(n1, n2);
    //         printf("%d", result);
    //         flag = 0;
    //         break;
    //     }
    // }
//    int a, result;
//    printf("Please enter a number: ");
//    scanf("%d", &a);
//    result = fac(a);
//    printf("%d", result); 

    // int a, b, ans;
    // printf("Please enter a base: ");
    // scanf("%d", &a);
    // printf("Please enter a power: ");
    // scanf("%d", &b);
    // ans = mypow(a, b);
    // printf("\n%d", ans);



    return 0;
}

// int findMax(int arr[], int n){

// }


// int myAdd(int n1, int n2)
// {
//     int result = n1 + n2;
//     return result;
// }

// int myDiff(int n1, int n2){
//     int result = n1 - n2;
//     return result;
// }

// int myMtp(int n1, int n2)
// {
//     int result = n1 * n2;
//     return result;
// }

// int myDiv(int n1, int n2){
//     int result = n1 / n2;
//     return result;
// }

// int fac(int n){
//     int result = 1;
//     for (;n > 0; n--){
//         result = result * n;
//     }

//     // for (int i = 1; i <= n; i++)
//     // {
//     //     result *= i;
//     // }
//     return result;
// }

// int mypow(int n1,int n2){
//     int ans = 1;
//     for (int b = n2; b >= 1; b--)
//     {
//         ans = ans * n1;
//     }
//     return ans;
// }