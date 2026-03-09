package ThreadProgramming.ProducerConsumer.BadImplementation;

import java.util.LinkedList;

public class Producer implements Runnable {
    LinkedList<Integer> sharedBuffer;
    int capacity;
    String name;

    public Producer(String name, LinkedList<Integer> sharedBuffer, int capacity) {
        this.name = name;
        this.sharedBuffer = sharedBuffer;
        this.capacity = capacity;
    }

    @Override
    public void run() {
        int value = 0;
        while (true) {
            while (sharedBuffer.size() >= capacity) {
                // Busy-waiting
            }
            sharedBuffer.add(value);
            System.out.println(this.name + ": Produced " + value);
            value++;
        }

        
    }

    
}
