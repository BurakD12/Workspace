package ThreadProgramming.ProducerConsumer.GoodImplementation;

import java.util.LinkedList;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        LinkedList<Integer> sharedBuffer = new LinkedList<Integer>();
        int capacity = 5;
        Thread producer1 = new Thread(new Producer("producer1", sharedBuffer, capacity));
        Thread consumer1 = new Thread(new Consumer("consumer1", sharedBuffer, capacity));
        
        producer1.start();
        consumer1.start();
        producer1.join();
        consumer1.join();
    }
}
