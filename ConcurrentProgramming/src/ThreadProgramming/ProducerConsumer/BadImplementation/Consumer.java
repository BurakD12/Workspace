package ThreadProgramming.ProducerConsumer.BadImplementation;

import java.util.LinkedList;

public class Consumer implements Runnable{
    LinkedList<Integer> sharedBuffer;
    int capacity;
    String name;

    public Consumer(String name, LinkedList<Integer> sharedBuffer, int capacity) {
        this.sharedBuffer = sharedBuffer;
        this.capacity = capacity;
        this.name = name;
    }

    @Override
    public void run() {
        int value;
        while (true) {
            while (sharedBuffer.size() == 0) {
                // waiting for the buffer to be filled
            }
            value = sharedBuffer.poll();
            System.out.println(name + ": Consumed " + value);
        }
    }

    
}
