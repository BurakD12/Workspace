package DFS;

import java.util.ArrayList;
import java.util.LinkedList;

public class AdjacencyList {
    private int capacity;
    private ArrayList<ArrayList<Integer>> adjacencyList;

    public AdjacencyList(int capacity) {
        this.capacity = capacity;
        this.adjacencyList = new ArrayList<>();

        for (int i = 0; i < capacity; i++) {
            adjacencyList.add(new ArrayList<>());
        }
    }

    public void addEdge(int src, int dest) throws IndexOutOfBoundsException {
        // Check if the source and destination indices are within bounds
        if (src < 0 || src >= capacity || dest < 0 || dest >= capacity) {
            throw new IndexOutOfBoundsException();
        }
        // Prevent self-loop and duplicate edges
        if (src == dest) {
            return;
        }
        // Check if the edge already exists
        if (adjacencyList.get(src).contains(dest)) {
            return;
        }
        adjacencyList.get(src).add(dest);
        adjacencyList.get(dest).add(src);
    }

    public void removeEdge(int src, int dest) throws IndexOutOfBoundsException {
        if (src < 0 || src >= capacity || dest < 0 || dest >= capacity) {
            throw new IndexOutOfBoundsException();
        }
        if (!adjacencyList.get(src).contains(dest)) {
            return;
        }
        adjacencyList.get(src).remove(dest);
        adjacencyList.get(dest).remove(src);
    }

    public void printList() {
        for (int i = 0; i < capacity; i++) {
            System.out.print("\n" + i + ": ");
            for (int j = 0; j < capacity; j++) {
                if (adjacencyList.get(i).contains(j)) {
                    System.out.print(j + "  ");
                }
            }
        }
    }

    public void DFS(int startVertex) {
        ArrayList<Integer> stack = new ArrayList<>();
        ArrayList<Integer> visited = new ArrayList<>();
        stack.add(startVertex);
        
        while (!stack.isEmpty()) {
            int placeHolder = stack.removeLast();
            visited.add(placeHolder);
            
            for (Integer adjacents : adjacencyList.get(placeHolder)) {
                if (!visited.contains(adjacents) && !stack.contains(adjacents)){
                    stack.add(adjacents);
                }
            }
        }
        
        System.out.println("DFS Traversal starting from vertex " + startVertex + ": " + visited);
    }
}
