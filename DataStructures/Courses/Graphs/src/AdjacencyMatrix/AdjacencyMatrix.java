package AdjacencyMatrix;

public class AdjacencyMatrix {
    private int capacity;
    private int[][] adjacencyMatrix;

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int[][] getAdjacencyMatrix() {
        return adjacencyMatrix;
    }

    public void setAdjacencyMatrix(int[][] adjacencyMatrix) {
        this.adjacencyMatrix = adjacencyMatrix;
    }

    public AdjacencyMatrix(int capacity){
        this.capacity = capacity;
        this.adjacencyMatrix = new int[capacity][capacity];
    }

    public void addEdge(int vertex1, int vertex2) throws IndexOutOfBoundsException{
        if (vertex1 < 0 || vertex1 >= capacity || vertex2 < 0 || vertex2 >= capacity) {
            throw new IndexOutOfBoundsException();
        }
        if (vertex1 == vertex2){
            return;
        }

        if (adjacencyMatrix[vertex1][vertex2] == 1){
            return;
        }
        else {
            adjacencyMatrix[vertex1][vertex2] = 1;
            adjacencyMatrix[vertex2][vertex1] = 1;
            return;
        }
    }

    public void removeEdge(int vertex1, int vertex2) throws IndexOutOfBoundsException{
        if (vertex1 < 0 || vertex1 >= capacity || vertex2 < 0 || vertex2 >= capacity) {
            throw new IndexOutOfBoundsException();
        }

        if (adjacencyMatrix[vertex1][vertex2] == 0){
            return;
        }
        else {
            adjacencyMatrix[vertex1][vertex2] = 0;
            adjacencyMatrix[vertex2][vertex1] = 0;
            return;
        }
    }

    public void printMatrix() {
        /*

        Sample Output:
                 0 1 2 3 
               0 1 0 1 0
               1 0 1 0 1
               2 0 1 0 1
               3 0 0 1 0
        */

        System.out.print("  ");
        for (int i = 0; i < capacity; i++) {
            System.out.print(i + " ");
        }

        System.out.println();
        for (int i = 0; i < capacity; i++) {
            System.out.print(i + " ");
            for (int j = 0; j < capacity; j++) {
                System.out.print(adjacencyMatrix[i][j] + " ");
            }
            System.out.println();
        }

    }
}

// https://www.programiz.com/dsa/graph-adjacency-matrix