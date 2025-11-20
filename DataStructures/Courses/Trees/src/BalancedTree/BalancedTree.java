package BalancedTree;

public class BalancedTree<T> {
    public Node<T> root;

    public BalancedTree() {
        this.root = null;
    }

    public boolean isBalanced() {
        return isBalanced(root);
    }

    private boolean isBalanced(Node<T> node) {
        if (node == null) {
            return true;
        }
        
        boolean leftBalanced = isBalanced(node.left);
        boolean rightBalanced = isBalanced(node.right);

        int leftHeight = getHeight(node.left); 
        int rightHeight = getHeight(node.right); 

        node.height = (Math.max(leftHeight, rightHeight) + 1); 

        int balanceFactor = leftHeight - rightHeight;

        return leftBalanced && rightBalanced && Math.abs(balanceFactor) <= 1; 
    }

    public int getHeight(Node<T> node) {
        if (node == null) {
            return 0;
        }
        return node.height;
    }
}
