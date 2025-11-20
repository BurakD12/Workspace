package GenericGraph;

public class MyGenericGraph<T> {
    private T[] vertexes;
    private int[][] edges;
    private int capacity;
    private int vertexCount;
    private int edgeCount;

    public MyGenericGraph(int capacity){
        this.capacity = capacity;
        this.vertexes = (T[]) new Object[capacity];
        this.edges = new int[capacity][capacity];
        this.edgeCount = 0;
        this.vertexCount = 0;
    }

    public void addVertex(T newVertex) {
        if (vertexCount == capacity){
            return;
        }
        for (int i = 0; i < capacity; i++) {
            if (vertexes[i] == newVertex){
                return;
            }
        }
        for (int i = 0; i < capacity; i++) {
            if (vertexes[i] == null){
                vertexes[i] = newVertex;
                vertexCount++;
                return;
            }
        }
        vertexes[vertexCount] = newVertex;
        vertexCount++;
    }
    
    public void removeVertex(T targetVertex) {
        if (vertexCount == 0){
            return;
        }
        int vertexPos = 0;
        boolean flag = false;
        for (int i = 0; i < capacity; i++) {
            if (vertexes[i] == targetVertex){
                flag = true;
                vertexPos = i;
            }
        }
        for (T vertex : vertexes) {
            removeEdge(vertex, targetVertex);
        }
        vertexes[vertexPos] = null;
        vertexCount--;
    }

    public void addEdge(T src, T dest) {
        boolean flag1 = false;
        boolean flag2 = false;
        int srcPos = 0;
        int destPos = 0;
        for (int i = 0; i < capacity; i++) {
            if (vertexes[i] == src){
                flag1 = true;
                srcPos = i;
            }
            if (vertexes[i] == dest){
                flag2 = true;
                destPos = i;
            }
        }
        if (flag1 == false || flag2 == false){
            return;
        }
        if (src == dest){
            return;
        }
        edges[srcPos][destPos] = 1;
        edges[destPos][srcPos] = 1;
    }

    public void removeEdge(T src, T dest) {
        boolean flag1 = false;
        boolean flag2 = false;
        int destPos = 0;
        int srcPos = 0;

        for (int i = 0; i < capacity; i++) {
            if (vertexes[i] == src){
                flag1 = true;
                srcPos = i;
            }
            if (vertexes[i] == dest){
                flag2 = true;
                destPos = i;
            }
        }
        edges[srcPos][destPos] = 0;
        edges[destPos][srcPos] = 0;
    }

    public void printGraph(){
        String format = "%15s";

        // Print vertexes
        for (int i = 0; i < capacity; i++) {
            if (vertexes[i] == null){
                continue;
            }
            System.out.printf(format, vertexes[i]);
        }

        System.out.println();
        // Print edges

        format = "%-15s";
        for (int i = 0; i < capacity; i++) {
            if (vertexes[i] == null){
                continue;
            }
            System.out.printf(format, vertexes[i]);
            for (int j = 0; j < capacity; j++) {
                if (vertexes[j] == null){
                    continue;
                }
                System.out.printf(format, edges[i][j]);
            }
            System.out.println();   
        }        
    }
}

/*
    Ayberk ==> [CAN, AYSE]
    Can ==> [AYBERK, AYSE]
    Ayse ==> [AYBERK, CAN]
*/

