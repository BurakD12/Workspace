package SingleThread;
import java.util.LinkedList;
import java.util.Stack;

public class App {
    public static void main(String[] args) throws Exception {
        int[][] maze = {
                { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
                { 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0 },
                { 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0 },
                { 0, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0 },
                { 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0 },
                { 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0 },
                { 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0 },
                { 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0 },
                { 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0 },
                { 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0 },
                { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }
        };

        LinkedList<int[]> path = new LinkedList<>();
        int startX = 1, startY = 1;
        int endX = 9, endY = 9;

        if (solveMaze(maze, startX, startY, endX, endY, path)) {
            System.out.println("Maze solved! Path taken:");
            for (int[] step : path) {
                System.out.println("[" + step[0] + ", " + step[1] + "]");
            }
            System.out.println("[" + endX + ", " + endY + "]");
        } else {
            System.out.println("No solution found for the maze.");
        }
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
        return maze[x - 0][y];
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
}
