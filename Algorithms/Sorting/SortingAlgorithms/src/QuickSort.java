import java.util.Random;

public class QuickSort {
    public static void main(String[] args) throws Exception {
        for (int index = 1; index < 7; index++) {
            int len = (int) Math.pow(10, index);
            int[] array = createRandomArray(len);

            long startTime = System.currentTimeMillis();
            quickSort(array, 0, array.length - 1);
            long endTime = System.currentTimeMillis();

            System.out.println("Array length: " + len + " - Time taken: " + (endTime - startTime) + " ms");
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

    public static void quickSort(int[] array, int leftPointer, int rightPointer) {
        if (rightPointer > leftPointer) {
            int pivotElement = partition(array, leftPointer, rightPointer);
            quickSort(array, leftPointer, pivotElement - 1);
            quickSort(array, pivotElement + 1, rightPointer);
        }
    }

    public static int partition(int[] array, int leftPointer, int rightPointer) {
        int pivot = array[rightPointer];
        int i = leftPointer - 1;
        for (int j = leftPointer; j < rightPointer; j++) {
            if (array[j] <= pivot) {
                i++;
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
            }
        }

        int temp = array[i + 1];
        array[i + 1] = array[rightPointer];
        array[rightPointer] = temp;

        return i + 1;
    }
}
