package SingularLinkedList;

import java.util.NoSuchElementException;

public class SingularLinkedList<T> {
    private Node<T> head;

    public SingularLinkedList() {
        this.head = null;
    }

    public void append(T value) {
        Node<T> newNode = new Node<>(value);

        if (head == null) {
            head = newNode;
            return;
        }

        Node<T> currentNode = head;
        while (currentNode.getNext() != null) {
            currentNode = currentNode.getNext();
        }
        currentNode.setNext(newNode);
    }

    public void prepend(T value) {
        Node<T> newNode = new Node<T>(value);
        newNode.setNext(head);
        head = newNode;
    }

    public void deleteByValue(T value) {
        if (head == null){
            return;
        }
        if (value == head.getData()) {
            head = head.getNext();
            return;
        }
        Node<T> currentNode = head;
        while (currentNode.getNext() != null) {
            if (currentNode.getNext().getData().equals(value)) {
                currentNode.setNext(currentNode.getNext().getNext());
                return;
            }
            currentNode = currentNode.getNext();
        }
    }

    public void deleteByIndex(int index) {
        if (index >= size() || index < 0) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
        
        if (index == 0) {
            head = head.getNext();
            return;
        }
        
        Node<T> currentNode = head;
        for (int i = 0; i < index - 1; i++) {
            currentNode = currentNode.getNext();
        }

        if (currentNode.getNext() != null) {
            currentNode.setNext(currentNode.getNext().getNext());
        }
    }

    public void deleteFirst() {
        if (head == null) {
            throw new NoSuchElementException("List is empty nothing to delete."); // List is empty, nothing to delete
        }
        head = head.getNext();
    }

    public void deleteLast() {
        if (head == null){
            throw new NoSuchElementException("List is empty nothing to delete.");
        }
        Node<T> currentNode = head;
        if (size() == 1){
            head = null;
            return;
        }
        for (int i = 0; i < size() - 2; i++) {
            currentNode = currentNode.getNext();
        }
        currentNode.setNext(currentNode.getNext().getNext());
    }

    public int size() {
        int count = 0;
        if (head == null) {
            return count;
        }
        Node<T> currentNode = head;
        while (currentNode != null) {
            count++;
            currentNode = currentNode.getNext();
        }
        return count;
    }

    public boolean isEmpty() {
        return head == null;
    }

    public boolean contains(T value) {
        if (head.equals(value)){
            return true;
        }
        Node<T> currentNode = head;
        while (currentNode != null) {
            if (currentNode.getData().equals(value)){
                return true;
            }
            currentNode = currentNode.getNext();
        }
        return false;
    }

    public void clear() {
        head = null;
    }

    @Override
    public String toString() {
        Node<T> currentNode = head;
        StringBuilder sb = new StringBuilder();
        while (currentNode != null) {
            sb.append(currentNode.getData()).append(" -> ");
            currentNode = currentNode.getNext();
        }
        sb.append("null");
        return sb.toString();
    }

    public int indexOf(T value) {
        int count = 0;
        Node<T> currentNode = head;
        while (currentNode.getNext() != null) {
            if (currentNode.getData().equals(value)){
                return count;
            }
            count++;
            currentNode = currentNode.getNext();
        }
        if (currentNode.getData().equals(value)){
            return count;
        }
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