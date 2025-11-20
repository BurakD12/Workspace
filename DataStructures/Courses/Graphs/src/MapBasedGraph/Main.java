package MapBasedGraph;

public class Main {
    public static void main(String[] args) {
        MapBasedGraph map = new MapBasedGraph<>();
        map.addVertex(0);
        map.addVertex(1);
        map.addVertex(2);
        map.addVertex(3);
        map.addVertex(4);
        map.addEdge(0, 1, 3);
        map.addEdge(0, 3, 7);
        map.addEdge(0, 4, 8);
        map.addEdge(1, 3, 4);
        map.addEdge(1, 2, 1);
        map.addEdge(2, 3, 2);
        map.addEdge(3, 4, 3);

        map.removeEdge(0, 1);
        map.removeVertex(1);
        map.printGraph();
    }
}
