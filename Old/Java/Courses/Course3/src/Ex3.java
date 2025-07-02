import java.util.Scanner;

/**
 * Ex3
 */
public class Ex3 {

    // Count Vowels in a String: Write a Java program that counts the number of
    // vowels in a given string.
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);

        System.out.print("Enter your sentence: ");
        String str = scan.nextLine();

        char[] charArr = str.toCharArray();

        int count = 0;

        //   iterator : collection
        for (char ch : charArr) { // For Each / Extended For Loop
            switch (ch) {
                case 'a' -> count++;
                case 'e' -> count++;
                case 'i' -> count++;
                case 'o' -> count++;
                case 'u' -> count++;
            }
        }

        System.out.println("There are " + count + " vowels.");
    }
}