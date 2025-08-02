package DoublyLinkedList;

import java.util.NoSuchElementException;

public class DoublyLinkedList<T> {
    private Node<T> head;

    public DoublyLinkedList() {
        this.head = null;
    }

    public void append(T value) {
        Node<T> newNode = new Node<>(value);
        if (head == null) {
            head = newNode;
        } else {
            Node<T> currentNode = head;
            while (currentNode.getNext() != null) {
                currentNode = currentNode.getNext();
            }
            currentNode.setNext(newNode);
            newNode.setPrev(currentNode);
        }
    }

    public void prepend(T value) {
        Node<T> newNode = new Node<>(value);
        if (head == null){
            head = newNode;
            return;
        }
        newNode.setNext(head);
        head.setPrev(newNode);
        head = newNode;
    }

    public void deleteByValue(T value) {
        Node<T> currentNode = head;
        if (head == null) {
            return;
        }
        if (head.getData() == null || head.getData().equals(value)) {
            head = head.getNext();
            head.setPrev(null);
            return;
        }
        while (currentNode.getData() == null || currentNode.getNext() != null) {
            if (currentNode.getNext().getData().equals(value)) {
                currentNode.setNext(currentNode.getNext().getNext());
                if (currentNode.getNext() != null) {
                    currentNode.getNext().setPrev(currentNode);
                }
                return;
            }
            currentNode = currentNode.getNext();
        }
        throw new NoSuchElementException();
    }

    public void deleteByIndex(int index) {
        if (index >= size() || index < 0) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }

        if (index == 0) {
            head = head.getNext();
            head.setPrev(null);
            return;
        }

        Node<T> currentNode = head;
        for (int i = 0; i < index - 1; i++) {
            currentNode = currentNode.getNext();
        }

        if (currentNode.getNext() != null) {
            currentNode.setNext(currentNode.getNext().getNext());
            if (currentNode.getNext() != null) {
                currentNode.getNext().setPrev(currentNode);
            }
        }

        

    }

    public void deleteFirst() {
        if (head == null) {
            throw new NoSuchElementException("List is empty nothing to delete.");
        }
        head = head.getNext();
        if (head != null){
            head.setPrev(null);
        }
    }

    public void deleteLast() {
        if (head == null) {
            throw new NoSuchElementException("List is empty nothing to delete.");
        }
        Node<T> currentNode = head;
        if (size() == 1) {
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
        Node<T> currentNode = head;
        while (currentNode != null) {
            if (currentNode.getData() == value || currentNode.getData().equals(value)) {
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
        if (head == null) {
            return "[]";
        }

        Node<T> currentNode = head;
        StringBuilder sb = new StringBuilder();

        sb.append("[");
        while (currentNode != null) {
            sb.append(currentNode.getData());
            currentNode = currentNode.getNext();
            if (currentNode != null) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public int indexOf(T value) {
        int count = 0;
        Node<T> currentNode = head;
        while (currentNode != null) {
            if (currentNode.getData() == value || currentNode.getData().equals(value)) {
                return count;
            }
            count++;
            currentNode = currentNode.getNext();
        }
        return -1;
    }

    public T get(int index) {
        if (index > size() - 1) {
            throw new IndexOutOfBoundsException("Index is out of bounds");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index is out of bounds");
        }
        Node<T> currentNode = head;
        for (int i = 0; i < index; i++) {
            currentNode = currentNode.getNext();
        }
        return currentNode.getData();
    }
}
