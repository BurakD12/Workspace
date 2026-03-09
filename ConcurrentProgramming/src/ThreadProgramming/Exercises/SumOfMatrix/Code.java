package ThreadProgramming.Exercises.SumOfMatrix;

import java.util.Random;

public class Code {

    public static void main(String[] args) {
        System.out.println("\033[H\033[2J");
        int[][] matrix = new int[10000][10000];
        fillMatrixWithRandomValues(matrix);
        int[] threadCounts = {1, 2, 4, 8, 16, 32, 64};

        for (int threadCount : threadCounts) {
            Thread[] threads = new Thread[threadCount];
            float interval = (float) matrix.length / threadCount;    
            float startIndex = 0; 
            float endIndex = interval;
            for (int i = 0; i < threadCount; i++) {
                int start;
                int end;
                if (startIndex % 1 == 0) {
                    start = (int) startIndex;
                } else { 
                    start = (int) startIndex + 1;
                }
                if (endIndex % 1 == 0) {
                    end = (int)endIndex - 1;
                } else {
                    end = (int)endIndex;
                }
                startIndex += interval;
                endIndex += interval;

                threads[i] = new MyThread("Thread-" + (i + 1), matrix, start, end);
            }

            long startTime = System.currentTimeMillis();
            for (int i = 0; i < threadCount; i++) {
                threads[i].start();
            }
            for (int i = 0; i < threadCount; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            long sum = 0;
            for (int i = 0; i < threadCount; i++) {
                sum += ((MyThread)threads[i]).getTotalSum();
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Total sum is: " + sum + " for " + threadCount + " thread(s). In " + (endTime - startTime) +  " milliseconds.");
        }
    }


    public static void fillMatrixWithRandomValues(int[][] matrix){
        Random random = new Random();
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                matrix[i][j] = random.nextInt(10);
            }
        }
    }
}