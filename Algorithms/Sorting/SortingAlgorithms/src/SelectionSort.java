import java.util.Random;

public class SelectionSort {
    public static void main(String[] args) throws Exception {
        for (int index = 1; index < 7; index++) {
            int len = (int) Math.pow(10, index);
            int[] array = createRandomArray(len);

            long startTime = System.currentTimeMillis();
            selectionSort(array);
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

    public static void selectionSort(int[] array) {
        for (int i = 0; i < array.length; i++) {
            int min_index = i;
            for (int j = i + 1; j < array.length; j++) {
                if (array[min_index] > array[j]) {
                    min_index = j;
                }
            }
            if (min_index != i) {
                int ph = array[min_index];
                array[min_index] = array[i];
                array[i] = ph;
            }
        }
    }
}
