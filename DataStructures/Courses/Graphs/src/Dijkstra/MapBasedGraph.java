package Dijkstra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;


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

    // Dijkstra
    public Map<T, Object[]> dijkstra(T start) {
        if (!adjacencyMap.containsKey(start)) {
            throw new IllegalArgumentException("Start vertex not found in the graph");
        }

        Map<T, Object[]> table = new HashMap<>();
        ArrayList<T> unvisited = new ArrayList<>();
        for (T vertex : adjacencyMap.keySet()) {
            table.put(vertex, new Object[] { Integer.MAX_VALUE, null });
            unvisited.add(vertex);
        }

        table.put(start, new Object[] { 0, null });

        while (!unvisited.isEmpty()) {
            T currentVertex = findNextVertex(unvisited, table);
            unvisited.remove(currentVertex);
            Map<T, Integer> adjacents = adjacencyMap.get(currentVertex);
            
            for (Entry<T, Integer> entry : adjacents.entrySet()) {
                T adjacentVertex = entry.getKey();
                Integer weight = entry.getValue();

                if (!unvisited.contains(adjacentVertex)){
                    continue;
                }

                int currentDistance = (int) table.get(currentVertex)[0];
                int newDistance = currentDistance + weight;
                int knownDistance = (int) table.get(adjacentVertex)[0];

                if (newDistance < knownDistance) {
                    table.put(adjacentVertex, new Object[] { newDistance, currentVertex });
                }
            }
        }
        return table;
    }

    public T findNextVertex(ArrayList<T> unvisited, Map<T, Object[]> table) {
        int minDistance = Integer.MAX_VALUE;
        T nextVertex = null;
        
        for (T vertex : table.keySet()) {
            if (!unvisited.contains(vertex)){
                continue;
            }

            int currentDistance = (int) table.get(vertex)[0];
            if (currentDistance < minDistance){
                nextVertex = vertex;
                minDistance = currentDistance;
            }
        }
        return nextVertex;
    }

    public String getPath(Map<T, Object[]> table, T startVertex, T endVertex) {
        Stack vertexStack = new Stack<>();
        Stack weightStack = new Stack<>();
        StringBuilder sb = new StringBuilder();
        T nextVertex = endVertex;

        while (nextVertex != startVertex) {
            vertexStack.push(nextVertex);
            T placeHolderVertex = (T) table.get(nextVertex)[1];
            weightStack.push(adjacencyMap.get(placeHolderVertex).get(nextVertex));
            nextVertex = (T) table.get(nextVertex)[1];
        }
        vertexStack.push(startVertex);

        while (!vertexStack.isEmpty()) {
            sb.append(vertexStack.pop());
            if (!weightStack.isEmpty()){
                sb.append(" -(").append(weightStack.pop()).append(")-> ");
            }
        }
        sb.append("\n").append("Total weight: ").append(table.get(endVertex)[0]);
        return sb.toString();
    }

    public void printTable(Map<T, Object[]> table) {
        StringBuilder sb = new StringBuilder();
        for (T key : table.keySet()) {
            if (table.get(key)[1] == null) {
                sb.append(key).append("  ").append(table.get(key)[0]).append("  ").append("-").append("\n");
            } else {
                sb.append(key).append("  ").append(table.get(key)[0]).append("  ").append(table.get(key)[1]).append("\n");
            }
        }
        System.out.println(sb.toString());
    }

}