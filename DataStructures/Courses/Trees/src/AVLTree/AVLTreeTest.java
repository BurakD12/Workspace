package AVLTree;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended JUnit 5 tests for AVLTree and Node based on the provided method signatures.
 *
 * Assumptions (typical for AVL implementations):
 *  • Height convention: getHeight(null) == 0; node height is maintained by insert/delete.
 *  • inorderTraversal() prints elements in non-decreasing order (numbers printed as plain ints).
 *  • Deleting a value that doesn’t exist (or deleting from an empty tree) must not throw.
 *  • Duplicate inserts are implementation-defined: they may be ignored OR stored.
 *    Tests below are tolerant to either policy but always require the tree to remain AVL-valid.
 *
 * NOTE: These tests use reflection to access a private "root" field in AVLTree<T>.
 *       If your field name differs, update getRoot(..) accordingly.
 */
class AVLTreeTest {

    /* ---------- Helpers (reflection, traversal, invariants) ---------- */

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> Node<T> getRoot(AVLTree<T> tree) {
        try {
            Field root = AVLTree.class.getDeclaredField("root");
            root.setAccessible(true);
            return (Node<T>) root.get(tree);
        } catch (NoSuchFieldException e) {
            fail("AVLTree must declare a private field named 'root' of type Node<T>.");
            return null; // unreachable
        } catch (IllegalAccessException e) {
            fail("Unable to access AVLTree.root via reflection: " + e.getMessage());
            return null; // unreachable
        }
    }

    private static <T extends Comparable<T>> Node<T> find(Node<T> node, T value) {
        if (node == null) return null;
        int cmp = value.compareTo(node.getData());
        if (cmp == 0) return node;
        return (cmp < 0) ? find(node.getLeft(), value) : find(node.getRight(), value);
    }

    private static <T extends Comparable<T>> List<T> inorder(Node<T> n) {
        List<T> out = new ArrayList<>();
        inorder(n, out);
        return out;
    }

    private static <T extends Comparable<T>> void inorder(Node<T> n, List<T> out) {
        if (n == null) return;
        inorder(n.getLeft(), out);
        out.add(n.getData());
        inorder(n.getRight(), out);
    }

    /** Verifies AVL invariants recursively:
     *   1) |height(left) - height(right)| <= 1
     *   2) height(node) == max(height(left), height(right)) + 1
     *   3) local BST ordering by child links
     * (Global ordering is checked via inorder() in tests.)
     */
    private static <T extends Comparable<T>> void assertAvlOk(Node<T> n) {
        if (n == null) return;
        int hl = (n.getLeft() == null) ? 0 : n.getLeft().getHeight();
        int hr = (n.getRight() == null) ? 0 : n.getRight().getHeight();

        assertTrue(Math.abs(hl - hr) <= 1, () ->
                "AVL balance violated at node " + n.getData() + " (hl=" + hl + ", hr=" + hr + ")");

        assertEquals(Math.max(hl, hr) + 1, n.getHeight(), () ->
                "Height invariant violated at node " + n.getData());

        if (n.getLeft() != null) {
            assertTrue(n.getLeft().getData().compareTo(n.getData()) < 0,
                    "BST property violated on left link of " + n.getData());
        }
        if (n.getRight() != null) {
            assertTrue(n.getRight().getData().compareTo(n.getData()) > 0,
                    "BST property violated on right link of " + n.getData());
        }

        assertAvlOk(n.getLeft());
        assertAvlOk(n.getRight());
    }

    private static List<Integer> parseIntegersFromStdout(String s) {
        Matcher m = Pattern.compile("-?\\d+").matcher(s);
        List<Integer> out = new ArrayList<>();
        while (m.find()) out.add(Integer.parseInt(m.group()));
        return out;
    }

    private static <T> long countOf(List<T> list, T value) {
        return list.stream().filter(v -> Objects.equals(v, value)).count();
    }

    @SafeVarargs
    private static void assertThrowsOneOf(Runnable action, Class<? extends Throwable>... expected) {
        try {
            action.run();
            fail("Expected one of " + Arrays.toString(expected) + " to be thrown.");
        } catch (Throwable t) {
            for (Class<? extends Throwable> c : expected) {
                if (c.isInstance(t)) return; // ok
            }
            fail("Unexpected exception type: " + t.getClass().getName() + " — " + t.getMessage());
        }
    }

