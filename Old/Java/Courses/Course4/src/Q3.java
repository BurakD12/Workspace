import java.util.Arrays;

/**
 * Q3
 */
public class Q3 {

    public static void main(String[] args) {
        // Rotate Array
        // Implement a function named rotateArray that takes an array of integers and a number n as parameters. The function should rotate the array to the right by n positions.
        // D E A B C
        // A B C D E

        int[] arr = { 1, 2, 3, 4, 5, 6, 7 };
        int n = 3;
        rotateArray(arr, n);

        System.out.println(Arrays.toString(arr));
    }

    public static void rotateArray(int[] arr, int n) {
        int len = arr.length;

        n = n % len;

        int[] temp = new int[len];
        
        for(int i = 0; i < n; i++)
            temp[i] = arr[len - n + i];

        for (int i = n; i < len; i++)
            temp[i] = arr[i - n];

        for (int i = 0; i < len; i++)
            arr[i] = temp[i];
    }
}