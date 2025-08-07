package CircularQueue;

public class CircularQueue<T> {
    private int capacity;
    private int front;
    private int rear;
    private T[] queue;

    public CircularQueue(int capacity) throws IllegalArgumentException{
        if (capacity < 1){
            throw new IllegalArgumentException("Capacity cannot be lower than 1.");
        }
        this.capacity = capacity;
        this.front = -1;
        this.rear = -1;
        this.queue = (T[]) new Object[capacity];
    }

    public void enqueue(T data) throws QueueOverflowException{
        if (isFull()){
            throw new QueueOverflowException("The queue is full, cannot enqueue anymore.");
        }
        
        if (isEmpty()){
            front = 0;
            rear = 0;
            queue[rear] = data;
            return;
        }
        
        rear++;
        rear %= capacity;
        queue[rear] = data;
    }

    public T dequeue() throws QueueUnderflowException{
        if (isEmpty()){
            throw new QueueUnderflowException("The queue is empty, cannot dequeue anymore.");
        }
        if (rear == front){
            T tempData = queue[front];
            front = -1;
            rear = -1;
            return tempData;
        }
        T tempData = queue[front];
        front++;
        front %= capacity;
        return tempData;
    }

    public T peek() throws QueueUnderflowException{
        if (isEmpty()){
            throw new QueueUnderflowException("The queue is empty, nothing to peek at.");
        }
        return queue[front];
    }

    public int size(){
        if (rear == -1){
            return 0;
        }
        if (rear >= front){
            return rear - front + 1;
        }
        int count = 0;
        for (int i = front; i < capacity; i++) {
            count++;
        }
        for (int i = 0; i <= rear; i++) {
            count++;
        }
        return count;
    }
    
    public boolean isEmpty(){
        return rear == -1;
    }

    public boolean isFull(){
        return (rear + 1) % capacity == front;

    }
}
