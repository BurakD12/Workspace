package GenericGraph;

public class Main {
    public static void main(String[] args) {
        MyGenericGraph graph = new MyGenericGraph<>(3);
        graph.addVertex("AYBERK");
        graph.addVertex("AYSE");
        graph.addVertex("CAN");
        graph.addEdge("AYBERK", "AYSE");
        graph.addEdge("CAN", "AYSE");
        graph.addEdge("CAN", "AYBERK");
        graph.removeVertex("AYBERK");
        graph.printGraph();
    }
}
