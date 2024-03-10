import java.util.Scanner;

/**
 * Ex2
 */
public class Ex2 {

    // Take an int from user and then calculate 'reversedNum' variable that stores the reversed of taken input
    public static void main(String[] args) {
        // 0
        // rem = 1
        // rev = 54321

        Scanner scan = new Scanner(System.in);
        System.out.print("Enter a number: ");
        int x = scan.nextInt(), y = 0;

        while (x > 0){
            y = y * 10;
            int a = x % 10;
            y += a;
            x = x / 10;
        }
        System.out.println("The reversed number is: " + y);
    }
}