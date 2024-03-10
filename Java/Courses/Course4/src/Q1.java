import java.util.Scanner;

public class Q1 {
    public static void main(String[] args) {
        // Write a function named isPalindrome that takes a String as a parameter and
        // returns true if the string is a palindrome (reads the same forwards and
        // backwards), and false otherwise.

        Scanner scan = new Scanner(System.in);
        System.out.print("Enter your word: ");
        String str = scan.nextLine();

        if (isPalindrome(str)) {
            System.out.println("This is a palindrome!");
        } else {
            System.out.println("This is not a palindrome!");
        }
    }

    public static boolean isPalindrome(String str) {
        int n = str.length();
        String strcpy = new String(str).toLowerCase();

        for (int i = 0; i < n / 2; i++) {
            if (strcpy.charAt(i) != strcpy.charAt(n - 1 - i)) {
                return false;
            }
        }
        return true;
    }
}
