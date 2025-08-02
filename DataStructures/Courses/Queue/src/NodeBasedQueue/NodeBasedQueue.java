package NodeBasedQueue;

import ArrayBasedQueue.QueueUnderflowException;

public class NodeBasedQueue<T> {
    private Node<T> front;
    private Node<T> rear;

    public NodeBasedQueue(){
        this.front = null;
        this.rear = null;
    }
    
    public void enqueue(T data){
        if (rear == null){
            rear = new Node<T>(data);
            front = rear;
            return;
        }
        if (front == rear){
            rear = new Node<T>(data);
            front.setNext(rear);
            return;
        }
        Node<T> newNode = new Node<T>(data);
        rear.setNext(newNode);
        rear = newNode;
    }

    public T dequeue() throws QueueUnderflowException{
        if (rear == null){
            throw new QueueUnderflowException("Queue is empty, nothing to dequeue.");
        }
        if (front == rear){
            T tempData = front.getData();
            front = null;
            rear = null;
            return tempData;
        }
        T tempData = front.getData();
        front = front.getNext();
        return tempData;
    }

    public T peek() throws QueueUnderflowException{
        if (rear == null){
            throw new QueueUnderflowException("Queue is empty, nothing to peek at.");
        }
        return front.getData();
    }

    public int size(){
        int count = 0;
        if (rear == null){
            return count;
        }
        Node<T> currentNode = front;
        while (currentNode != null) {
            count++;
            currentNode = currentNode.getNext();
        }
        return count;
    }

    public boolean isEmpty(){
        return rear == null;
    }
}
