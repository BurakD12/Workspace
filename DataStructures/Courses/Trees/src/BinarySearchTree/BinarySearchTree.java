package BinarySearchTree;

public class BinarySearchTree<T extends Comparable<T>> {
    private Node<T> root;

    public BinarySearchTree() {
        this.root = null;
    }

    public void insert(T value) {
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
        if (value.compareTo(node.getData()) < 0) {
            node.setLeft(delete(node.getLeft(), value));
        }
        else if (value.compareTo(node.getData()) > 0) {
            node.setRight(delete(node.getRight(), value));
        }
        else if (value.compareTo(node.getData()) == 0) {
            if (node.getLeft() == null && node.getRight() == null){
                return null;
            } else if (node.getLeft() == null) {
            
            } else if (node.getRight() == null) {

            } else {
            
            }
        }
    }

    private T inorderSuccessor(Node<T> node) {

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
