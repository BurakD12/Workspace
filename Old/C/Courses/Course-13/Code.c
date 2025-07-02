#include "stdio.h"
#include "stdlib.h"
#include "string.h"
// int countNumbers(char *str, int num);
int myStrCmp(char *str1, int len1, char *str2, int len2);

int main()
{
    // int x;

    // printf("How long is your array: ");
    // scanf("%d", &x);

    // int *arr = (int*)malloc(sizeof(int)*x);

    // if(arr == NULL){
    //     perror("Errorr, memory could not been allocated!");
    //     return -1;
    // }

    // for (int i = 0; i < x; i++)
    // {
    //     printf("Enter element-%d: ",i);
    //     scanf("%d", arr + i);
    // }

    // int max = *(arr + 0), min = *(arr + 0), sum = 0;

    // for (int i = 0; i < x; i++)
    // {
    //     if (max < *(arr + i))
    //     {
    //         max = *(arr + i);
    //     }
    //     if (min > *(arr + i))
    //     {
    //         min = *(arr + i);
    //     }
    //     sum += *(arr + i);
    // }

    // double avg = (double)sum / x;
    // printf("The maximum value is: %d\nThe minimum value is: %d\nThe average is: %lf", max, min, avg);
    // free(arr);

    // int *arr1 = (int *)malloc(sizeof(int) * 5);
    // int *arr2 = (int *)calloc(5, sizeof(int));

    // for (int i = 0; i < 5; i++)
    // {
    //     printf("%p ==> %d\n", arr1 + i, arr2[i]);
    // }

    // printf("\n");

    // for (int i = 0; i < 5; i++)
    // {
    //     printf("%p ==> %d\n", arr2 + i, arr2[i]);
    // }

    // arr2 = realloc(arr2, 10 * sizeof(int));
    // printf("\n");

    // for (int i = 0; i < 10; i++)
    // {
    //     printf("%p ==> %d\n", arr2 + i, arr2[i]);
    // }

    // arr2 = realloc(arr2, 7 * sizeof(int));
    // printf("\n");

    // for (int i = 0; i < 7; i++)
    // {
    //     printf("%p ==> %d\n", arr2 + i, arr2[i]);
    // }

    // char str1[] = "Ayb", str2[] = "Ayb";
    // printf("%d", strcmp(str1, str2));

    // Kullanicidan bir string al, girilen stringde kac tane rakam var bul ve printle. countNumbers(str,numOfElements)

    // char str[50];

    // printf("Enter your string: ");
    // fgets(str, sizeof(str), stdin); // a1b2c3\n\0\0\0\0\0....
    // int a = countNumbers(str, 50);
    // printf("There are %d numbers in the string.", a);

    // char str1[] = "Alp", str2[] = "Alp";
    // printf("%d", strcmp(str1, str2));
    // printf("%d", myStrCmp(str1, sizeof(str1), str2, sizeof(str2)));

    

    return 0;
}

// int countNumbers(char *str, int num){
//     int cnt = 0;
//     for (int i = 0; str[i] != '\n'; i++)
//     {
//         if (*(str + i) >= '0' && *(str + i) <= '9'){
//             cnt++;
//         }
//     }
//     return cnt;
// }


// int myStrCmp(char *str1, int len1, char *str2, int len2){
//     // alperen alp
//     if (len2 < len1){
//         for (int i = 0; i < len2; i++){
//             if (*(str1 + i) > *(str2 + i)){
//                 return 1;
//             }
//             else if (*(str1 + i) < *(str2 + i)){
//                 return -1;
//             }
//         }
//         return 1;
//     }
//     if (len1 < len2)
//     {
//         for (int i = 0; i < len1; i++)
//         {
//             if (*(str1 + i) > *(str2 + i))
//             {
//                 return 1;
//             }
//             else if (*(str1 + i) < *(str2 + i))
//             {
//                 return -1;
//             }
//         }
//         return 1;
//     }
//     if (len1 == len2)
//     {
//         for (int i = 0; i < len2; i++)
//         {
//             if (*(str1 + i) > *(str2 + i))
//             {
//                 return 1;
//             }
//             else if (*(str1 + i) < *(str2 + i))
//             {
//                 return -1;
//             }
//         }
//         return 0;
//     }
// }