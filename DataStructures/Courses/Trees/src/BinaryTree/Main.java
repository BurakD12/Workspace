package BinaryTree;

public class Main {
    public static void main(String[] args) {
        BinaryTree<Integer> tree = new BinaryTree<>();

        tree.root = new Node<>(1);
        tree.root.right = new Node<>(2);
        tree.root.left = new Node<>(3);
        tree.root.right.left = new Node<>(4);
        tree.root.right.right = new Node<>(5);  
        tree.root.left.left = new Node<>(6);
        tree.root.left.right = new Node<>(7);

        System.out.print("Inorder Traversal: ");
        tree.inorderTraversal();

        System.out.print("\nPreorder Traversal: ");
        tree.preorderTraversal();

        System.out.print("\nPostorder Traversal: ");
        tree.postorderTraversal();
    }
}
