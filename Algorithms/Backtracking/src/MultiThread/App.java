package MultiThread;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Random;

public class App {
    public static void main(String[] args) throws Exception {
        // Create a large maze (50x50)
        int size = 50;
        int[][] maze = createLargeMaze(size);

        int startX = 1, startY = 1;
        // Ensure end point is at an odd position (maze generation works on odd positions)
        int endX = (size - 2) % 2 == 0 ? size - 3 : size - 2;
        int endY = (size - 2) % 2 == 0 ? size - 3 : size - 2;

        System.out.println("Solving maze using thread-based parallel exploration...");
        System.out.println("Start: [" + startX + ", " + startY + "]");
        System.out.println("End: [" + endX + ", " + endY + "]");
        System.out.println();

        // Create a copy of the maze for thread-based solving
        int[][] mazeCopy = new int[maze.length][];
        for (int i = 0; i < maze.length; i++) {
            mazeCopy[i] = maze[i].clone();
        }

        // Verify maze has a solution before solving
        System.out.println("Verifying maze connectivity...");
        if (verifyMazeConnectivity(mazeCopy, startX, startY, endX, endY)) {
            System.out.println("✓ Maze is solvable - start and end are connected!");
        } else {
            System.out.println("⚠ Warning: Start and end may not be connected!");
        }
        System.out.println();

        // Use thread-based solver
        LinkedList<int[]> path = solveMazeThreaded(mazeCopy, startX, startY, endX, endY);

        if (path != null && !path.isEmpty()) {
            System.out.println("\n✓ Maze solved! Path length: " + path.size() + " steps");
            System.out.println("Path taken (first 20 and last 20 steps):");
            int showSteps = Math.min(20, path.size());
            for (int i = 0; i < showSteps; i++) {
                System.out.println("[" + path.get(i)[0] + ", " + path.get(i)[1] + "]");
            }
            if (path.size() > 40) {
                System.out.println("... (" + (path.size() - 40) + " steps hidden) ...");
            }
            if (path.size() > 20) {
                for (int i = Math.max(showSteps, path.size() - 20); i < path.size(); i++) {
                    System.out.println("[" + path.get(i)[0] + ", " + path.get(i)[1] + "]");
                }
            }
            System.out.println("[" + endX + ", " + endY + "]");
        } else {
            System.out.println("\n✗ No solution found for the maze.");
        }
    }

    /**
     * Thread-based maze solver that spawns threads for each available direction
     */
    public static LinkedList<int[]> solveMazeThreaded(int[][] maze, int startX, int startY, int endX, int endY) {
        MazeSharedState sharedState = new MazeSharedState(maze, startX, startY, endX, endY);
        LinkedList<int[]> initialPath = new LinkedList<>();
        
        // Display initial state
        System.out.println("Initial maze state:");
        sharedState.displayMaze();
        
        // Create the first explorer thread
        MazeExplorer explorer = new MazeExplorer(sharedState, startX, startY, initialPath, -1, -1);
        explorer.start();
        
        try {
            explorer.join(); // Wait for all threads to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        
        // Final display
        System.out.println("\n=== FINAL STATE ===");
        sharedState.displayMaze();
        
        return sharedState.getSolution();
    }

    public static boolean solveMaze(int[][] maze, int x, int y, int endX, int endY, LinkedList<int[]> path) {
        path.add(new int[] { x, y });
        while (!isWon(x, y, endX, endY) && !isLost(maze, x, y)) {
            if (path.peekLast()[1] != y + 1 && lookRight(maze, x, y) == 1) {
                int[] currentBlock = { x, y };
                if (currentBlock[0] != path.peekLast()[0] || currentBlock[1] != path.peekLast()[1]) {
                    path.add(currentBlock);    
                }
                y++;
            } else if (path.peekLast()[0] != x + 1 || lookDown(maze, x, y) == 1) {
                int[] currentBlock = { x, y };
                if (currentBlock[0] != path.peekLast()[0] || currentBlock[1] != path.peekLast()[1]) {
                    path.add(currentBlock);    
                }
                x++;
            } else if (path.peekLast()[1] != y - 1 && lookLeft(maze, x, y) == 1) {
                int[] currentBlock = { x, y };
                if (currentBlock[0] != path.peekLast()[0] || currentBlock[1] != path.peekLast()[1]) {
                    path.add(currentBlock);    
                }
                y--;
            } else if (path.peekLast()[0] != x - 1 && lookUp(maze, x, y) == 1) {
                int[] currentBlock = { x, y };
                if (currentBlock[0] != path.peekLast()[0] || currentBlock[1] != path.peekLast()[1]) {
                    path.add(currentBlock);    
                }
                x--;
            } else {
                maze[x][y] = 0;
                x = path.peekLast()[0];
                y = path.peekLast()[1];
                path.pop();
            }
        }
        if (isWon(x, y, endX, endY)) {
            return true;
        }
        return false;
    }

    public static int lookRight(int[][] maze, int x, int y) {
        return maze[x][y + 1];

    }

    public static int lookDown(int[][] maze, int x, int y) {
        return maze[x + 1][y];
    }

    public static int lookLeft(int[][] maze, int x, int y) {
        return maze[x][y - 1];
    }

    public static int lookUp(int[][] maze, int x, int y) {
        return maze[x - 1][y];
    }

    public static boolean isWon(int x, int y, int endX, int endY) {
        return x == endX && y == endY;
    }

    public static boolean isLost(int[][] maze, int x, int y) {
        if (lookRight(maze, x, y) == 0 && lookDown(maze, x, y) == 0 && lookLeft(maze, x, y) == 0
                && lookUp(maze, x, y) == 0) {
            return true;
        }
        return false;
    }

    /**
     * Creates a large maze using iterative backtracking algorithm
     * This generates a proper maze with paths, dead ends, and a guaranteed solution
     */
    public static int[][] createLargeMaze(int size) {
        // Ensure size is odd for proper maze generation
        if (size % 2 == 0) size++;
        
        int[][] maze = new int[size][size];
        
        // Initialize with walls (0 = wall, 1 = path)
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                maze[i][j] = 0;
            }
        }
        
