#include "stdio.h"
#include "stdlib.h"
#include "stdbool.h"

int main()
{
    system("cls");

    // age, gender, bmi, avg
    // int age = 21;
    // char gender = 'M';
    // float bmi = 25.2;
    // double avg = 8.6;

    // printf("age ===> %d\n", age);
    // printf("gender ===> %c\n", gender);
    // printf("bmi ===> %f\n", bmi);
    // printf("avg ===> %lf\n", avg);

    // int num;
    // printf("Enter a number: ");
    // scanf("%d", &num);

    // printf("The entered number is %d", num);

    // num ==> 20
    // &num ==> 0X16FF12AC

    // int age;
    // printf("Enter your age: ");
    // scanf("%d", &age);

    // char gender;
    // printf("Enter your gender: ");
    // scanf("%c", &gender);
    // scanf("%c", &gender);

    // float bmi;
    // printf("Enter your bmi: ");
    // scanf("%f", &bmi);

    // double avg;
    // printf("Enter your average score: ");
    // scanf("%lf", &avg);

    // printf("\n-------------------------------------------\n\n");

    // printf("You`re %d years old.\n", age);
    // printf("You are a %c.\n", gender);
    // printf("Your bmi is %f.\n", bmi);
    // printf("Your average is %lf.", avg);

    /*

                                                        OPERATORS

        1. Arithmetic Operators

            Operator	    Meaning of Operator
                +	        addition or unary plus
                -	        subtraction or unary minus
                *	        multiplication
                /	        division
                %	        remainder after division (modulo division)


            10 % 3 ==> 1
            10 % 2 ==> 0
            11 % 12 ==> 11
    */

    // int a = 9, b = 4, c;

    // c = a + b;
    // printf("a+b = %d \n", c); // 13

    // c = a - b;
    // printf("a-b = %d \n", c); // 5

    // c = a * b;
    // printf("a*b = %d \n", c); // 36

    // c  = (float)a / b;
    // printf("a/b = %f \n", ((float)a / b)); // 2

    // c = a % b;
    // printf("Remainder when a divided by b = %d \n", c); // 1

    /*
        2. Increment/Decrement Operators

            ++
            --
    */

    // int a = 5, b = 5, c = 5, d = 5;

    // printf("A++: %d\n", a++); // postincrement
    // printf("++B: %d\n", ++b); // preincrement
    // printf("C--: %d\n", c--); // postdecrement
    // printf("--D: %d\n", --d); // predecrement

    // printf("\nA: %d\n", a);
    // printf("B: %d\n", b);
    // printf("C: %d\n", c);
    // printf("D: %d\n", d);

    /*
        3. Assignment Operators

            Operator	    Example	        Same as
                =	        a = b	        a = b
                +=	        a += b	        a = a+b
                -=	        a -= b	        a = a-b
                *=	        a *= b	        a = a*b
                /=	        a /= b	        a = a/b
                %=	        a %= b	        a = a%b
    */

    // int a = 5, c;

    // c = a;
    // printf("c = %d\n", c); // 5

    // c += a;
    // printf("c = %d\n", c); // 10

    // c -= a;
    // printf("c = %d\n", c); // 5

    // c *= a;
    // printf("c = %d\n", c); // 25

    // c /= a;
    // printf("c = %d\n", c); // 5

    // c %= a;
    // printf("c = %d\n", c); // 0

    // Relational Operators

    /*
            Operators           Example             Same As
            ==                  a == c;                 a = c
            >=                  a >= c;                 a >= c
            >                   a > c;                  a > c
            <                   a < c;                  a < c
            <=                  a <= c;                 a <= c
            !=                  a != c;                 a != c


    */

    // int a = 5, b = 3, c = 7;
    // bool ans;

    // ans = a < b;
    // printf("%d < %d ==> %d\n",a, b, ans);

    // ans = a > c;
    // printf("%d > %d ==> %d\n", a, c, ans);

    // ans = b <= a;
    // printf("%d <= %d ==> %d\n", b, a, ans);

    // ans = c >= a;
    // printf("%d >= %d ==> %d\n", c, a, ans);

    // ans = a == b;
    // printf("%d == %d ==> %d\n", a, b, ans);

    // ans = b != a;
    // printf("%d != %d ==>%d\n",b, a, ans);

    // Logical Operators

    /*
            Operators                   Example
                &&                      x && y
                ||                      x || y
                !                       !x
    */
    // int a = 5, b = 10;
    // bool ans;

    // ans = a == 5 && b == 10;
    // printf("%d == 5 && %d == 10 ==> %d\n",a, b, ans);

    // ans = a == 4 || b == 10;
    // printf("%d == 4 || %d == 10 ==> %d\n",a, b, ans);

    // ans = !(a < 10);
    // printf("!(%d < 10) ==> %d",a, ans);

    // Bitwise Operators

    /*
            If-Else Structures
            https://www.programiz.com/c-programming/c-if-else-statement
    */

    return 0;
}
