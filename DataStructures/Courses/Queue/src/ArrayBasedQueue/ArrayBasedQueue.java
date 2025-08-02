package ArrayBasedQueue;

public class ArrayBasedQueue<T> {
    private int capacity;
    private int front;
    private int rear;
    private T[] queue;

    public ArrayBasedQueue(int capacity) throws IllegalArgumentException{
        if (capacity < 1){
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        this.front = -1;
        this.rear = -1;
        this.queue = (T[]) new Object[capacity];
    }

    public void enqueue(T value) throws QueueOverflowException{
        if (rear == -1){
            front++;
            rear++;
            queue[rear] = value;
            return;
        }
        if (rear == capacity - 1){
            throw new QueueOverflowException("The queue is full cannot accept new value.");
        }
        rear++;
        queue[rear] = value;
    }

    public T dequeue() throws QueueUnderflowException{
        if (rear == -1){
            throw new QueueUnderflowException("The queue is empty nothing to dequeue.");
        }
        if (front == rear){
            T tempValue = queue[front];
            front = -1;
            rear = -1;
            return tempValue;
        }
        T tempValue = queue[front];
        front++;
        return tempValue;
    }
    
    public T peek() throws QueueUnderflowException{
        if (rear == -1){
            throw new QueueUnderflowException("The queue is empty, nothing to peek at.");
        }
        return queue[front];
    }

    public int size(){
        if (rear == -1){
            return 0;
        }
        return rear - front + 1;
    }

    public boolean isEmpty(){
        return rear == -1;
    }

    public boolean isFull(){
        return rear == capacity - 1;
    }
}
