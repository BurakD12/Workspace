import java.util.Random;

public class InsertionSort {
    public static void main(String[] args) throws Exception {
        for (int i = 1; i <= 7; i++)
        {
            int len = (int) Math.pow(10, i);
            int[] array = createRandomArray(len);

            long startTime = System.nanoTime();
            insertionSort(array);
            long endTime = System.nanoTime();

            // Calculate elapsed time in milliseconds
            long elapsedTime = (endTime - startTime) / 1_000_000;
            System.out.println("Time taken to sort array of length " + len + ": " + elapsedTime + " ms");
        }
    }   

    public static int[] createRandomArray(int len) {
        int[] newArray = new int[len];
        Random rand = new Random();

        for (int i = 0; i < len; i++) {
            newArray[i] = rand.nextInt(len);
        }
        return newArray;
    }

    public static void insertionSort(int[] array) {
        for (int i = 1; i < array.length; i++) {
            int key = array[i];
            int j = i - 1;
            while (j >= 0 && array[j] > key) {
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = key;
        }
    }
}
