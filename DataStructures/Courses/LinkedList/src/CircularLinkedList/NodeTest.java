package CircularLinkedList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    /* ------------------------------------------------------------------
     *  Constructor & data access
     * ------------------------------------------------------------------ */
    @Test
    void constructorStoresData() {
        Node<Integer> node = new Node<>(42);
        assertEquals(42, node.getData());
    }

    @Test
    void setDataUpdatesValue() {
        Node<String> node = new Node<>("A");
        node.setData("B");
        assertEquals("B", node.getData());
    }

    /* ------------------------------------------------------------------
     *  next-reference handling
     * ------------------------------------------------------------------ */
    @Test
    void nextIsNullAfterConstruction() {
        Node<Integer> node = new Node<>(1);
        assertNull(node.getNext());
    }

    @Test
    void setNextLinksNodes() {
        Node<Integer> first  = new Node<>(1);
        Node<Integer> second = new Node<>(2);

        first.setNext(second);

        assertSame(second, first.getNext());
        assertNull(second.getNext());
    }

    @Test
    void allowsSelfReferenceForCircularity() {
        Node<String> node = new Node<>("X");
        node.setNext(node);
        assertSame(node, node.getNext());
    }
}
