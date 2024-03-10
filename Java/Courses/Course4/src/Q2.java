import java.util.Arrays;
import java.util.Scanner;

/**
 * Q2
 */
public class Q2 {

    public static void main(String[] args) {
        // Sort Array of Strings by Length
        // Write a function named sortByLength that takes an array of Strings as a
        // parameter and returns the array sorted by the length of the strings, from
        // shortest to longest.
        
        Scanner scan = new Scanner(System.in);
        System.out.print("Enter how many Strings do you want: ");
        int n = scan.nextInt();
        String[] arr1 = new String[n];
        scan.nextLine();

        for (int i = 0; i < n; i++) {
            System.out.print("Enter String number " + (i + 1) + ": ");
            arr1[i] = scan.nextLine();
        }

        String[] arr = sortByLength(arr1);
        System.out.println(Arrays.toString(arr));
    }

    public static String[] sortByLength(String[] arr) {
        String[] copyArray = new String[arr.length];
        System.arraycopy(arr, 0, copyArray, 0, arr.length);

        for (int i = 0; i < copyArray.length - 1; i++) {
            for (int j = 0; j < copyArray.length - 1 - i; j++) {
                if (copyArray[j].length() > copyArray[j+1].length()) {
                    String newString = copyArray[j];
                    copyArray[j] = copyArray[j+1];
                    copyArray[j+1] = newString;
                }
            }
        }

        return copyArray;
    }

}