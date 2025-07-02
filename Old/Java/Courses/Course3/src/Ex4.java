import java.util.Scanner;

/**
 * Ex4
 */
public class Ex4 {

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);

        System.out.print("Enter the initial string: ");
        String str = scan.nextLine();

        System.out.print("Enter 2 characters to replace each other: ");

        char char1 = scan.nextLine().charAt(0);
        char char2 = scan.nextLine().charAt(0);
        String newStr = charReplace(char1, char2, str);

        System.out.println(newStr);
    }

    // Write a function that takes a String str, Char old, Char new as parameter and
    // replaces all 'old' chars with 'new' one.
    static String charReplace(char char1, char char2, String str) {
        char[] charArr = str.toCharArray();

        for (int i = 0; i < charArr.length; i++) {
            if (charArr[i] == char1) {
                charArr[i] = char2;
            }
        }

        String newStr = new String(charArr);
        return newStr;

    }
}