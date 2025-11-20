package MapBasedGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MapBasedGraph<T> {
    private HashMap<T, HashMap<T, Integer>> adjacencyMap;

    public MapBasedGraph() {
        this.adjacencyMap = new HashMap<>();
    }

    public void addEdge(T src, T dest, int weight) {
        adjacencyMap.get(src).put(dest, weight);
        adjacencyMap.get(dest).put(src, weight);
    }

    public void removeEdge(T src, T dest) {
        adjacencyMap.get(src).remove(dest);
        adjacencyMap.get(dest).remove(src);
    }

    public void addVertex(T vertex) {
        adjacencyMap.put(vertex, new HashMap<>());
    }

    public void removeVertex(T vertex) {
        for (HashMap<T, Integer> map : adjacencyMap.values()) {
            if (map.containsKey(vertex)) {
                map.remove(vertex);
            }
        }
        adjacencyMap.remove(vertex);
    }

    public void printGraph() {
        for (T vertex : adjacencyMap.keySet()) {
            System.out.print(vertex + " --> ");
            Map<T, Integer> edges = adjacencyMap.get(vertex);
            for (Entry<T, Integer> entry : edges.entrySet()) {
                T adjacentVertex = entry.getKey();
                Integer weight = entry.getValue();

                System.out.print(adjacentVertex + "(" + weight + ")");

                if (!entry.equals(edges.entrySet().toArray()[edges.size() - 1])) {
                    System.out.print(", ");
                }
            }

            System.out.println();
        }
    }
}