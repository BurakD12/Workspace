package DFS;

public class Main {
    public static void main(String[] args) {
        AdjacencyList adjacencyList = new AdjacencyList(5);
        adjacencyList.addEdge(0, 1);
        adjacencyList.addEdge(0, 2);
        adjacencyList.addEdge(0, 3);
        adjacencyList.addEdge(1, 2);
        adjacencyList.addEdge(2, 4);
        adjacencyList.DFS(0);
    }
}
