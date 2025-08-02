package DoublyLinkedList;

import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Exhaustive tests for the DoublyLinkedList<T> public API. */
class DoublyLinkedListTest {

    DoublyLinkedList<Integer> list;

    @BeforeEach
    void setUp() {
        list = new DoublyLinkedList<>();
    }

    // ---------------------------------------------------------------------
    // Construction, size, emptiness
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("New list is empty and size zero")
    void emptyOnConstruction() {
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals(-1, list.indexOf(999));
        assertFalse(list.contains(1));
    }

    // ---------------------------------------------------------------------
    // Append & prepend
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Append items grows list and preserves order")
    void appendItems() {
        list.append(1);
        list.append(2);
        list.append(3);

        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    @DisplayName("Prepend items grows list with reversed order")
    void prependItems() {
        list.prepend(3);
        list.prepend(2);
        list.prepend(1);

        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    @DisplayName("Mixing prepend and append maintains expected order")
    void mixedPrependAppend() {
        list.append(2);   // [2]
        list.prepend(1);  // [1,2]
        list.append(3);   // [1,2,3]

        assertEquals("[1, 2, 3]", list.toString());
    }

    @Test
    @DisplayName("Appending/prepending null is accepted")
    void allowNullValues() {
        list.append(null);
        list.prepend(1);

        assertEquals(2, list.size());
        assertNull(list.get(1));
        assertTrue(list.contains(null));
        assertEquals(1, list.indexOf(null));
    }

    // ---------------------------------------------------------------------
    // get() and indexOf()
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("get throws IndexOutOfBounds for bad indexes")
    void getInvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));

        list.append(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(5));
    }

    @Test
    @DisplayName("indexOf returns correct position or -1")
    void indexOfBehaviour() {
        IntStream.rangeClosed(1, 5).forEach(list::append);  // [1..5]

        assertEquals(0, list.indexOf(1));
        assertEquals(4, list.indexOf(5));
        assertEquals(-1, list.indexOf(42));
    }

    // ---------------------------------------------------------------------
    // deleteByValue
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("deleteByValue")
    class DeleteByValueTests {
        @BeforeEach
        void seed() {
            IntStream.rangeClosed(1, 5).forEach(list::append); // [1,2,3,4,5]
        }

        @Test
        @DisplayName("Deletes head element")
        void deleteHead() {
            list.deleteByValue(1);
            assertEquals(4, list.size());
            assertEquals(2, list.get(0));
            assertEquals(-1, list.indexOf(1));
        }

        @Test
        @DisplayName("Deletes tail element")
        void deleteTail() {
            list.deleteByValue(5);
            assertEquals(4, list.size());
            assertEquals(-1, list.indexOf(5));
            assertEquals(4, list.get(list.size() - 1)); // new tail
        }

        @Test
        @DisplayName("Deletes middle element")
        void deleteMiddle() {
            list.deleteByValue(3);
            assertEquals("[1, 2, 4, 5]", list.toString());
        }

        @Test
        @DisplayName("Deleting non‑existent value throws")
        void deleteMissing() {
            assertThrows(NoSuchElementException.class, () -> list.deleteByValue(99));
        }

        @Test
        @DisplayName("Deleting first occurrence when duplicates exist")
        void deleteFirstDuplicate() {
            list.append(3);              // duplicate 3 at tail
            list.deleteByValue(3);       // remove the first 3 (index 2)

            assertEquals(5, list.size());
            assertEquals(2, list.indexOf(4));
            assertEquals(4, list.indexOf(3)); // duplicate now at index 4
        }

        @Test
        @DisplayName("Deleting null when multiple nulls exist")
        void deleteNullValue() {
            list.append(null);
            list.prepend(null);          // [null,1..5,null]

            list.deleteByValue(null);    // remove the head null
            assertEquals(6, list.size());
            assertEquals(5, list.indexOf(null)); // tail null now at last index
        }
    }

    // ---------------------------------------------------------------------
    // deleteByIndex
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("deleteByIndex")
    class DeleteByIndexTests {

        @BeforeEach
        void seed() {
            IntStream.rangeClosed(1, 3).forEach(list::append); // [1,2,3]
        }

        @Test
        @DisplayName("Delete index 0 (head)")
        void deleteHeadIndex() {
            list.deleteByIndex(0);
            assertEquals("[2, 3]", list.toString());
        }

        @Test
        @DisplayName("Delete last index")
        void deleteTailIndex() {
            list.deleteByIndex(list.size() - 1);
            assertEquals("[1, 2]", list.toString());
        }

        @Test
        @DisplayName("Delete middle index")
        void deleteMiddleIndex() {
            list.deleteByIndex(1);
            assertEquals("[1, 3]", list.toString());
        }

        @Test
        @DisplayName("Invalid indexes throw IndexOutOfBounds")
        void deleteInvalidIndex() {
            assertThrows(IndexOutOfBoundsException.class, () -> list.deleteByIndex(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> list.deleteByIndex(3)); // size is 3
        }
    }

    // ---------------------------------------------------------------------
    // deleteFirst & deleteLast
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("deleteFirst and deleteLast on single‑element list empties it")
    void deleteFirstLastSingleElement() {
        list.append(10);

        list.deleteFirst();
        assertTrue(list.isEmpty());

        list.append(20);
        list.deleteLast();
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("deleteFirst & deleteLast on empty list throw")
    void deleteFirstLastOnEmpty() {
        assertThrows(NoSuchElementException.class, list::deleteFirst);
        assertThrows(NoSuchElementException.class, list::deleteLast);
    }

    // ---------------------------------------------------------------------
    // clear()
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("clear removes everything and resets size")
    void clearList() {
        IntStream.rangeClosed(1, 100).forEach(list::append);
        list.clear();

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    // ---------------------------------------------------------------------
    // contains()
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("contains reflects current membership")
    void containsMembership() {
        list.append(1);
        list.append(2);

        assertTrue(list.contains(1));
        assertFalse(list.contains(99));

        list.deleteByValue(1);
        assertFalse(list.contains(1));
    }
}
