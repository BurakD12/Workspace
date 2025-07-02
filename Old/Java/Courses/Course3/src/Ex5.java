import java.util.Scanner;

/**
 * Ex5
 */
public class Ex5 {

    public static void main(String[] args) {
        // Scanner scan = new Scanner(System.in);
        // System.out.print("Enter a binary number: ");
        
        // String str = scan.nextLine();
        // int decVal = binToDec(str);

        // System.out.println(decVal);
    }

    // Convert Binary to Decimal: Write a Java method that converts a binary number
    // (as a string) to its decimal equivalent.

    // static int binToDec(String str) {
    //     int decVal = 0;
    //     char[] binVal = str.toCharArray();
    //     // 0000 1101
    //     for (int i = (binVal.length - 1); i >= 0; i--) {
    //         if (binVal[i] == '1') {
    //             decVal += Math.pow(2, (binVal.length - 1) - i);
    //         }
    //     }
    //     return decVal;
    // }

    // static int binToDec(String str) {
    //     int decVal = 0;
    //     int digit = 0;
    //     char[] charArr = str.toCharArray();

    //     for (int i = charArr.length-1; i >= 0; i--, digit++) {
    //         decVal += Math.pow(2, digit) * (charArr[i] - '0');  
    //     }
    //     return decVal;
    // }
}