    /* ------------------ Basic behavior & traversal ------------------ */

    @Test
    @DisplayName("getHeight(null) == 0; empty inorder prints nothing")
    void emptyTreeBasics() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        assertEquals(0, avl.getHeight(null), "getHeight(null) must be 0");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream orig = System.out;
        System.setOut(new PrintStream(baos));
        try {
            avl.inorderTraversal();
        } finally {
            System.setOut(orig);
        }
        String printed = baos.toString().trim();
        assertTrue(printed.isEmpty(), "Empty tree should print nothing (or only whitespace).");
    }

    @Test
    @DisplayName("Single-node heights: root height must be 1")
    void singleNodeHeights() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        avl.insert(42);
        Node<Integer> root = getRoot(avl);
        assertNotNull(root);
        assertEquals(1, root.getHeight(), "A leaf's height should be 1.");
        assertEquals(List.of(42), inorder(root));
        assertAvlOk(root);
    }

    @Test
    @DisplayName("Insert a few items: inorder is sorted; AVL invariants hold")
    void insertInorderAndAvlInvariant() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        int[] values = { 10, 4, 14, 2, 6, 12, 16 };
        for (int v : values) avl.insert(v);

        Node<Integer> root = getRoot(avl);
        List<Integer> in = inorder(root);
        assertEquals(List.of(2,4,6,10,12,14,16), in, "Inorder must be sorted.");
        assertAvlOk(root);
    }

    /* ------------------ Rotations via classic patterns ------------------ */

    @Test
    @DisplayName("RR rotation (insert 1,2,3) should yield root == 2")
    void rotationRR() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        avl.insert(1);
        avl.insert(2);
        avl.insert(3);

        Node<Integer> root = getRoot(avl);
        assertNotNull(root);
        assertEquals(2, root.getData(), "Root should be 2 after RR case rebalancing.");
        assertAvlOk(root);
    }

    @Test
    @DisplayName("LL rotation (insert 3,2,1) should yield root == 2")
    void rotationLL() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        avl.insert(3);
        avl.insert(2);
        avl.insert(1);

        Node<Integer> root = getRoot(avl);
        assertNotNull(root);
        assertEquals(2, root.getData(), "Root should be 2 after LL case rebalancing.");
        assertAvlOk(root);
    }

    @Test
    @DisplayName("LR rotation (insert 3,1,2) should yield root == 2")
    void rotationLR() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        avl.insert(3);
        avl.insert(1);
        avl.insert(2);

        Node<Integer> root = getRoot(avl);
        assertNotNull(root);
        assertEquals(2, root.getData(), "Root should be 2 after LR case rebalancing.");
        assertAvlOk(root);
    }

    @Test
    @DisplayName("RL rotation (insert 1,3,2) should yield root == 2")
    void rotationRL() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        avl.insert(1);
        avl.insert(3);
        avl.insert(2);

        Node<Integer> root = getRoot(avl);
        assertNotNull(root);
        assertEquals(2, root.getData(), "Root should be 2 after RL case rebalancing.");
        assertAvlOk(root);
    }

    /* ------------------ Deletions: leaf / one child / two children / root ------------------ */

    @Test
    @DisplayName("Delete leaf node keeps AVL valid")
    void deleteLeaf() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        for (int v : new int[]{2,1,3}) avl.insert(v);

        avl.delete(1); // leaf
        Node<Integer> root = getRoot(avl);
        assertEquals(List.of(2,3), inorder(root));
        assertAvlOk(root);
    }

    @Test
    @DisplayName("Delete node with one child keeps AVL valid")
    void deleteOneChild() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        for (int v : new int[]{5,3,7,2}) avl.insert(v);

        avl.delete(3); // node 3 has single child (2)
        Node<Integer> root = getRoot(avl);
        assertEquals(List.of(2,5,7), inorder(root));
        assertAvlOk(root);
    }

    @Test
    @DisplayName("Delete node with two children keeps AVL valid")
    void deleteTwoChildren() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        for (int v : new int[]{20,10,30,5,15,25,40,13,17}) avl.insert(v);

        avl.delete(10);
        Node<Integer> root = getRoot(avl);
        List<Integer> expected = List.of(5,13,15,17,20,25,30,40); // 10 removed
        assertEquals(expected, inorder(root));
        assertAvlOk(root);
    }

    @Test
    @DisplayName("Deleting non-existent values should not throw and preserve invariants")
    void deleteNonExistentDoesNotThrow() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        for (int v : new int[]{10,5,15}) avl.insert(v);

        assertDoesNotThrow(() -> avl.delete(42));
        assertDoesNotThrow(() -> avl.delete(-7));

        Node<Integer> root = getRoot(avl);
        assertAvlOk(root);
        assertEquals(List.of(5,10,15), inorder(root));
    }

    /* ------------------ inorderTraversal() printing behavior ------------------ */

    @Test
    @DisplayName("inorderTraversal prints sorted order")
    void inorderTraversalPrintsSorted() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        List<Integer> data = List.of(8,3,10,1,6,14,4,7,13);
        data.forEach(avl::insert);

        Node<Integer> root = getRoot(avl);
        List<Integer> expected = inorder(root);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream orig = System.out;
        System.setOut(new PrintStream(baos));
        try {
            avl.inorderTraversal();
        } finally {
            System.setOut(orig);
        }
        List<Integer> printed = parseIntegersFromStdout(baos.toString());
        assertEquals(expected, printed, "Printed inorder must be sorted and match tree content.");
    }

    /* ------------------ Successor via reflection to private method ------------------ */

    @Test
    @DisplayName("inorderSuccessor(node) returns next higher key")
    void inorderSuccessorWorks() throws Exception {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        for (int v : new int[]{5,3,7,6,8}) avl.insert(v);

        Node<Integer> root = getRoot(avl);
        Node<Integer> node5 = find(root, 5);
        assertNotNull(node5, "Node(5) should exist.");

        Method succ = AVLTree.class.getDeclaredMethod("inorderSuccessor", Node.class);
        succ.setAccessible(true);
        Integer next = (Integer) succ.invoke(avl, node5.getRight());
        assertEquals(6, next, "Successor of 5 should be 6 (min of right subtree).");
    }

    /* ===================== Added EDGE CASES below ===================== */

    @Test
    @DisplayName("Insert null and delete null: must throw (NPE or IAE)")
    void nullOperations() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        assertThrowsOneOf(() -> avl.insert(null), NullPointerException.class, IllegalArgumentException.class);
        assertThrowsOneOf(() -> avl.delete(null), NullPointerException.class, IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Delete on empty tree should not throw")
    void deleteFromEmptyTree() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        assertDoesNotThrow(() -> avl.delete(123));
        Node<Integer> root = getRoot(avl);
        assertNull(root, "Root should remain null after delete on empty tree.");
    }

    @Test
    @DisplayName("Duplicate inserts: tree remains valid; count is implementation-defined")
    void duplicateInsertsTolerated() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        avl.insert(5);
        avl.insert(5);
        avl.insert(5);

        Node<Integer> root = getRoot(avl);
        assertAvlOk(root);
        List<Integer> in = inorder(root);
        long c = countOf(in, 5);
        assertTrue(c >= 1 && c <= 3, "After three duplicate inserts, count of '5' should be between 1 and 3.");
    }

    @Test
    @DisplayName("Repeated deletions of the same key eventually remove it, then become no-ops")
    void repeatedDeleteSameValue() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        for (int i = 0; i < 5; i++) avl.insert(10); // allow either one or many 10s

        for (int i = 0; i < 5; i++) { // delete up to 5 times; excess deletes should no-op
            assertDoesNotThrow(() -> avl.delete(10));
            Node<Integer> root = getRoot(avl);
            assertAvlOk(root);
        }
        Node<Integer> root = getRoot(avl);
        if (root != null) {
            assertEquals(0L, countOf(inorder(root), 10), "All 10s should be removed after repeated deletions.");
        }
    }

    @Test
    @DisplayName("Monotonic ascending inserts stay balanced")
    void ascendingSequenceStaysBalanced() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        for (int i = 1; i <= 100; i++) avl.insert(i);
        Node<Integer> root = getRoot(avl);
        assertAvlOk(root);
        assertEquals(IntStream.rangeClosed(1,100).boxed().toList(), inorder(root));
    }

    @Test
    @DisplayName("Monotonic descending inserts stay balanced")
    void descendingSequenceStaysBalanced() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        for (int i = 100; i >= 1; i--) avl.insert(i);
        Node<Integer> root = getRoot(avl);
        assertAvlOk(root);
        assertEquals(IntStream.rangeClosed(1,100).boxed().toList(), inorder(root));
    }

    @Test
    @DisplayName("Extreme integer values can be inserted and ordered correctly")
    void extremeValues() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        avl.insert(Integer.MIN_VALUE);
        avl.insert(0);
        avl.insert(Integer.MAX_VALUE);
        Node<Integer> root = getRoot(avl);
        assertAvlOk(root);
        assertEquals(List.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE), inorder(root));
    }

    @Test
    @DisplayName("Works with generic types: Strings")
    void worksWithStrings() {
        AVLTree<String> avl = new AVLTree<String>();
        List<String> words = List.of("delta", "alpha", "charlie", "bravo");
        words.forEach(avl::insert);

        Node<String> root = getRoot(avl);
        assertNotNull(root);
        assertAvlOk(root);
        assertEquals(List.of("alpha","bravo","charlie","delta"), inorder(root));
    }

    @Test
    @DisplayName("Works with custom Comparable type")
    void worksWithCustomComparable() {
        class Key implements Comparable<Key> {
            final int k;
            Key(int k) { this.k = k; }
            @Override public int compareTo(Key o) { return Integer.compare(this.k, o.k); }
            @Override public String toString() { return String.valueOf(k); }
        }

        AVLTree<Key> avl = new AVLTree<Key>();
        List<Key> keys = List.of(new Key(5), new Key(1), new Key(9), new Key(3), new Key(7));
        keys.forEach(avl::insert);

        Node<Key> root = getRoot(avl);
        assertNotNull(root);
        assertAvlOk(root);

        List<Integer> actual = inorder(root).stream().map(k -> k.k).toList();
        assertEquals(List.of(1,3,5,7,9), actual);
    }

    @Test
    @DisplayName("Deletion-triggered rebalancing maintains AVL invariants")
    void deletionTriggersRebalanceButStaysValid() {
        AVLTree<Integer> avl = new AVLTree<Integer>();
        // Build a complete-ish tree:
        for (int v : new int[]{50,30,70,20,40,60,80,10,25,35,45,55,65,75,85}) avl.insert(v);

        // Delete a sequence designed to cause rebalance at various heights:
        for (int v : new int[]{10,20,25,30,35,40,45}) {
            avl.delete(v);
            assertAvlOk(getRoot(avl));
        }

        // Final in-order must be sorted and contain remaining items:
        List<Integer> expected = List.of(50,55,60,65,70,75,80,85);
        assertEquals(expected, inorder(getRoot(avl)));
    }
}

/* ------------------ Node<T> unit tests ------------------ */

class NodeTest {

    @Test
    @DisplayName("Node getters/setters for data/children/height")
    void nodeAccessorsWork() {
        Node<Integer> n = new Node<>(10);
        assertEquals(10, n.getData());
        assertNull(n.getLeft());
        assertNull(n.getRight());

        n.setData(11);
        assertEquals(11, n.getData());

        Node<Integer> left = new Node<>(5);
        Node<Integer> right = new Node<>(20);
        n.setLeft(left);
        n.setRight(right);
        assertSame(left, n.getLeft());
        assertSame(right, n.getRight());

        n.setHeight(3);
        assertEquals(3, n.getHeight());
    }

    @Test
    @DisplayName("Node height default is 1 for new leaf")
    void defaultLeafHeightIsOne() {
        Node<String> n = new Node<>("x");
        assertEquals(1, n.getHeight(), "Newly created leaf node should have height = 1.");
    }
}