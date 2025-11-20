package BFS;

public class Main {
    public static void main(String[] args) {
        AdjacencyList adjacencyList = new AdjacencyList(10);
        adjacencyList.addEdge(0, 1);
        adjacencyList.addEdge(0, 2);
        adjacencyList.addEdge(1, 3);
        adjacencyList.addEdge(1, 4);
        adjacencyList.addEdge(2, 5);
        adjacencyList.addEdge(2, 6);
        adjacencyList.addEdge(3, 7);
        adjacencyList.addEdge(4, 8);
        adjacencyList.addEdge(5, 9);
        adjacencyList.addEdge(6, 0);
        adjacencyList.addEdge(7, 1);
        adjacencyList.addEdge(8, 2);
        adjacencyList.addEdge(9, 3);
        adjacencyList.BFS(0);
    }
}
