#include <stdio.h>
#include <stdlib.h>
#include <math.h>

int main()
{
    /*
        1. For Loop
        2. While Loop
        3. Do-While Loop
    */

    /*

        for(initialization; condition; update){

        }

        for(int i = 0; i < 10; i++){
            ...
        }
    */

    // for (int i = 0; i < 100; i++)
    // {
    //     printf("%d ", i);
    // }

    // Kullanicinin girdigi 2 araliktaki cift sayilari printle
    // int min, max;
    // printf("Give us a starting point: ");
    // scanf("%d", &min);

    // printf("Give us a final point: ");
    // scanf("%d", &max);

    // for(int i = min; i <= max; i++)
    // {
    //     if (i % 2 == 0)
    //     {
    //         printf("%d ", i);
    //     }
    // }

    // Kullanicin girdigi sayinin faktoryelini bulan programi yazin.
    // int a, ans = 1;
    // printf("Enter your number ");
    // scanf("%d",&a);

    // for(int i = a; i > 0; i--)
    // {
    //     ans = ans * i;
    // }
    // printf("%d", ans);

    // Kullanicinin taban ve kuvvet girmesini iste. Girilen sayilara gore power operasyonu yapip sonucu printle

    // int a, b;
    // printf("Enter your base: ");
    // scanf("%d", &a);

    // printf("Enter your exponent: ");
    // scanf("%d", &b);

    // int c = 1;

    // for(int i = b; i > 0; i--)
    // {
    //     c = a * c;
    // }
    // printf("%d", c);

    // Kullanicinin girdigi sayiya kadar olan sayilarin toplamini bulan programi yaz
    // int a, b, c = 0;

    // printf("Enter your first number: ");
    // scanf("%d", &a);

    // printf("Enter your second number: ");
    // scanf("%d", &b);

    // for (int i = a; i <= b; i++)
    // {
    //     c = i + c;
    // }
    // printf("%d", c);

    // Girilen sayinin asal olup olmadigini printleyen programi yazin.

    // int n;
    // printf("Enter your number: ");
    // scanf("%d", &n);
    // int flag = 1;
    // int counter = 1;

    // for (int i = 2; i <= sqrt(n); i++)
    // {
    //     if (n % i == 0)
    //     {
    //         flag = 0;
    //         break;
    //     }
    //     counter++;
    // }

    // if (flag == 1 && n > 1)
    // {
    //     printf("Prime!");
    // }
    // else
    // {
    //     printf("Not Prime!");
    // }

    // printf("\nCounter: %d", counter);

    //

    // int num, cnt = 0, ans = 0;

    // printf("Enter your number: ");
    // scanf("%d", &num);

    // for (; num > 0; cnt++)
    // {
    //     int rem = num % 2;

    //     ans += rem * pow(10, cnt);

    //     num /= 2;
    // }

    // printf("%d", ans);

    /*
       0

       (1 x 2^0) + (0 x 2^1) + (1 x 2^2) + (1 x 2^3)


    */

    // int a, cnt = 0, ans = 0;
    // printf("Enter your number in base 2: ");
    // scanf("%d",&a);
    // for (;a > 0;cnt++)
    // {
    //     int rem = a % 10;
    //     ans += rem * pow(2,cnt);
    //     a /= 10;

    // }
    // printf("%d", ans);

    /*

            *
            * *
            * * *
            * * * *
            * * * * *

    */

    // for (int row = 1; row <= 5; row++)
    // {
    //     for (int star = row; star > 0; star--){
    //         printf("* ");
    //     }
    //     printf("\n");
    // }

    /*

            1
            1 2
            1 2 3
            1 2 3 4
            1 2 3 4 5

    */
    //    for (int row = 1; row <= 5; row++)
    //    {
    //         for (int num = 1; num <= row; num++)
    //         {
    //             printf("%d ", num);
    //         }
    //         printf("\n");
    //    }

    /*
            A
            B B
            C C C
            D D D D
            E E E E E
    */
    //    for (int row = 1; row <= 5; row++)
    //    {
    //         for (int c = 1; c <= row; c++)
    //         {
    //             printf("%c ", 64 + row);
    //         }
    //         printf("\n");
    //    }

    /*

        * * * * *
        * * * *
        * * *
        * *
        *

    */
    // for (int row = 5; row >= 1; row--)
    // {
    //     for (int star = row; star >= 1; star--)
    //     {
    //         printf("* ");
    //     }
    //     printf("\n");
    // }

    /*

        1 2 3 4 5
        1 2 3 4
        1 2 3
        1 2
        1

    */
    // for (int row = 5; row >= 1; row--)
    // {
    //     for (int num = 1; num <= row; num++)
    //     {
    //         printf("%d ", num);
    //     }
    //     printf("\n");
    // }

    /*

    |X X X X *
    |X X X * * *
    |X X * * * * *
    |X * * * * * * *
    |* * * * * * * * *

    //
    */
    // for (int row = 1; row <= 5; row++)
    // {
    //     for (int s = 5 - row; s >= 1; s--)
    //     {
    //         printf("  ");
    //     }
    //     for (int a = 1; a <= 2 * row - 1; a++)
    //     {
    //         printf("* ");
    //     }
    //     printf("\n");
    // }

    /*

            1
          2 3 2
        3 4 5 4 3
      4 5 6 7 6 5 4
    5 6 7 8 9 8 7 6 5

    */
    // for (int row = 1; row <= 5; row++)
    // {
    //     for (int l = 5 - row; l >= 1; l--)
    //     {
    //         printf("  ");
    //     }
    //     for (int a = row; a < 2 * row; a++)
    //     {
    //         printf("%d ", a);
    //     }
    //     for (int b = 2 * row - 2; b >= row; b--)
    //     {
    //         printf("%d ", b);
    //     }
    //     printf("\n");
    // }

    /*

    * * * * * * * * *
      * * * * * * *
        * * * * *
          * * *
            *

    */
    //    for (int row = 5; row >= 1; row--)
    //    {
    //         for (int l = 5; l >= row; l--)
    //         {
    //             printf("  ");
    //         }
    //         for (int star = 2 * row; star > 1; star--)
    //         {
    //             printf("* ");
    //         }
    //         printf("\n");
    //    }

    /*

    1
    2 3
    4 5 6
    7 8 9 10

    */
    // int num = 1;
    // for (int row = 1; row <= 4; row++)
    // {
    //     for (int a = 1; a <= row; a++, num++){
    //         printf("%d ", num);
    //     }
    //     printf("\n");
    // }

    // do-while
    // while

    return 0;
}