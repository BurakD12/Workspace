package DoublyLinkedList;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the standalone Node<T> class.
 */
class NodeTest {

    @Test
    @DisplayName("Constructor sets data and null links")
    void constructorInitialisesFields() {
        Node<Integer> n = new Node<>(42);

        assertEquals(42, n.getData(), "Data must equal ctor argument");
        assertNull(n.getNext(), "Next should default to null");
        assertNull(n.getPrev(), "Prev should default to null");
    }

    @Test
    @DisplayName("All setters and getters work and can be chained")
    void settersAndGetters() {
        Node<String> a = new Node<>("A");
        Node<String> b = new Node<>("B");
        Node<String> c = new Node<>("C");

        a.setNext(b);
        b.setPrev(a);
        b.setNext(c);
        c.setPrev(b);

        b.setData("Z");

        assertSame(a, b.getPrev());
        assertSame(c, b.getNext());
        assertEquals("Z", b.getData());
    }
}
