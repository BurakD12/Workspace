#include <stdio.h>
#include <stdlib.h>

int main(){
    system("cls");

    // int var = 5;
    // printf("Var = %d\n", var);
    // printf("&Var = %p\n", &var);

    // int *varPtr = &var;
    // printf("VarPtr = %p\n", varPtr);
    // printf("*VarPtr = %d\n", *varPtr);

    // int *pc, c;
    // c = 5;
    // pc = &c;
    // printf("%d", *pc);

    // int *pc, c;
    // c = 5;
    // pc = &c;
    // c = 1;
    // printf("%d", c);
    // printf("%d", *pc);

    // int *pc, c;
    // c = 5;
    // pc = &c;
    // *pc = 1;
    // printf("%d", *pc);
    // printf("%d", c);

    // int *pc, c, d;
    // c = 5;
    // d = -15;

    // pc = &c;
    // printf("%d", *pc);

    // pc = &d;
    // printf("%d", *pc);

    // int *pc, c;

    // c = 22;
    // printf("Address of c: %p\n", &c); // X
    // printf("Value of c: %d\n\n", c); //22

    // pc = &c;
    // printf("Address of pointer pc: %p\n", pc);// X
    // printf("Content of pointer pc: %d\n\n", *pc); // 22

    // c = 11;
    // printf("Address of pointer pc: %p\n", pc); // X
    // printf("Content of pointer pc: %d\n\n", *pc); // 11

    // *pc = 2;
    // printf("Address of c: %p\n", &c); // X
    // printf("Value of c: %d\n\n", c); // 2

    /*
        int c, *pc;
        pc = c; // ERROR
        *pc = &c; // ERROR
        pc = &c;
        *pc = c;
    */

    // int a = 10;
    // int *p = &a;
    // printf("%d\n", *p);

    // int a = 5;
    // int *p;
    // p = &a;
    // printf("%p\n", p);

    // int a = 10;
    // int *p = &a;
    // *p = 20;
    // printf("%d\n", a);

    // int x = 10, y = 20;
    // int *ptr;
    // ptr = &x;
    // ptr = &y;
    // *ptr = 30;
    // printf("x = %d, y = %d\n", x, y);

    // int arr[] = {10, 20, 30};
    // int *p = arr;
    // printf("%d\n", p[1]);

    // char *s = "hello";
    // printf("%c\n", *s);

    // int a[] = {1, 2, 3, 4, 5};
    // int *p = a;
    // printf("%d\n", *(p + 3));

    // int num = 100;
    // int *p = &num;
    // int **pp = &p;
    // printf("%d\n", **pp);

    // int a = 5;
    // int *p = &a;
    // printf("%d\n", (*p)++);
    // printf("%d\n", a);

    // int x = 5;
    // int *p = &x;
    // int **q = &p;
    // printf("%d\n", *(*q));

    // int arr[] = {4, 5, 6, 7};
    // int *p = (arr + 1);
    // printf("%d ", *p++);
    // printf("%d\n", *p);

    // int x = 10;
    // int *p = &x;
    // int **q = &p;
    // int ***r = &q;
    // ***r = 20;
    // printf("x = %d\n", x);

    // int arr[] = {10, 20, 30, 40, 50};
    // int *p = arr;
    // int i;
    // for (i = 0; i < 5; i++)
    // {
    //     printf("%d ", *p);
    //     p++;
    // }

    // int a[] = {1, 2, 3, 4, 5};
    // int *p = (a + 1);
    // printf("%d\n", p[2]);

    // int a = 10, b = 20;
    // int *ptr = &a;

    // /*
    //     int *ptr;
    //     ptr = &a;
    // */

    // ptr = &b;
    // printf("%d\n", *ptr);

    // int x = 100;
    // int *p = &x;
    // int y = *p;
    // printf("%d\n", y);

    // int num = 50;
    // int *p = &num;
    // printf("%d\n", *p + 10);

    // int value = 5;
    // int *ptr;
    // ptr = &value;
    // printf("%d\n", *ptr * 2);

    // int data = 15;
    // int *ptr = &data;
    // printf("%d\n", ++(*ptr));

    // int a = 10, b = 20;
    // int *p1 = &a, *p2 = &b;
    // *p1 = *p2;
    // printf("%d %d\n", a, b);

    // int num = 30;
    // int *p = &num;
    // int **pp = &p;
    // printf("%d\n", **pp + 5);

    // int x = 10, y = 0;
    // int *p = &x, *q = &y;
    // *q = *p;
    // printf("%d %d\n", x, y);

    // int value = 10;
    // int *ptr = &value;
    // int temp = *ptr;
    // *ptr = 20;
    // printf("%d %d\n", temp, value);

    // int array[2] = {10, 20};
    // int *ptr = array;
    // printf("%d\n", *(ptr + 1));

    // int x = 10;
    // int *p = &x;
    // int **q = &p;
    // int ***r = &q;
    // ***r = 5;
    // printf("%d\n", x);

    // int values[] = {10, 20, 30};
    // int *p = values;
    // *(p++) += 10;
    // printf("%d %d\n", *p, values[0]);

    // int a = 10, b = 20;
    // int *p = &a, *q = &b;
    // *p = *q + 1;
    // q = p;
    // *q += 5;
    // printf("%d %d\n", a, b);

    // int a = 10, b = 20;
    // int *p = &a, *q = &b;
    // *p = *p + 1;
    // printf("%d %d\n", a, *p);

    // int x = 5;
    // int *p = &x;
    // int *q = p;
    // *q = *q + 10;
    // printf("%d %d\n", x, *p);

    // int array[] = {10, 20, 30};
    // int *p = array;
    // int *q = p + 1;
    // printf("%d\n", *q - *p);

    // int a = 10, b = 20;
    // int *p = &a;
    // int **q = &p;
    // *q = &b;
    // printf("%d %d\n", a, **q);

    // int x = 10, y = 20;
    // int *p = &x, *q = &y;
    // *p = (*q)++;
    // printf("%d %d %d\n", x, y, *p);

    // int x = 10, y = 20;
    // int *p = &x, *q = &y;
    // *p = *q + 2;
    // q = p;
    // *q += 3;
    // printf("%d %d\n", x, y);

    // int x = 5;
    // int *p = &x;
    // int **q = &p;
    // *p = 10;
    // **q = 15;
    // printf("%d %d\n", x, *p);

    // int values[] = {10, 20, 30};
    // int *p = values;
    // int *q = p + 1;
    // *p += *q;
    // printf("%d %d\n", *p, values[0]);

    // int x = 10;
    // int *p = &x;
    // int **q = &p;
    // **q = 20;
    // printf("%d %d %d\n", x, *p, **q);

    // int x = 5, y = 10;
    // int *p = &x, *q = &y;
    // int temp = *p;
    // *p = *q;
    // *q = temp;
    // printf("%d %d\n", x, y);

    // int x = 10, y = 20, z = 30;
    // int *p1 = &x, *p2 = &y, *p3 = &z;
    // *p1 = (*p2)++ + (*p3)--;
    // printf("%d %d %d\n", x, y, z);

    // int x = 100;
    // int *p = &x;
    // int **q = &p;
    // int ***r = &q;
    // ***r = 200;
    // printf("%d\n", x);

    // int a = 10, b = 20;
    // int *p = &a, *q = &b;
    // int **r = &p;
    // *r = q;
    // **r = 30;
    // printf("%d %d\n", a, b);

    // int array[] = {10, 20, 30, 40};
    // int *p = array;
    // int sum = 0;
    // for (int i = 0; i < 4; i++)
    // {
    //     sum += *(p + i);
    // }
    // printf("%d\n", sum);

    // int x = 5;
    // int *p = &x;
    // int **q = &p;
    // **q = **q + 10;
    // printf("%d %d\n", x, **q);

    // int nums[] = {1, 2, 3, 4, 5};
    // int *p = nums + 2;
    // printf("%d\n", *p);

    // int x = 10;
    // int *p = &x, *q = &x;
    // *p = *q + 3;
    // printf("%d %d\n", x, *q);

    // int x = 10;
    // int *p = &x, *q = &x;
    // *p += 5;
    // *q += 5;
    // printf("%d\n", x);

    // int x = 10;
    // int *p = &x;
    // int **q = &p;
    // int ***r = &q;
    // ***r = 20;
    // printf("%d %d %d\n", x, *p, **q);

    // int x = 10, y = 20;
    // int *p = &x, *q = &y;
    // *p = *q + 2;
    // *q = *p + 2;
    // printf("%d %d\n", x, y);

    int arr[5];

    printf("\n\n");
    return 0;
}
