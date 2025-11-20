package BinarySearchTree;

public class BinarySearchTree<T extends Comparable<T>> {
    private Node<T> root;

    public BinarySearchTree() {
        this.root = null;
    }

    public void insert(T value) throws NullPointerException{
        if (value == null){
            throw new NullPointerException();
        }
        root = insert(root, value);
    }

    private Node<T> insert(Node<T> node, T value) {
        if (node == null) {
            return new Node<>(value);
        }
        if (value.compareTo(node.getData()) < 0) {
            node.setLeft(insert(node.getLeft(), value));
        } else if (value.compareTo(node.getData()) > 0) {
            node.setRight(insert(node.getRight(), value));
        }
        return node;
    }

    public void delete(T value) {
        root = delete(root, value);
    }

    private Node<T> delete(Node<T> node, T value) {
        if (node == null){
            return null;
        }
        if (value.compareTo(node.getData()) < 0) {
            node.setLeft(delete(node.getLeft(), value));
        } else if (value.compareTo(node.getData()) > 0) {
            node.setRight(delete(node.getRight(), value));
        } else if (value.compareTo(node.getData()) == 0) {
            if (node.getLeft() == null && node.getRight() == null){
                return null;
            } else if (node.getLeft() == null) {
                return node.getRight();
            } else if (node.getRight() == null) {
                return node.getLeft();
            } else {
                T successorValue = inorderSuccessor(node.getRight());
                node.setData(successorValue);
                node.setRight(delete(node.getRight(), successorValue));
            }
        }
        return node;
    }

    private T inorderSuccessor(Node<T> node) {
        while (node.getLeft() != null) {
            node = node.getLeft();
        }
        return node.getData();
    }

    public void inorderTraversal() {
        inorderTraversal(root);
    }

    private void inorderTraversal(Node<T> node) {
        if (node != null) {
            inorderTraversal(node.getLeft());
            System.out.print(node.getData() + " ");
            inorderTraversal(node.getRight());
        }
    }
}
