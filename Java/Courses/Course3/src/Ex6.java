import java.util.Scanner;

/**
 * Ex6
 */
public class Ex6 {

    // Write a java method that finds how many words present in a given string.

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        
        System.out.print("Enter your sentence: ");
        String str = scan.nextLine();

        int count = getWordCount(str);
        System.out.println("The word count for your sentence is: " + count);

    }

    static int getWordCount(String str) {
        String[] strArr = str.split(" ");
        int count = 0;
        for (String testString : strArr) {
            if (testString.length() > 0)
                count++;
        }
        return count;
    }
}