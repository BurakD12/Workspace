package ThreadProgramming.ProducerConsumer.GoodImplementation;

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
            synchronized(sharedBuffer) {
                while (sharedBuffer.size() == 0) {
                    try {
                        sharedBuffer.wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                value = sharedBuffer.poll();
                System.out.println(name + ": Consumed " + value);
                sharedBuffer.notifyAll();
            }
        }
    }

    
}