        // Generate maze using iterative backtracking (avoids stack overflow)
        // This creates a perfect maze - all cells are connected, so there's always a path
        generateMazeIterative(maze, size);
        
        // Ensure start and end are paths (they should already be, but guarantee it)
        maze[1][1] = 1;
        
        // Calculate end point (must be odd for maze generation)
        int endRow = (size - 2) % 2 == 0 ? size - 3 : size - 2;
        int endCol = (size - 2) % 2 == 0 ? size - 3 : size - 2;
        maze[endRow][endCol] = 1;
        
        // Ensure end point is connected to the maze network
        // Connect end point to nearest path if it's isolated
        ensureEndConnected(maze, size, endRow, endCol);
        
        // Add border walls
        for (int i = 0; i < size; i++) {
            maze[0][i] = 0;
            maze[size - 1][i] = 0;
            maze[i][0] = 0;
            maze[i][size - 1] = 0;
        }
        
        return maze;
    }
    
    /**
     * Ensures the end point is connected to the maze network
     */
    private static void ensureEndConnected(int[][] maze, int size, int endRow, int endCol) {
        // Check if end point is already connected (has at least one adjacent path)
        int[][] neighbors = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        boolean connected = false;
        
        for (int[] dir : neighbors) {
            int checkRow = endRow + dir[0];
            int checkCol = endCol + dir[1];
            if (checkRow > 0 && checkRow < size - 1 && 
                checkCol > 0 && checkCol < size - 1 && 
                maze[checkRow][checkCol] == 1) {
                connected = true;
                break;
            }
        }
        
        // If not connected, connect it to the nearest path
        if (!connected) {
            // Find nearest path cell and connect
            for (int[] dir : neighbors) {
                int checkRow = endRow + dir[0];
                int checkCol = endCol + dir[1];
                if (checkRow > 0 && checkRow < size - 1 && 
                    checkCol > 0 && checkCol < size - 1) {
                    // Check if this neighbor has a path nearby
                    for (int[] dir2 : neighbors) {
                        int nearRow = checkRow + dir2[0];
                        int nearCol = checkCol + dir2[1];
                        if (nearRow > 0 && nearRow < size - 1 && 
                            nearCol > 0 && nearCol < size - 1 && 
                            maze[nearRow][nearCol] == 1) {
                            maze[checkRow][checkCol] = 1; // Connect through neighbor
                            return;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Iterative backtracking maze generation algorithm (no stack overflow risk)
     */
    private static void generateMazeIterative(int[][] maze, int size) {
        Random random = new Random();
        Stack<int[]> stack = new Stack<>();
        boolean[][] visited = new boolean[size][size];
        
        // Start from (1, 1)
        int startRow = 1;
        int startCol = 1;
        
        stack.push(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;
        maze[startRow][startCol] = 1;
        
        // Directions: North, East, South, West (2 steps away)
        int[][] directions = {{-2, 0}, {0, 2}, {2, 0}, {0, -2}};
        
        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            int row = current[0];
            int col = current[1];
            
            // Get unvisited neighbors
            List<int[]> unvisitedNeighbors = new ArrayList<>();
            
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                
                // Check if the neighbor is valid and unvisited
                if (newRow > 0 && newRow < size - 1 && 
                    newCol > 0 && newCol < size - 1 && 
                    !visited[newRow][newCol]) {
                    unvisitedNeighbors.add(new int[]{newRow, newCol, dir[0], dir[1]});
                }
            }
            
            if (!unvisitedNeighbors.isEmpty()) {
                // Choose a random unvisited neighbor
                int[] chosen = unvisitedNeighbors.get(random.nextInt(unvisitedNeighbors.size()));
                int newRow = chosen[0];
                int newCol = chosen[1];
                int wallRow = row + chosen[2] / 2;
                int wallCol = col + chosen[3] / 2;
                
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
     * Verifies that start and end points are connected in the maze
     */
    private static boolean verifyMazeConnectivity(int[][] maze, int startX, int startY, int endX, int endY) {
        int rows = maze.length;
        int cols = maze[0].length;
        boolean[][] visited = new boolean[rows][cols];
        
        // Simple BFS to check connectivity
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        queue.offer(new int[]{startX, startY});
        visited[startX][startY] = true;
        
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int x = current[0];
            int y = current[1];
            
            if (x == endX && y == endY) {
                return true; // Found path to end
            }
            
            for (int[] dir : directions) {
                int newX = x + dir[0];
                int newY = y + dir[1];
                
                if (newX >= 0 && newX < rows && newY >= 0 && newY < cols &&
                    !visited[newX][newY] && maze[newX][newY] == 1) {
                    visited[newX][newY] = true;
                    queue.offer(new int[]{newX, newY});
                }
            }
        }
        
        return false; // No path found
    }
}
