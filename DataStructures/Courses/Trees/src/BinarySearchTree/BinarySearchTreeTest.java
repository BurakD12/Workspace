package BinarySearchTree;

import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for {@link BinarySearchTree}.
 *
 * <p>Assumptions about the reference implementation:
 * <ul>
 *   <li>{@code inorderTraversal()} prints elements to {@code System.out},
 *       separated by whitespace and in strict ascending order.</li>
 *   <li>Duplicate insertions are accepted and appear exactly as many times as
 *       they were inserted (skip duplicate-related tests if your version
 *       disallows duplicates).</li>
 *   <li>{@code delete(T)} is a no-op when the value is absent.</li>
 * </ul>
 */
class BinarySearchTreeTest {

    private BinarySearchTree<Integer> bst;

    /* ─────────────────────────────
     *  Test lifecycle
     * ───────────────────────────── */
    @BeforeEach
    void setUp() {
        bst = new BinarySearchTree<>();
    }

    /* ─────────────────────────────
     *  Helper – capture traversal
     * ───────────────────────────── */
    private List<Integer> inorderAsList(BinarySearchTree<Integer> tree) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        tree.inorderTraversal();

        System.setOut(originalOut);
        String output = baos.toString().trim();
        if (output.isEmpty()) return Collections.emptyList();

