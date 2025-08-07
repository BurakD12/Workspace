package PriorityQueue;

import javax.management.RuntimeErrorException;

import ArrayBasedQueue.QueueOverflowException;
import ArrayBasedQueue.QueueUnderflowException;

public class PriorityQueue<T extends Comparable<T>> {
    private int capacity;
    private int front;
    private int rear;
    private T[] priorityQueue;

    public PriorityQueue(int capacity) throws IllegalArgumentException{
        if (capacity < 1){
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        this.front = -1;
        this.rear = -1;
        this.priorityQueue = (T[]) new Comparable[capacity];
    }
    
    //      { [] [3.2] [3.1] [3] [2.9] }
    //             F               R
    public void enqueue(T data) throws QueueOverflowException, RuntimeException{
        if (data == null){
            throw new RuntimeException();
        }
        if (rear == capacity - 1){
            throw new QueueOverflowException("The queue is full cannot enqueue anymore.");
        }
        if (rear == -1){
            rear++;
            front++;
            priorityQueue[front] = data;
            return;
        }
        for (int i = front; i <= rear; i++) {
            if (data.compareTo(priorityQueue[i]) > 0){
                rear++;
                for (int j = rear; j > i; j--) {
                    priorityQueue[j] = priorityQueue[j - 1];
                }
                priorityQueue[i] = data;
                return;
            }
        }
        rear++;
        priorityQueue[rear] = data;

    }

    public T dequeue() throws QueueUnderflowException{
        if (rear == -1){
            throw new QueueUnderflowException("The queue is empty nothing to dequeue.");
        }
        if (front == rear){
            T tempValue = priorityQueue[front];
            front = -1;
            rear = -1;
            return tempValue;
        }
        T tempValue = priorityQueue[front];
        front++;
        return tempValue;
    }

    public T peek() throws QueueUnderflowException{
        if (rear == -1){
            throw new QueueUnderflowException("The queue is empty, nothing to peek at.");
        }
        return priorityQueue[front];
    }

    public int size(){
        if (rear == -1){
            return 0;
        }
        return rear - front + 1;
    }

    public boolean isFull(){
        return rear == capacity - 1;
    }

    public boolean isEmpty(){
        return rear == -1;
    }
}
