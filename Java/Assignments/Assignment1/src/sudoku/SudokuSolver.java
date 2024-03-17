package sudoku;

import utils.SudokuHelper;

public class SudokuSolver {
    int[][] grid;
    int[][] solution = new int[9][9];
    static final int SIZE = 9;

    public void printGrid(int[][] grid) {
        for (int i = 0; i < SIZE; i++) {
            System.out.println();
            for (int j = 0; j < SIZE; j++) {
                System.out.print(grid[i][j] + " ");
            }
        }
    }
   
    public int[][] solveSudoku(int[][] grid) {
        this.grid = grid;
        if (solve()) {
            return this.grid;
        }
        else
            return null;
    }

    public boolean solve() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (grid[i][j] == 0) {
                    for (int k = 1; k < 10; k++) {
                        if (is_valid_move(i, j, k)) {
                            grid[i][j] = k;
                            if (solve()) {
                                return true;
                            }
                            grid[i][j] = 0; // backtrack
                        }
                    }
                    return false; // backtrack
                }
            }
        }
        return true;
    }

    private boolean is_valid_move(int row, int col, int num) {
       return SudokuHelper.isValidRow(grid,row,num)
       && SudokuHelper.isValidCol(grid,col,num)
               && SudokuHelper.isValidBox(grid, row - (row % 3), col - (col % 3), num);
    
    }
}
