package SingleThread;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Stack;

public class MazeGenerator {
    private Random random;
    
    public MazeGenerator() {
        this.random = new Random();
    }
    
    /**
     * Generates a perfect maze with the specified dimensions.
     * A perfect maze has exactly one solution path between any two points.
     * Only orthogonal movements (North, South, East, West) are allowed.
     * @param rows Number of rows in the maze (should be odd for best results)
     * @param cols Number of columns in the maze (should be odd for best results)
     * @return 2D array where 1 represents a road and 0 represents a wall
     */
    public int[][] createMaze(int rows, int cols) {
        // Ensure dimensions are odd for proper maze generation
        if (rows % 2 == 0) rows++;
        if (cols % 2 == 0) cols++;
        
        int[][] maze = new int[rows][cols];
        
        // Initialize maze with walls (all zeros)
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                maze[i][j] = 0;
            }
        }
        
        // Generate perfect maze using iterative depth-first search
        // This ensures exactly one path between any two points
        if (rows >= 3 && cols >= 3) {
            generatePerfectMaze(maze, rows, cols);
        }
        
        return maze;
    }
    
    /**
     * Generates a perfect maze using iterative depth-first search algorithm.
     * This guarantees exactly one solution path between any two points.
     */
    private void generatePerfectMaze(int[][] maze, int rows, int cols) {
        Stack<int[]> stack = new Stack<>();
        boolean[][] visited = new boolean[rows][cols];
        
        // Start from (1, 1)
        int startRow = 1;
        int startCol = 1;
        
        stack.push(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;
        maze[startRow][startCol] = 1;
        
        // Directions: North, East, South, West (only orthogonal movements)
        int[][] directions = {{-2, 0}, {0, 2}, {2, 0}, {0, -2}};
        
        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            int row = current[0];
            int col = current[1];
            
            // Get unvisited neighbors
            int[][] unvisitedNeighbors = getUnvisitedNeighbors(row, col, rows, cols, visited, directions);
            
            if (unvisitedNeighbors.length > 0) {
                // Choose a random unvisited neighbor
                int[] chosen = unvisitedNeighbors[random.nextInt(unvisitedNeighbors.length)];
                int newRow = chosen[0];
                int newCol = chosen[1];
                int wallRow = chosen[2];
                int wallCol = chosen[3];
                
                // Carve path to the neighbor
                maze[wallRow][wallCol] = 1; // Remove wall between cells
                maze[newRow][newCol] = 1;   // Mark neighbor as path
                visited[newRow][newCol] = true;
                
                // Push neighbor onto stack
                stack.push(new int[]{newRow, newCol});
            } else {
                // Backtrack if no unvisited neighbors
                stack.pop();
            }
        }
    }
    
    /**
     * Gets all unvisited neighbors of a cell that are exactly 2 steps away (orthogonal only)
     */
    private int[][] getUnvisitedNeighbors(int row, int col, int maxRows, int maxCols, 
                                          boolean[][] visited, int[][] directions) {
        int count = 0;
        int[][] temp = new int[4][4]; // Max 4 directions
        
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            int wallRow = row + dir[0] / 2;
            int wallCol = col + dir[1] / 2;
            
            // Check if the neighbor is valid and unvisited
            if (newRow > 0 && newRow < maxRows - 1 && 
                newCol > 0 && newCol < maxCols - 1 && 
                !visited[newRow][newCol]) {
                
                temp[count][0] = newRow;
                temp[count][1] = newCol;
                temp[count][2] = wallRow;
                temp[count][3] = wallCol;
                count++;
            }
        }
        
        // Copy valid neighbors to result array
        int[][] result = new int[count][4];
        for (int i = 0; i < count; i++) {
            result[i] = temp[i];
        }
        
        return result;
    }
    
    /**
     * Saves a maze to a file with starting and ending points marked
     */
    private void saveMazeToFile(int[][] maze, String filename, int startRow, int startCol, int endRow, int endCol) throws IOException {
        File directory = new File("mazes");
        if (!directory.exists()) {
            directory.mkdir();
        }
        
        File file = new File(directory, filename);
        try (FileWriter writer = new FileWriter(file)) {
            // Write starting and ending points on the first line
            writer.write("START: " + startRow + "," + startCol + " END: " + endRow + "," + endCol + "\n");
            
            // Write the maze
            for (int i = 0; i < maze.length; i++) {
                for (int j = 0; j < maze[i].length; j++) {
                    writer.write(maze[i][j] + " ");
                }
                writer.write("\n");
            }
        }
    }
    
    /**
     * Finds valid starting and ending points in the maze
     * Returns int array: [startRow, startCol, endRow, endCol]
     */
    private int[] findStartAndEndPoints(int[][] maze) {
        int rows = maze.length;
        int cols = maze[0].length;
        
        // Find first valid cell (path) as start point
        int startRow = -1, startCol = -1;
        for (int i = 0; i < rows && startRow == -1; i++) {
            for (int j = 0; j < cols; j++) {
                if (maze[i][j] == 1) {
                    startRow = i;
                    startCol = j;
                    break;
                }
            }
        }
        
        // Find last valid cell (path) as end point
        int endRow = -1, endCol = -1;
        for (int i = rows - 1; i >= 0 && endRow == -1; i--) {
            for (int j = cols - 1; j >= 0; j--) {
                if (maze[i][j] == 1) {
                    endRow = i;
                    endCol = j;
                    break;
                }
            }
        }
        
        return new int[]{startRow, startCol, endRow, endCol};
    }
    
    /**
     * Generates multiple mazes with different dimensions and saves them to files
     */
    public void generateMaze() {
        try {
            // Generate mazes of different sizes
            int[][] maze1 = createMaze(5, 5);
            int[] points1 = findStartAndEndPoints(maze1);
            saveMazeToFile(maze1, "maze_5x5.txt", points1[0], points1[1], points1[2], points1[3]);
            System.out.println("Generated maze_5x5.txt");
            
            int[][] maze2 = createMaze(10, 10);
            int[] points2 = findStartAndEndPoints(maze2);
            saveMazeToFile(maze2, "maze_10x10.txt", points2[0], points2[1], points2[2], points2[3]);
            System.out.println("Generated maze_10x10.txt");
            
            int[][] maze3 = createMaze(15, 15);
            int[] points3 = findStartAndEndPoints(maze3);
            saveMazeToFile(maze3, "maze_15x15.txt", points3[0], points3[1], points3[2], points3[3]);
            System.out.println("Generated maze_15x15.txt");
            
            int[][] maze4 = createMaze(20, 10);
            int[] points4 = findStartAndEndPoints(maze4);
            saveMazeToFile(maze4, "maze_20x10.txt", points4[0], points4[1], points4[2], points4[3]);
            System.out.println("Generated maze_20x10.txt");
            
            int[][] maze5 = createMaze(25, 25);
            int[] points5 = findStartAndEndPoints(maze5);
            saveMazeToFile(maze5, "maze_25x25.txt", points5[0], points5[1], points5[2], points5[3]);
            System.out.println("Generated maze_25x25.txt");
            
            System.out.println("\nAll mazes generated successfully in 'mazes' folder!");
        } catch (IOException e) {
            System.err.println("Error writing maze files: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        MazeGenerator generator = new MazeGenerator();
        generator.generateMaze();
    }
}
