package ArrayBasedStack;

public class ArrayBasedStack<T> {
    private T[] stack;
    private int top;
    private int capacity;

    public ArrayBasedStack(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero.");
        }

        this.capacity = capacity;
        this.stack = (T[]) new Object[capacity];
        this.top = -1;
    }

    public void push(T item) throws StackOverflowException {
        if (!isFull()){
            top++;
            stack[top] = item;
        }
        else {
            throw new StackOverflowException("Stack is full, cannot push item: " + item);
        }
    }

    public T pop() throws StackUnderflowException{
        if (!isEmpty()){
            T poppedItem = stack[top--];
            return poppedItem;
        }
        else {
            throw new StackUnderflowException("Stack is empty, cannot pop an item.");
        }
    }

    public T peek() throws StackUnderflowException{
        if (!isEmpty()){
            return stack[top];
        }
        else {
            throw new StackUnderflowException("The stack is empty, nothing to peek at.");
        }
    }

    public boolean isEmpty() {
        return top == -1;
    }

    public boolean isFull() {
        return top == capacity - 1;
    }

    public int size() {
        return top + 1;
    }
}
