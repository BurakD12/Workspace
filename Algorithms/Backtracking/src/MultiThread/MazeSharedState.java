package MultiThread;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MazeSharedState {
    private final int[][] maze;
    private final boolean[][] visited;
    private final int endX, endY;
    private final int startX, startY;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicBoolean solutionFound = new AtomicBoolean(false);
    private LinkedList<int[]> solutionPath = null;
    private final Object solutionLock = new Object();
    
    // Track thread positions: Map<ThreadId, {x, y}>
    private final Map<Long, int[]> threadPositions = new ConcurrentHashMap<>();
    private final AtomicInteger tickCounter = new AtomicInteger(0);
    private final Object displayLock = new Object();

    public MazeSharedState(int[][] maze, int startX, int startY, int endX, int endY) {
        this.maze = maze;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.visited = new boolean[maze.length][maze[0].length];
    }

    /**
     * Try to mark a cell as visited. Returns true if successful (cell was not visited before)
     */
    public boolean tryVisit(int x, int y) {
        lock.writeLock().lock();
        try {
            if (visited[x][y] || solutionFound.get()) {
                return false;
            }
            visited[x][y] = true;
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Check if a cell is visited (read-only)
     */
    public boolean isVisited(int x, int y) {
        lock.readLock().lock();
        try {
            return visited[x][y];
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Check if solution is found
     */
    public boolean isSolutionFound() {
        return solutionFound.get();
    }

    /**
     * Set the solution path (only first thread to find solution sets it)
     */
    public boolean setSolution(LinkedList<int[]> path) {
        synchronized (solutionLock) {
            if (!solutionFound.get()) {
                solutionFound.set(true);
                this.solutionPath = new LinkedList<>(path);
                return true;
            }
            return false;
        }
    }

    /**
     * Get the solution path
     */
    public LinkedList<int[]> getSolution() {
        synchronized (solutionLock) {
            return solutionPath;
        }
    }

    /**
     * Get maze value at position
     */
    public int getMazeValue(int x, int y) {
        return maze[x][y];
    }

    public int getEndX() {
        return endX;
    }

    public int getEndY() {
        return endY;
    }

    public int getRows() {
        return maze.length;
    }

    public int getCols() {
        return maze[0].length;
    }

    /**
     * Update thread position for display
     */
    public void updateThreadPosition(long threadId, int x, int y) {
        threadPositions.put(threadId, new int[]{x, y});
    }

    /**
     * Remove thread position when thread finishes
     */
    public void removeThreadPosition(long threadId) {
        threadPositions.remove(threadId);
    }

    /**
     * Get all thread positions (snapshot)
     */
    public Map<Long, int[]> getThreadPositions() {
        return new ConcurrentHashMap<>(threadPositions);
    }

    /**
     * Increment tick counter and return current tick
     */
    public int incrementTick() {
        return tickCounter.incrementAndGet();
    }

    /**
     * Get current tick
     */
    public int getTick() {
        return tickCounter.get();
    }

    /**
     * Display the maze with colored thread positions
     */
    public void displayMaze() {
        synchronized (displayLock) {
            int tick = getTick();
            int rows = getRows();
            
            // For large mazes, only display every 5th tick to reduce output
            if (rows > 20 && tick % 5 != 0 && tick > 0) {
                return;
            }
            
            System.out.println("\n=== TICK " + tick + " ===");
            System.out.println("Active Threads: " + threadPositions.size());
            
            Map<Long, int[]> positions = getThreadPositions();
            
            // ANSI color codes
            String[] colors = {
                "\033[31m", // Red
                "\033[32m", // Green
                "\033[33m", // Yellow
                "\033[34m", // Blue
                "\033[35m", // Magenta
                "\033[36m", // Cyan
                "\033[91m", // Bright Red
                "\033[92m", // Bright Green
                "\033[93m", // Bright Yellow
                "\033[94m", // Bright Blue
                "\033[95m", // Bright Magenta
                "\033[96m", // Bright Cyan
            };
            String reset = "\033[0m";
            
            // Create a map of positions to thread IDs (for coloring)
            Map<String, Long> positionToThread = new ConcurrentHashMap<>();
            for (Map.Entry<Long, int[]> entry : positions.entrySet()) {
                int[] pos = entry.getValue();
                positionToThread.put(pos[0] + "," + pos[1], entry.getKey());
            }
            
            // Display maze
            for (int i = 0; i < maze.length; i++) {
                for (int j = 0; j < maze[i].length; j++) {
                    String key = i + "," + j;
                    
                    if (i == startX && j == startY) {
                        System.out.print("\033[42mS\033[0m "); // Green background for Start
                    } else if (i == endX && j == endY) {
                        System.out.print("\033[41mE\033[0m "); // Red background for End
                    } else if (positionToThread.containsKey(key)) {
                        long threadId = positionToThread.get(key);
                        int colorIndex = (int)(threadId % colors.length);
                        System.out.print(colors[colorIndex] + "T" + reset + " ");
                    } else if (maze[i][j] == 1) {
                        System.out.print("· "); // Path
                    } else {
                        System.out.print("█ "); // Wall
                    }
                }
                System.out.println();
            }
            System.out.println();
        }
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }
}

