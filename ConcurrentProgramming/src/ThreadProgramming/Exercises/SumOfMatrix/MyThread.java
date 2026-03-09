package ThreadProgramming.Exercises.SumOfMatrix;

public class MyThread extends Thread{
    long totalSum;
    int[][] matrix;
    int rowStartIndex;
    int rowEndIndex;

    public MyThread(String name, int[][] matrix, int rowStartIndex, int rowEndIndex){
        super(name);
        this.matrix = matrix;
        this.rowStartIndex = rowStartIndex;
        this.rowEndIndex = rowEndIndex;
    }

    @Override
    public void run() {
        for (int i = rowStartIndex; i <= rowEndIndex; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                totalSum += matrix[i][j];
            }
        }
    }
    
    public long getTotalSum() {
        return totalSum;
    }
}