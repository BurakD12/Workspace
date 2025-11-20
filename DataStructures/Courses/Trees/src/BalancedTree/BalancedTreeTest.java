package BalancedTree;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link BalancedTree} and {@link Node}.
 *
 *  Assumed public contract:
 *  • An empty tree is balanced.                          (isBalanced() → true)
 *  • getHeight(null) returns 0.                          (null-safe)
 *  • isBalanced() recomputes height fields on the fly.   (idempotent)
 *  • Balance factor |leftHeight - rightHeight| ≤ 1 for every subtree.
 */
class BalancedTreeTest {

    /* ────────────────────────────────────────────────
     *  Fundamental scenarios
     * ──────────────────────────────────────────────── */
    @Test
    void emptyTree_isBalanced() {
        BalancedTree<Integer> tree = new BalancedTree<>();
        assertTrue(tree.isBalanced(), "Empty tree must be balanced");
    }

    @Test
    void singleNode_isBalancedAndHeightOne() {
        BalancedTree<Integer> tree = new BalancedTree<>();
        tree.root = new Node<>(1);

        assertTrue(tree.isBalanced());
        tree.isBalanced();                    // populate height
        assertEquals(1, tree.getHeight(tree.root));
    }

    @Test
    void perfectTwoLevelTree_isBalancedAndHeightTwo() {
        BalancedTree<Integer> tree = perfect2LevelTree();

        assertTrue(tree.isBalanced());
        tree.isBalanced();
        assertEquals(2, tree.getHeight(tree.root));
    }

    @Test
    void leftChain_depthThree_isUnbalanced() {
        BalancedTree<Integer> tree = leftChainTree(3);
        assertFalse(tree.isBalanced());
    }

    @Test
    void rightChain_depthThree_isUnbalanced() {
        BalancedTree<Integer> tree = rightChainTree(3);
        assertFalse(tree.isBalanced());
    }

    /* ────────────────────────────────────────────────
     *  API contract for getHeight
     * ──────────────────────────────────────────────── */
    @Test
    void getHeight_null_returnsZero() {
        BalancedTree<Integer> tree = new BalancedTree<>();
        assertEquals(0, tree.getHeight(null));
    }

    @Test
    void getHeight_returnsNodeHeightField() {
        BalancedTree<Integer> tree = new BalancedTree<>();
        Node<Integer> node = new Node<>(99);
        node.height = 7;
        assertEquals(7, tree.getHeight(node));
    }

    /* ────────────────────────────────────────────────
     *  Additional edge-case coverage
     * ──────────────────────────────────────────────── */

    /** Left subtree exactly one level deeper than right → still balanced. */
    @Test
    void mildlySkewedTree_stillBalanced() {
        BalancedTree<Integer> tree = new BalancedTree<>();
        tree.root = new Node<>(10);
        tree.root.left = new Node<>(5);
        tree.root.left.left = new Node<>(3);      // depth 3 on left
        tree.root.right = new Node<>(15);         // depth 2 on right

        assertTrue(tree.isBalanced());
    }

    /** Imbalance buried in a deep right-hand branch should be detected. */
    @Test
    void deepSubtreeImbalance_detected() {
        BalancedTree<Integer> tree = new BalancedTree<>();
        tree.root = new Node<>(1);
        tree.root.left  = new Node<>(2);
        tree.root.right = rightChainTree(3).root;    // depth 4 on right

        assertFalse(tree.isBalanced());
    }

    /** A very deep path (linked-list-shaped tree) is unbalanced. */
    @Test
    void veryDeepLeftChain_isUnbalanced() {
        BalancedTree<Integer> tree = leftChainTree(100);
        assertFalse(tree.isBalanced());
    }

    /** isBalanced() must ignore stale height values and recompute correctly. */
    @Test
    void heightRecomputedByIsBalanced() {
        BalancedTree<Integer> tree = perfect2LevelTree();
        tree.root.height = 42;
        tree.root.left.height = 99;
        tree.root.right.height = 99;

        assertTrue(tree.isBalanced());          // still balanced
        assertEquals(2, tree.getHeight(tree.root));
    }

    /** Calling isBalanced() repeatedly should give consistent results. */
    @Test
    void isBalanced_isIdempotent() {
        BalancedTree<Integer> tree = perfect2LevelTree();
        boolean first  = tree.isBalanced();
        boolean second = tree.isBalanced();
        assertEquals(first, second);
    }

    /** Generic type parameter should not affect balancing logic. */
    @Test
    void treeWithStringData_isBalanced() {
        BalancedTree<String> tree = new BalancedTree<>();
        tree.root = new Node<>("root");
        tree.root.left  = new Node<>("L");
        tree.root.right = new Node<>("R");

        assertTrue(tree.isBalanced());
    }

    /* ────────────────────────────────────────────────
     *  Helper builders
     * ──────────────────────────────────────────────── */

    private static BalancedTree<Integer> perfect2LevelTree() {
        BalancedTree<Integer> t = new BalancedTree<>();
        t.root = new Node<>(10);
        t.root.left  = new Node<>(5);
        t.root.right = new Node<>(15);
        return t;
    }

    /** Builds a chain leaning left of the requested depth (≥1). */
    private static BalancedTree<Integer> leftChainTree(int depth) {
        BalancedTree<Integer> t = new BalancedTree<>();
        t.root = new Node<>(1);
        Node<Integer> curr = t.root;
        for (int i = 1; i < depth; i++) {
            curr.left = new Node<>(i + 1);
            curr = curr.left;
        }
        return t;
    }

    /** Builds a chain leaning right of the requested depth (≥1). */
    private static BalancedTree<Integer> rightChainTree(int depth) {
        BalancedTree<Integer> t = new BalancedTree<>();
        t.root = new Node<>(1);
        Node<Integer> curr = t.root;
        for (int i = 1; i < depth; i++) {
            curr.right = new Node<>(i + 1);
            curr = curr.right;
        }
        return t;
    }
}