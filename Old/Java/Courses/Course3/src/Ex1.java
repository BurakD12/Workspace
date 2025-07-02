import java.util.HashSet;
import java.util.Set;

/**
 * Ex1
 */
public class Ex1 {

    // Find Duplicates in Array: Implement a Java program to find all duplicates in
    // an integer array. 
    // public static void main(String[] args) {
    //     // 1 9 7 3 2 4 9 8 6
    //     int[] arrMain = new int[] { 1, 9, 7, 3, 2, 4, 9, 8, 9, 7, 4 };
    //     Set<Integer> seen = new HashSet<>();

    //     for (int i = 0; i < arrMain.length; i++) {
    //         for (int j = i + 1; j < arrMain.length; j++) {
    //             if (arrMain[i] == arrMain[j]) {
    //                 seen.add(arrMain[i]);
    //             }
    //         }
    //     }

    //     System.out.print("The list of duplicate numbers are: " + seen.toString());
    // }

    // public static void main(String[] args) {
    //     int[] arrMain = new int[] { 1, 9, 7, 3, 2, 4, 9, 8, 9, 7, 4 };
        
    //     for (int i = 0; i < arrMain.length; i++) {
    //         int found = 0;

    //         for (int j = i + 1; j < arrMain.length ; j++) {
    //             if (arrMain[i] == arrMain[j] && arrMain[i] != Integer.MIN_VALUE) {
    //                 found = 1;
    //                 arrMain[j] = Integer.MIN_VALUE;
    //             }
    //         }

    //         if (found == 1) {
    //             System.out.println(arrMain[i] + " Repeats");
    //         }
    //     }
    // }
}