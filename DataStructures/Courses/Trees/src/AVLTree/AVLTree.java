package AVLTree;

public class AVLTree<T extends Comparable<T>> {
    private Node<T> root;

    public AVLTree() {
        this.root = null;
    }

    public void insert(T value) throws NullPointerException{
        if (value == null){
            throw new NullPointerException();
        }
        root = insert(value, root);
    }

    public Node<T> insert(T value, Node<T> node) {
        if (node == null) {
            return new Node<T>(value);
        } else if (node.getData().compareTo(value) < 0) {
            node.setRight(insert(value, node.getRight()));
        } else if (node.getData().compareTo(value) > 0) {
            node.setLeft(insert(value, node.getLeft()));
        } else {
            // Value already exists in the tree
            return node;
        }

        // Update height of the current node
        node.setHeight(Math.max(getHeight(node.getLeft()), getHeight(node.getRight())) + 1);

        int balanceFactor = getHeight(node.getLeft()) - getHeight(node.getRight());
        if (balanceFactor < -1) {
            if (value.compareTo(node.getRight().getData()) > 0) {
                node = rotateLeft(node);
            } else {
                node.setRight(rotateRight(node.getRight()));
                node = rotateLeft(node);
            }
        } else if (balanceFactor > 1) {
            if (value.compareTo(node.getLeft().getData()) < 0) {
                node = rotateRight(node);
            } else {
                node.setLeft(rotateLeft(node.getLeft()));
                node = rotateRight(node);
            }
        }

        return node;
    }

    public void delete(T value) throws NullPointerException{
        if (value == null){
            throw new NullPointerException();
        }
        root = delete(value, root);
    }

    private Node<T> delete(T value, Node<T> node) {
        if (node == null) {
            return null;
        }
        if (value.compareTo(node.getData()) < 0) {
            node.setLeft(delete(value, node.getLeft()));
        } else if (value.compareTo(node.getData()) > 0) {
            node.setRight(delete(value, node.getRight()));
        } else if (value.compareTo(node.getData()) == 0) {
            if (node.getLeft() == null && node.getRight() == null) {
                return null;
            } else if (node.getLeft() == null) {
                return node.getRight();
            } else if (node.getRight() == null) {
                return node.getLeft();
            } else {
                T successorValue = inorderSuccessor(node.getLeft());
                node.setData(successorValue);
                node.setLeft(delete(successorValue, node.getLeft()));
            }
        }

        node.setHeight(Math.max(getHeight(node.getLeft()), getHeight(node.getRight())) + 1);

        int balanceFactor = getHeight(node.getLeft()) - getHeight(node.getRight());

        if (balanceFactor < -1){
            int rightChildBalanceFactor = getHeight(node.getRight().getLeft()) - getHeight(node.getRight().getRight());
            if (rightChildBalanceFactor <= 0){
                node = rotateLeft(node);
            } else {
                node.setRight(rotateRight(node.getRight()));
                node = rotateLeft(node);
            }
        } else if (balanceFactor > 1){
            int leftChildBalanceFactor = getHeight(node.getLeft().getLeft()) - getHeight(node.getLeft().getRight());
            if (leftChildBalanceFactor >= 0){
                node = rotateRight(node);
            } else {
                node.setLeft(rotateLeft(node.getLeft()));
                node = rotateRight(node);
            }
        }
        return node;
    }

    private T inorderSuccessor(Node<T> node) {
        Node<T> current = node;
        while (current.getLeft() != null) {
            current = current.getLeft();
        }
        return current.getData();
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

    private Node<T> rotateRight(Node<T> y) {
        Node<T> x = y.getLeft();
        Node<T> beta = x.getRight();

        y.setLeft(beta);
        x.setRight(y);

        // Update heights
        y.setHeight(Math.max(getHeight(y.getLeft()), getHeight(y.getRight())) + 1);
        x.setHeight(Math.max(getHeight(x.getLeft()), getHeight(x.getRight())) + 1);

        return x;
    }

    private Node<T> rotateLeft(Node<T> x) {
        Node<T> y = x.getRight();
        Node<T> beta = y.getLeft();

        x.setRight(beta);
        y.setLeft(x);

        x.setHeight(Math.max(getHeight(x.getLeft()), getHeight(x.getRight())) + 1);
        y.setHeight(Math.max(getHeight(y.getLeft()), getHeight(y.getRight())) + 1);
        return y;
    }

    int getHeight(Node<T> node) {
        return node == null ? 0 : node.getHeight();
    }
}
