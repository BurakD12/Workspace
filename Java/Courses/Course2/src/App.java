import java.util.Scanner;

public class App {
    public static void main(String[] args) throws Exception {
        // int[] numbers = {3,4,5,1};

        // for(int num : numbers){
        //     System.out.println("Number: " + num);
        // }

        // --------------------------------------------------------------------------

        /*
            Question: Ask user to enter 5 numbers and store them in an array.
            Then print the numbers in reverse order.
        */

        // Scanner scan = new Scanner(System.in);
        // int[] array = new int[5];
        // for(int i = 0; i < 5; i++){
        //     System.out.print("Enter the " + (i + 1) + ". number: ");
        //     array[i] = scan.nextInt();
        // } 

        // System.out.println("Numbers in reverse order:");
        // for (int i = 4; i >= 0; i--){
        //     System.out.println("Number: " + array[i]);
        // }
        
        // --------------------------------------------------------------------------

        /*
            Question: Ask user to enter 5 numbers and store them in an array.
            Then print the numbers in reverse order using a while loop.
        */

        // Scanner scan = new Scanner(System.in);
        // int[] array = new int[5];
        // int i = 0;
        // while (i < 5) {
        //     System.out.print("Enter the " + (i + 1) + ". number: ");
        //     array[i] = scan.nextInt();
        //     i++;
        // }

        // System.out.println("Numbers in reverse order: ");
        // while (i > 0) {
        //     System.out.println("Number: " + array[(i - 1)]);
        //     i--;
        // }

        /*
            Question: Ask user to enter 5 numbers and store them in an array.
            Then sort the numbers in ascending order and print them.
        */

        // Scanner scan = new Scanner(System.in);
        // int[] array = new int[5];
        
        // for (int i = 0; i < 5; i++) {
        //     System.out.print("Enter the " + (i + 1) + ". number: ");
        //     array[i] = scan.nextInt();
        // }

        // for (int i = 0; i < array.length; i++){
        //     for (int j = 0; j < (array.length - 1); j++) {
        //         if (array[j] > array[j + 1]){
        //             int place_holder = array[j];
        //             array[j] = array[j + 1];
        //             array[j + 1] = place_holder;
        //         }
        //     }
        // }

        // System.out.println("The sorted array: ");
        // for (int num = 0; num < array.length; num++) {
        //     System.out.println(array[num]);
        // }

        // --------------------------------------------------------------------------

        /*
            Question: Define a matrix with 3 rows and 3 columns.
            Then ask the user to enter the elements of the matrix.
            Finally, print the matrix.
        */

        // Scanner scan = new Scanner(System.in);
        // int[][] matrix = new int[3][3];
        // System.out.println("Fill in the matrix: ");
        // for (int i = 0; i < 3; i++){
        //     for (int j = 0; j < 3; j++){
        //         System.out.println("Enter the number for row " + (i + 1) + " and column " + (j + 1) + ": ");
        //         matrix[i][j] = scan.nextInt();
        //     }
        // }

        // for (int i = 0; i < 3; i++){
        //     System.out.print("\nRow " + (i + 1) + ":  ");
        //     for (int j = 0; j < 3; j++){
        //         System.out.print(matrix[i][j] + "  ");
        //     }
        // }

        /*
            Question: Define a matrix with 3 rows and 3 columns.
            Then ask the user to enter the elements of the matrix.
            Finally, print the matrix in a formatted way.

            Row 1: 1  2  3      AVG: 2.0        MIN: 1        MAX: 3
            Row 2: 4  5  6      AVG: 5.0        MIN: 4        MAX: 6
            Row 3: 7  8  9      AVG: 8.0        MIN: 7        MAX: 9
        */

        // Scanner scan = new Scanner(System.in);
        // int[][] matrix = new int[3][3];
        // System.out.println("Fill in the matrix: ");
        // for (int i = 0; i < 3; i++){
        //     for (int j = 0; j < 3; j++){
        //         System.out.println("Enter the number for row " + (i + 1) + " and column " + (j + 1) + ": ");
        //         matrix[i][j] = scan.nextInt();
        //     }
        // }

        // for (int i = 0; i < 3; i++){
        //     System.out.print("\nRow " + (i + 1) + ":  ");
        //     int min = matrix[i][0];
        //     int max = matrix[i][0];
        //     int sum = 0;
        //     for (int j = 0; j < 3; j++){
        //         if(min > matrix[i][j]){
        //             min = matrix[i][j];
        //         }
        //         if(max < matrix[i][j]){
        //             max = matrix[i][j];
        //         }
        //         sum += matrix[i][j];
        //         System.out.print(matrix[i][j] + "  ");
        //     }
        //     System.out.print("  AVG: " + sum / 3 + "    MIN: " + min + "    MAX: " + max);
        // }

        /*
            Question: Implement a function that takes a string as input and cleans it up by removing extra spaces, tabs, and newlines.
            The function should return the cleaned string.
            
            Example:
            Input: "  Hello   World! \n\n  This is a   test.  "
            Output: "Hello World! This is a test."

            Note: You can use the String class methods to manipulate the string.
        */

        System.out.println("Cleaned String: " + StringCleaner("  Hello   World! \n\n  This is a   test.  "));
    }

    static String StringCleaner(String string){
        // "Hello   World! \n\n  This is a   test."
        // "Hello World! This is a test."
        // First, we will remove leading and trailing spaces
        String finalString = "";
        String cleanedString = string.trim();
        cleanedString = cleanedString.replace("\n"," ");
        int count = 0;
        for (int i = 0; i < cleanedString.length(); i++){
            if (cleanedString.charAt(i) != ' '){
                finalString += cleanedString.charAt(i);
                count = 0;
            }
            else if (cleanedString.charAt(i) == ' ' && count == 0){
                finalString += ' ';
                count++;
            }
            
        }
        return finalString;

    }
}
