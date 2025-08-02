package NodeBasedStack;

import ArrayBasedStack.StackUnderflowException;

public class NodeBasedStack<T> {
    Node<T> top;

    public NodeBasedStack() {
        this.top = null;
    }

    public void push(T item){
        Node<T> newTop = new Node(item);
        newTop.setNext(top);
        top = newTop;
    }

    public T pop() throws StackUnderflowException {
        if (top == null){
            throw new StackUnderflowException("There are no elements to pop.");
        }
        else {
            T data = top.getData();
            top = top.getNext();
            return data;
        }
    }

    public T peek() throws StackUnderflowException {
        if (top == null){
            throw new StackUnderflowException("There are no elements to peek at.");
        }
        else {
            return top.getData();
        }
    }

    public boolean isEmpty() {
        return top == null;
    }

    public int size() {
        int count = 0;
        if (top == null){
            return count;
        }
        Node<T> currentNode = top;
        while (currentNode != null) {
            count++;
            currentNode = currentNode.getNext();
        }
        return count;
    }
}