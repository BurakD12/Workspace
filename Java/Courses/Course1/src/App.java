import java.util.Scanner;

public class App {
    public static void main(String[] args) throws Exception {
        /*
         * Ask user to enter his name and age.
         * Then print a message saying "Hello [name], you are [age] years old."
         */
        // Scanner scan = new Scanner(System.in);

        // System.out.print("Enter your name: ");
        // String name = scan.nextLine();

        // System.out.print("Enter your age: ");
        // int age = scan.nextInt();

        // System.out.println("Hello " + name + ", you are " + age + " years old.");

        // -------------------------------------------------------------------------------

        /*
         * Question: Ask user to enter a number then print whether the number is even or
         * odd.
         * 
         */

        // Scanner scan = new Scanner(System.in);
        // System.out.print("Enter your number: ");
        // int num = scan.nextInt();
        // if(num % 2 == 0){
        // System.out.println("The number " + num + " is even.");
        // }
        // else {
        // System.out.println("The number " + num + " is odd.");
        // }

        // -------------------------------------------------------------------------------
        /*
         * Question: Ask user to enter a number then print whether the number is
         * positive, negative or zero.
         */

        // Scanner scan = new Scanner(System.in);
        // System.out.print("\nEnter a number: ");
        // int num = scan.nextInt();

        // if (num > 0){
        // System.out.println("The number " + num + " is positive.");
        // }
        // else if (num == 0){
        // System.out.println("The number 0 is zero.");
        // }
        // else {
        // System.out.println("The number " + num + " is negative.");
        // }

        // -------------------------------------------------------------------------------

        /*
         * Question: Ask user to enter a number then print ıts factorial.
         */

        // Scanner scan = new Scanner(System.in);
        // System.out.print("\nEnter a number: ");
        // int num = scan.nextInt();
        // int place_holder = num;
        // int res = 1;
        // for(int i = 0; num > i; num--){
        // res = res * num;
        // }
        // System.out.println("The factorial of the number " + place_holder + " is: " +
        // res);

        // -------------------------------------------------------------------------------

        /*
         * Question: Ask user to enter a number then print whether the number is prime
         * or not.
         */

        // Scanner scan = new Scanner(System.in);
        // System.out.print("Enter a number: ");
        // int num = scan.nextInt();

        // boolean isPrime = true;
        // for(int i = 2; i < num; i++){
        // if(num % i == 0){
        // isPrime = false;
        // break; // No need to check further if we found a divisor
        // }
        // }

        // if(isPrime && num > 1) {
        // System.out.println("The number " + num + " is prime.");
        // }
        // else {
        // System.out.println("The number " + num + " is not prime.");
        // }

        // -------------------------------------------------------------------------------
        /*
         * Question: Ask user to enter a number then print the number of digits in that
         * number.
         */

        // Scanner scan = new Scanner(System.in);
        // System.out.print("\nEnter a number: ");
        // int num = scan.nextInt();
        // int place_holder = num;
        // int count = num != 0 ? 0 : 1; 

        // /*
        //     if (num != 0) {
        //         count = 0;
        //     } else {
        //         count = 1;
        //     }
        // */

        // while (num != 0) {
        //     num = num / 10;
        //     count++;
        // }

        // System.out.println("The number " + place_holder + " has " + count + " digits.");

        // -------------------------------------------------------------------------------
        /*
            Question: Ask user to enter a number then print its reverse.
        */

        // Scanner scan = new Scanner(System.in);
        // System.out.print("\nEnter a number: ");
        // int num = scan.nextInt();
        // int rev = 0;
        // int place_holder = num;

        // while (place_holder != 0) {
        //     int rem = place_holder % 10;
        //     rev = rev * 10 + rem; 
        //     place_holder /= 10; // Remove the last digit from place_holder
        // }

        // System.out.println("The reverse of the number " + num + " is: " + rev);

        // -------------------------------------------------------------------------------
        /*
            Question: Ask user to enter a number then print the sum of its digits.
        */

        // Scanner scan = new Scanner(System.in);
        // System.out.print("\nEnter a number: ");
        // int num = scan.nextInt();
        // int place_holder = Math.abs(num);
        // int sum = 0;
        // while (place_holder != 0) {
        //     sum = sum + (place_holder % 10);
        //     place_holder /= 10;
        // }
        // System.out.println("The sum of the digits of the number " + num + " is: " + sum);

        System.out.println("Hello, World! This is a placeholder for future code examples.");

    }
}
