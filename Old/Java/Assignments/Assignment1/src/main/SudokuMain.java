package main;

import sudoku.SudokuSolver;

public class SudokuMain {

    public static void main(String[] args) {
        int[][] grid = {
                { 5, 3, 0, 0, 7, 0, 0, 0, 0 },
                { 6, 0, 0, 1, 9, 5, 0, 0, 0 },
                { 0, 9, 8, 0, 0, 0, 0, 6, 0 },
                { 8, 0, 0, 0, 6, 0, 0, 0, 3 },
                { 4, 0, 0, 8, 0, 3, 0, 0, 1 },
                { 7, 0, 0, 0, 2, 0, 0, 0, 6 },
                { 0, 6, 0, 0, 0, 0, 2, 8, 0 },
                { 0, 0, 0, 4, 1, 9, 0, 0, 5 },
                { 0, 0, 0, 0, 8, 0, 0, 7, 9 },
        };
        int[][] grid2 = {
                { 5, 3, 0, 0, 0, 2, 2, 2, 2 },
                { 6, 0, 0, 1, 9, 5, 0, 0, 0 },
                { 0, 9, 8, 0, 0, 0, 0, 6, 0 },
                { 8, 0, 0, 0, 6, 0, 0, 0, 3 },
                { 4, 0, 0, 8, 0, 0, 0, 0, 1 },
                { 0, 0, 0, 0, 1, 0, 0, 0, 6 },
                { 0, 6, 0, 0, 0, 0, 2, 8, 0 },
                { 0, 0, 0, 0, 0, 0, 0, 0, 5 },
                { 0, 0, 0, 0, 8, 0, 0, 7, 9 },
        };

        System.out.println("\n--- input Grid 1---\n");

        // Create an instance of SudokuSolver
        SudokuSolver solver = new SudokuSolver();
        solver.printGrid(grid);
        // Solve the Sudoku puzzle for grid 1
        int[][] solvedGrid = solver.solveSudoku(grid);

        if (solvedGrid == null)
            System.out.println("\n \nNo solution exists for the given Sudoku puzzle.");
        else {
            System.out.println("\n--- Solved Grid 1---\n");
            solver.printGrid(solvedGrid);
        }
        ///////////////////////////////// Grid 2 ////////////////////

        System.out.println("\n--- input Grid 2---\n");
        solver.printGrid(grid2);

        int[][] solvedGrid2 = solver.solveSudoku(grid2);

        if (solvedGrid2 == null)
            System.out.println("\n \nNo solution exists for the given Sudoku puzzle.");
        else {
            System.out.println("\n--- Solved Grid 2---\n");
            solver.printGrid(solvedGrid2);
        }
    }
}