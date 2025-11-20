package AdjacencyMatrix;

public class Main {
    public static void main(String[] args) {
        AdjacencyMatrix adjacencyMatrix = new AdjacencyMatrix(4);
        adjacencyMatrix.addEdge(0, 1);
        adjacencyMatrix.addEdge(0, 2);
        adjacencyMatrix.addEdge(0, 3);
        adjacencyMatrix.addEdge(1, 2);
        adjacencyMatrix.removeEdge(1, 2);
        adjacencyMatrix.printMatrix();
    }
}
