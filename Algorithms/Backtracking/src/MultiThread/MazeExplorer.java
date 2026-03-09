package MultiThread;

import java.security.Key;
import java.security.KeyStore.Entry;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MazeExplorer extends Thread {
    private static final AtomicInteger threadIdCounter = new AtomicInteger(0);
    private final long threadId;
    private final MazeSharedState sharedState;
    private final int x, y;
    private final LinkedList<int[]> currentPath;
    private final int fromX, fromY; // Previous position to avoid going back

    public MazeExplorer(MazeSharedState sharedState, int x, int y, LinkedList<int[]> path, int fromX, int fromY) {
        this(sharedState, x, y, path, fromX, fromY, threadIdCounter.incrementAndGet());
    }
    
    // Constructor with explicit thread ID (for child threads to inherit parent's color)
    public MazeExplorer(MazeSharedState sharedState, int x, int y, LinkedList<int[]> path, int fromX, int fromY, long threadId) {
        this.threadId = threadId;
        this.sharedState = sharedState;
        this.x = x;
        this.y = y;
        this.currentPath = new LinkedList<>(path);
        this.fromX = fromX;
        this.fromY = fromY;
    }

    @Override
    public void run() {
        
        // Check if solution already found
        if (sharedState.isSolutionFound()) {
            sharedState.removeThreadPosition(threadId);
            return;
        }

        // Try to visit this cell
        if (!sharedState.tryVisit(x, y)) {
            sharedState.removeThreadPosition(threadId);
            return; // Already visited or solution found
        }

        // Update thread position and display
        sharedState.updateThreadPosition(threadId, x, y);
        sharedState.incrementTick();
        sharedState.displayMaze();
        
        // Small delay to see the updates (reduced for large mazes)
        try {
            Thread.sleep(50); // 50ms delay between ticks for faster execution
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sharedState.removeThreadPosition(threadId);
            return;
        }

        // Add current position to path
        currentPath.add(new int[]{x, y});

        // Check if we reached the end
        if (x == sharedState.getEndX() && y == sharedState.getEndY()) {
            sharedState.setSolution(currentPath);
            System.out.println("Thread " + Thread.currentThread().getName() + " (ID: " + threadId + ") found solution!");
            sharedState.removeThreadPosition(threadId);
            return;
        }

        // Find all available directions (excluding the direction we came from)
        List<Direction> availableDirections = getAvailableDirections(x, y, fromX, fromY);

        if (availableDirections.isEmpty()) {
            // Dead end - this thread terminates
            sharedState.removeThreadPosition(threadId);
            return;
        }

        // If only one direction, continue in that direction
        if (availableDirections.size() == 1) {
            Direction dir = availableDirections.get(0);
            int newX = x + dir.deltaX;
            int newY = y + dir.deltaY;
            
            // Remove current position before moving (child thread will show new position)
            sharedState.removeThreadPosition(threadId);
            
            // Create new explorer for the next step with SAME thread ID to maintain color
            MazeExplorer nextExplorer = new MazeExplorer(sharedState, newX, newY, currentPath, x, y, threadId);
            nextExplorer.start();
            try {
                nextExplorer.join(); // Wait for this path to complete
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sharedState.removeThreadPosition(threadId);
                return;
            }
        } else {
            // Multiple directions available - spawn a thread for each
            List<MazeExplorer> explorers = new ArrayList<>();
            
            // Remove current position before spawning children (children will show their positions)
            sharedState.removeThreadPosition(threadId);
            
            for (Direction dir : availableDirections) {
                int newX = x + dir.deltaX;
                int newY = y + dir.deltaY;
                
                // Create a new explorer thread for this direction
                MazeExplorer explorer = new MazeExplorer(sharedState, newX, newY, currentPath, x, y);
                explorers.add(explorer);
                explorer.start();
            }
            
            // Wait for all threads to complete
            for (MazeExplorer explorer : explorers) {
                try {
                    explorer.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Get all available directions from current position
     */
    private List<Direction> getAvailableDirections(int x, int y, int fromX, int fromY) {
        List<Direction> directions = new ArrayList<>();
        int rows = sharedState.getRows();
        int cols = sharedState.getCols();
        
        // Check each direction (EAST, SOUTH, WEST, NORTH)
        // EAST (right)
        if (y + 1 < cols && 
            sharedState.getMazeValue(x, y + 1) == 1 && 
            !(x == fromX && y + 1 == fromY) &&
            !sharedState.isVisited(x, y + 1)) {
            directions.add(new Direction(0, 1));
        }
        
        // SOUTH (down)
        if (x + 1 < rows && 
            sharedState.getMazeValue(x + 1, y) == 1 && 
            !(x + 1 == fromX && y == fromY) &&
            !sharedState.isVisited(x + 1, y)) {
            directions.add(new Direction(1, 0));
        }
        
        // WEST (left)
        if (y - 1 >= 0 && 
            sharedState.getMazeValue(x, y - 1) == 1 && 
            !(x == fromX && y - 1 == fromY) &&
            !sharedState.isVisited(x, y - 1)) {
            directions.add(new Direction(0, -1));
        }
        
        // NORTH (up)
        if (x - 1 >= 0 && 
            sharedState.getMazeValue(x - 1, y) == 1 && 
            !(x - 1 == fromX && y == fromY) &&
            !sharedState.isVisited(x - 1, y)) {
            directions.add(new Direction(-1, 0));
        }
        
        return directions;
    }

    /**
     * Helper class to represent a direction
     */
    private static class Direction {
        final int deltaX, deltaY;

        Direction(int deltaX, int deltaY) {
            this.deltaX = deltaX;
            this.deltaY = deltaY;
        }
    }
}

