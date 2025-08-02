package NodeBasedStack;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extra edge-case tests for Node.
 */
@DisplayName("Node – extended coverage")
class NodeExtendedTest {

    /* ----------------------------------------------------------------
       1. setNext() re-assignment does not leak previous reference
       ---------------------------------------------------------------- */
    @Test
    @DisplayName("setNext() can safely overwrite an existing link")
    void setNextReassign() {
        Node<Integer> head = new Node<>(1);
        Node<Integer> first = new Node<>(2);
        Node<Integer> second = new Node<>(3);

        head.setNext(first);
        assertSame(first, head.getNext());

        head.setNext(second);             // re-assign
        assertSame(second, head.getNext());
        assertNotSame(first, head.getNext());
    }

    /* ----------------------------------------------------------------
       2. Long chain integrity (1000 nodes linked)
       ---------------------------------------------------------------- */
    @Test
    @DisplayName("Long linked-list chain preserves order")
    void longChainIntegrity() {
        final int N = 1_000;

        Node<Integer> head = new Node<>(0);
        Node<Integer> prev = head;

        for (int i = 1; i < N; i++) {
            Node<Integer> current = new Node<>(i);
            prev.setNext(current);
            prev = current;
        }

        // Traverse and validate order
        Node<Integer> cursor = head;
        for (int expected = 0; expected < N; expected++) {
            assertNotNull(cursor);
            assertEquals(expected, cursor.getData());
            cursor = cursor.getNext();
        }
        assertNull(cursor); // end of chain
    }
}