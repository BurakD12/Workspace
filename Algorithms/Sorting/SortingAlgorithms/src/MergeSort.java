import java.util.Random;

public class MergeSort {
    public static void main(String[] args) throws Exception {
        for (int i = 1; i <= 7; i++)
        {
            int len = (int) Math.pow(10, i);
            int[] array = createRandomArray(len);

            long startTime = System.nanoTime();
            mergeSort(array, 0, len-1);
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

    public static void mergeSort(int[] array, int l, int r) {
        if (l < r) {
            int q = (l + r) / 2;
            mergeSort(array, l, q);
            mergeSort(array, q + 1, r);
            merge(array, l, q, r);
        }
        
    }

    public static void merge(int[] array, int l, int q, int r) {

        // 10 20 30 40 50 60
        // 0  1  2  3  4  5
        // l = 0, q = 2, r = 5

        int n1 = q - l + 1;
        int n2 = r - q;

        int[] left = new int[n1];
        int[] right = new int[n2];

        for (int i = 0; i < n1; i++) {
            left[i] = array[l + i];
        }

        for (int j = 0; j < n2; j++) {
            right[j] = array[(q + 1) + j];
        }

        int leftPointer = 0;
        int rightPointer = 0;
        int mainPointer = l;

        while (leftPointer < n1 && rightPointer < n2) {
            if (left[leftPointer] <= right[rightPointer]) {
                array[mainPointer] = left[leftPointer++];
            } else {
                array[mainPointer] = right[rightPointer++];
            }
            mainPointer++;
        }
        while (leftPointer < n1) {
            array[mainPointer++] = left[leftPointer++];
        }
        while (rightPointer < n2) {
            array[mainPointer++] = right[rightPointer++];
        }
    }
}
