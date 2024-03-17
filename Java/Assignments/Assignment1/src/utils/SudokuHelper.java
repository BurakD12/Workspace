package utils;

public class SudokuHelper {

    public static boolean isValidRow(int[][] grid, int row, int num) {
        for (int i = 0; i < 9; i++) {
            if (grid[row][i] == num){
                return false;
            }
        }
        return true;
    }

    public static boolean isValidCol(int[][] grid, int col, int num) {
        for (int i = 0; i < 9; i++) {
            if (grid[i][col] == num) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidBox(int[][] grid, int row, int col, int num) {
        for (int i = 0; i < 3; i++){
            for (int j = 0; j < 3; j++){
                if (grid[row+i][col+j] == num){
                    return false;
                }
            }
        }
        return true;
    }
    
}