        return Arrays.stream(output.split("\\s+"))
                     .map(Integer::parseInt)
                     .collect(Collectors.toList());
    }

    /* ─────────────────────────────
     *  Original core tests
     * ───────────────────────────── */

    @Test
    void insertProducesSortedInorderTraversal() {
        int[] values = {50, 30, 70, 20, 40, 60, 80};
        Arrays.stream(values).forEach(bst::insert);

        List<Integer> traversal = inorderAsList(bst);
        List<Integer> expected = Arrays.stream(values)
                                       .boxed()
                                       .sorted()
                                       .collect(Collectors.toList());

        assertEquals(expected, traversal,
                "In-order traversal should yield ascending order");
    }

    @Test
    void inorderTraversalOnEmptyTreeProducesNoOutput() {
        assertTrue(inorderAsList(bst).isEmpty(),
                "Empty tree should produce no output");
    }

    @Test
    void deleteLeafNodeRemovesOnlyThatNode() {
        bst.insert(10);
        bst.insert(5);
        bst.insert(15);   // 5 & 15 are leaves

        bst.delete(5);
        assertEquals(List.of(10, 15), inorderAsList(bst));
    }

    @Test
    void deleteNodeWithOneChildReplacesParentLink() {
        /* 40
         * /
         *20
         * /
         *10
         */
        bst.insert(40);
        bst.insert(20);
        bst.insert(10);

        bst.delete(20);
        assertEquals(List.of(10, 40), inorderAsList(bst));
    }

    @Test
    void deleteNodeWithTwoChildrenUsesInorderSuccessor() {
        int[] vals = {50, 25, 75, 60};
        Arrays.stream(vals).forEach(bst::insert);

        bst.delete(50);   // root with two children
        assertEquals(List.of(25, 60, 75), inorderAsList(bst));
    }

    @Test
    void deleteRootWhenItBecomesEmpty() {
        bst.insert(42);
        bst.delete(42);
        assertTrue(inorderAsList(bst).isEmpty());
    }

    @Test
    void deleteNonExistentKeyDoesNotThrow() {
        bst.insert(1);
        bst.insert(2);

        assertDoesNotThrow(() -> bst.delete(999));
        assertEquals(List.of(1, 2), inorderAsList(bst));
    }

    @Test
    void deleteOnEmptyTreeDoesNotThrow() {
        assertDoesNotThrow(() -> bst.delete(10));
        assertTrue(inorderAsList(bst).isEmpty());
    }

    @Test
    void insertingDuplicatesPreservesMultiplicity() {
        // Skip if duplicates are not supported
        bst.insert(5);
        bst.insert(5);
        bst.insert(5);

        assertEquals(List.of(5), inorderAsList(bst));
    }

    @Test
    void insertNullThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> bst.insert(null));
    }

    /* ─────────────────────────────
     *  Additional edge-case tests
     * ───────────────────────────── */

    /* 1. Degenerate trees (skewed) */
    @Test
    void ascendingInsertsYieldSortedTraversalEvenWhenSkewedRight() {
        for (int i = 1; i <= 10; i++) bst.insert(i);
        assertEquals(
            List.of(1,2,3,4,5,6,7,8,9,10),
            inorderAsList(bst));
    }

    @Test
    void descendingInsertsYieldSortedTraversalEvenWhenSkewedLeft() {
        for (int i = 10; i >= 1; i--) bst.insert(i);
        assertEquals(
            List.of(1,2,3,4,5,6,7,8,9,10),
            inorderAsList(bst));
    }

    /* 2. Repeated root deletions until empty */
    @Test
    void successiveRootDeletionsEventuallyProduceEmptyTree() {
        int[] vals = {40, 20, 60, 10, 30, 50, 70};
        Arrays.stream(vals).forEach(bst::insert);

        while (!inorderAsList(bst).isEmpty()) {
            Integer key = inorderAsList(bst).get(0); // delete smallest each time
            bst.delete(key);
        }
        assertTrue(inorderAsList(bst).isEmpty());
    }

    /* 3. Duplicate handling: delete one copy */
    @Test
    void deleteRemovesOnlyOneInstanceWhenDuplicatesPresent() {
        bst.insert(7);
        bst.insert(7);
        bst.insert(7);

        bst.delete(7);
        assertEquals(List.of(), inorderAsList(bst));
    }

    /* 4. Null in delete */
    @Test
    void deleteNullThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> bst.delete(null));
    }

    /* 5. Extreme int boundaries */
    @Test
    void handlesIntegerMinAndMaxValues() {
        bst.insert(Integer.MIN_VALUE);
        bst.insert(0);
        bst.insert(Integer.MAX_VALUE);

        assertEquals(
            List.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE),
            inorderAsList(bst));

        bst.delete(Integer.MIN_VALUE);
        bst.delete(Integer.MAX_VALUE);
        assertEquals(List.of(0), inorderAsList(bst));
    }

    /* 6. Generic type other than Integer */
    @Test
    void worksWithDifferentComparableTypeLikeString() {
        BinarySearchTree<String> strTree = new BinarySearchTree<>();
        strTree.insert("delta");
        strTree.insert("alpha");
        strTree.insert("gamma");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        strTree.inorderTraversal();
        System.setOut(System.out);

        assertEquals("alpha delta gamma", out.toString().trim());
    }

    /* 7. Successor is immediate right child */
    @Test
    void deleteNodeWhoseSuccessorIsItsImmediateRightChild() {
        /* 10
         *   \
         *   15
         *   /
         * 12
         */
        bst.insert(10);
        bst.insert(15);
        bst.insert(12);

        bst.delete(10);
        assertEquals(List.of(12, 15), inorderAsList(bst));
    }

    /* 8. Random stress test */
    @Test
    void randomStressTestMaintainsSortedOrder() {
        Random rnd = new Random(42);
        Set<Integer> reference = new TreeSet<>();

        // insert 1000 unique ints
        while (reference.size() < 1_000) {
            int n = rnd.nextInt(10_000);
            if (reference.add(n)) bst.insert(n);
        }
        assertEquals(new ArrayList<>(reference), inorderAsList(bst));

        // delete 500 random keys
        int deleted = 0;
        for (Integer key : new ArrayList<>(reference)) {
            if (deleted == 500) break;
            bst.delete(key);
            reference.remove(key);
            deleted++;
        }
        assertEquals(new ArrayList<>(reference), inorderAsList(bst));
    }
}