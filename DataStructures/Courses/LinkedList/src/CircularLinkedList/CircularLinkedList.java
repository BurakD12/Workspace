package CircularLinkedList;

import java.util.NoSuchElementException;

public class CircularLinkedList<T> {
    private Node<T> head;
    private Node<T> tail;

    public CircularLinkedList() {
        this.head = null;
        this.tail = null;
    }

    public void append(T value) {
        if (head == null){
            head = new Node<>(value);
            tail = head;
            tail.setNext(head); // Point tail to head to make it circular
        } else {
            tail.setNext(new Node<>(value));
            tail = tail.getNext();
            tail.setNext(head);
        }
    }

    public void prepend(T value) {
        Node<T> newNode = new Node<>(value);
        if (head == null) {
            head = newNode;
            tail = head;
            tail.setNext(head); // Point tail to head to make it circular
        } else {
            newNode.setNext(head);
            head = newNode;
            tail.setNext(head); // Update tail to point to the new head
        }
    }

    public void deleteByValue(T value) {
        if (head == null) {
            throw new NoSuchElementException("List is empty, nothing to delete.");
        }
        
        Node<T> currentNode = head;
        Node<T> previousNode = tail;

        do {
            if (currentNode.getData().equals(value)) {
                if (currentNode == head) {
                    if (head == tail) {
                        head = null;
                        tail = null; // List becomes empty
                    } else {
                        head = head.getNext();
                        tail.setNext(head); // Update tail to point to new head
                    }
                } else if (currentNode == tail) {
                    tail = previousNode; 
                    tail.setNext(head); // Update tail to point to head
                } else {
                    previousNode.setNext(currentNode.getNext()); // Bypass the current node
                }
                return; // Value found and deleted
            } 
            previousNode = currentNode;
            currentNode = currentNode.getNext();
        } while (currentNode != head);
        throw new NoSuchElementException();
    }

    public void deleteByIndex(int index) {
        if (index >= size() || index < 0) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }

        if (index == 0){
            if (head == tail){
            head = null;
            tail = null;
            }
            else {
            head = head.getNext();
            tail.setNext(head);
            }
        }

        Node<T> currentNode = head;
        for (int i = 0; i < index - 1; i++) {
            currentNode = currentNode.getNext();
        }
        if (currentNode.getNext() == tail){
            currentNode.setNext(head);
            tail = currentNode;
        }
        else {
            currentNode.setNext(currentNode.getNext().getNext());
        }
    }

    public void deleteFirst() {
        if (head == null){
            throw new NoSuchElementException();
        }
        if (head == tail){
            head = null;
            tail = null;
        }
        else {
            head = head.getNext();
            tail.setNext(head);
        }
    }

    public void deleteLast() {
        if (head == null){
            throw new NoSuchElementException();
        }
        if (head == tail){
            head = null;
            tail = null;
        }
        else {
            Node<T> currentNode = head;
            for (int i = 0; i < size() - 2; i++){
                currentNode = currentNode.getNext();
            }
            currentNode.setNext(head);
            tail = currentNode;
        }
    }

    public int size() {
        int count = 0;
        if (head == null){
            return count;
        }
        if (head == tail){
            count++;
            return count;
        }
        count++;
        Node<T> currentNode = head;
        while (currentNode.getNext() != head) {
            count++;
            currentNode = currentNode.getNext();
        }
        return count;
    }

    public boolean isEmpty() {
        return head == null;
    }

    public boolean contains(T value) {
        if (head == null){
            return false;
        }
        Node<T> currentNode = head;
        do {
            if (currentNode.getData().equals(value)){
                return true;
            }
            currentNode = currentNode.getNext();
        } while (currentNode != head);
        return false;
    }

    public void clear() {
        head = null;
        tail = null;
    }

    @Override
    public String toString() {
        Node<T> currentNode = head;
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        do {
            sb.append(currentNode.getData()).append(" -> ");
            currentNode = currentNode.getNext();
        } while (currentNode != head);
        sb.append(currentNode.getData()).append(" (head)").append("]");
        return sb.toString();
    }

    public int indexOf(T value) {
        Node<T> currentNode = head;
        int count = 0;
        do {
            if (currentNode.getData().equals(value)){
                return count;
            }
            count++;
            currentNode = currentNode.getNext();
        } while (currentNode != head);
        return -1;
    }

    public T get(int index) {
        if (index > size() - 1){
            throw new IndexOutOfBoundsException("Index is out of bounds");
        }
        if (index < 0){
            throw new IndexOutOfBoundsException("Index is out of bounds");
        }
        Node<T> currentNode = head;
        for (int i = 0; i < index; i++) {
            currentNode = currentNode.getNext();
        }
        return currentNode.getData();
    }
}
