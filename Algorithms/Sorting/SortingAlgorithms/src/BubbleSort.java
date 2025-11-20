import java.util.Random;

public class BubbleSort {
    public static void main(String[] args) throws Exception {
        for (int index = 1; index < 7; index++) {
            int len = (int) Math.pow(10, index);
            int[] array = createRandomArray(len);

            long startTime = System.currentTimeMillis();
            bubbleSort(array);
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

    public static void bubbleSort(int[] array) {
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array.length - i - 1; j++) {
                if (array[j] > array[j + 1]){
                    int ph = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = ph;
                }
            }
        }
    }
}
