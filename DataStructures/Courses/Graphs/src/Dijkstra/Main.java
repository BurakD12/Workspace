package Dijkstra;

public class Main {
    public static void main(String[] args) {
        MapBasedGraph map = new MapBasedGraph<>();
        map.addVertex('A');
        map.addVertex('B');
        map.addVertex('C');
        map.addVertex('D');
        map.addVertex('E');
        map.addEdge('A', 'B', 6);
        map.addEdge('A', 'D', 1);
        map.addEdge('B', 'D', 2);
        map.addEdge('E', 'D', 1);
        map.addEdge('E', 'B', 2);
        map.addEdge('E', 'C', 5);
        map.addEdge('C', 'B', 5);

        map.printTable(map.dijkstra('A'));
        System.out.println(map.getPath(map.dijkstra('A'), 'A', 'C'));
    }
}
