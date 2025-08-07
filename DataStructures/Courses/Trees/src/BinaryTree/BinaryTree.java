package BinaryTree;

public class BinaryTree<T> {
    public Node<T> root;

    public BinaryTree() {
        this.root = null;
    }

    public void inorderTraversal() {
        inorderTraversal(root);
    }

    private void inorderTraversal(Node<T> node) {
        if (node == null) {
            return;
        }
        inorderTraversal(node.getLeft());
        System.out.print(node.data + " ");
        inorderTraversal(node.getRight());
    }

    public void preorderTraversal() {
        preorderTraversal(root);
    }

    private void preorderTraversal(Node<T> node) {
        if (node == null) {
            return;
        }
        System.out.print(node.data + " ");
        preorderTraversal(node.getLeft());
        preorderTraversal(node.getRight());
    }

    public void postorderTraversal() {
        postorderTraversal(root);
    }

    private void postorderTraversal(Node<T> node) {
        if (node == null) {
            return;
        }

        postorderTraversal(node.getLeft());
        postorderTraversal(node.getRight());
        System.out.print(node.data + " ");
    }
}
