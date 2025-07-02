#include "stdio.h"
#include "stdlib.h"

int main()
{
    // int array[5];
    // for (int k = 0; k < 5; k++){
    //     printf("Please enter your number: ");
    //     scanf("%d", &array[k]);
    // }
    // for (int l = 0; l < 5; l++){
    //     printf("%d\n",array[l]);
    // }

    // double array[5], b = 0;
    // for (int k = 0; k < 5; k++){
    //     printf("Please enter your number: ");
    //     scanf("%lf", &array[k]);
    // }
    // for (int a = 0; a < 5; a++){
    //     b = b + array[a];
    // }
    // printf("\nThe sum of the values are: %lf\n", b);
    // printf("The arithmetic mean of the values are: %lf\n", b / 5);

    // int array[5];
    // for (int k = 0; k < 5; k++){
    //     printf("Enter your number: ");
    //     scanf("%d", &array[k]);
    // }
    // int min = array[0], max = array[0];
    // for (int a = 0; a < 5; a++)
    // {
    //     if(min > array[a]){
    //         min = array[a];
    //     }
    // }
    // for (int b = 0; b < 5; b++){
    //     if(max < array[b]){
    //         max = array[b];
    //     }
    // }
    // printf("The lowest value is: %d\n", min);
    // printf("The highest value is: %d", max);

    // int arr[] = {1,1,1,1,2,2,3,3,3,3,3,4,4,4,5,6,6,6,7,7,8,8,9,9};

    // for (int a = 0, cnt = 1; a < 24; a += cnt){
    //     cnt = 1;
    //     int source = arr[a];
    //     int i = a + 1;
    //     while(source == arr[i]){
    //         cnt++;
    //         i++;
    //     }
    //     printf("%d occures %d times\n",source, cnt);
    // }

    /*
        5 elemanli bir array tanimla. Kullaniciya bu arrayi doldurt fakat sadece pozitif sayi girilmesine izin ver. Array doldugu anda elementleri printle.
    */
    // int arr[5];
    // for(int x = 0; x < 5; x++){
    //     int num = -1;
    //     while (num < 0){
    //         printf("Enter a positive number: ");
    //         scanf("%d", &num);
    //     }
    //     arr[x] = num;
    // }

    // for(int y = 0; y < 5; y++){
    //     printf("%d ", arr[y]);
    // }

    /*
        10 elemanli bir array doldurt. Bu array'i kucuge buyuge sirala ve printle
    */
    // int arr[10];
    // for (int n = 0; n < 10; n++)
    // {
    //     printf("Please enter your number: ");
    //     scanf("%d", &arr[n]);
    // }
    // system("cls");

    // printf("Before: ");
    
    // for(int b = 0; b < 10; b++){
    //     printf("%d ", arr[b]);
    // }

    // for (int k = 0; k < 10; k++){
    //     for (int l = 0; l < 10 - k - 1; l++){
    //         if (arr[l] > arr[l + 1]){
    //             int a = arr[l];
    //             arr[l] = arr[l + 1];
    //             arr[l + 1] = a;
    //         }
    //     }
    // }
    // printf("\n\nAfter: ");
    // for (int c = 0; c < 10; c++)
    // {
    //     printf("%d ", arr[c]);
    // }

    // int mat[3][3];
    // for(int a = 0; a < 3; a++){
    //     for (int b = 0; b < 3; b++){
    //         printf("Enter your number here: ");
    //         scanf("%d", &mat[a][b]);
    //     }
    // }
    // for (int n = 0; n < 3; n++)
    // {
    //     printf("\n");
    //     for (int m = 0; m < 3; m++)
    //     {
    //         printf("%d ",mat[n][m]);
    //     }
    // }

    // 3x3
    // avg of every row
    // avg of whole matrix

    /*
            1 2 3   avg = 3     min = 1      max = 3
            4 5 6   avg = 6     min = 4      max = 6
            7 8 9   avg = 9     min = 7      max = 9
    */
    
    // int mat[3][3];
    // for (int a = 0; a < 3; a++)
    // {
    //     for (int b = 0; b < 3; b++)
    //     {
    //         printf("Enter your number: ");
    //         scanf("%d", &mat[a][b]);
    //     }
    // }
    // for (int n = 0; n < 3; n++)
    // {
    //     int min = mat[n][0], max = mat[n][0];
    //     float avg = 0;
    //     for (int m = 0; m < 3; m++)
    //     {
    //         if (mat[n][m] > max){
    //             max = mat[n][m];
    //         }
    //         if (mat[n][m] < min){
    //             min = mat[n][m];
    //             }
    //         printf("%d ", mat[n][m]);
    //         avg = avg + mat[n][m];
    //     }
    //     printf("\t\tmin: %d max: %d avg: %f\n", min, max, avg / 3.0);
    // }

    return 0;
}