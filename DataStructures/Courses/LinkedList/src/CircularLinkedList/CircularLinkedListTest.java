package CircularLinkedList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class CircularLinkedListTest {

    private CircularLinkedList<Integer> list;

    @BeforeEach
    void setUp() {
        list = new CircularLinkedList<>();
    }

    /* ------------------------------------------------------------------
     *  Basic state helpers
     * ------------------------------------------------------------------ */
    @Test
    void newListIsEmpty() {
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    /* ------------------------------------------------------------------
     *  append()
     * ------------------------------------------------------------------ */
    @Test
    void appendToEmptyCreatesSingleElement() {
        list.append(10);
        assertEquals(1, list.size());
        assertFalse(list.isEmpty());
        assertEquals(10, list.get(0));
    }

    @Test
    void appendAddsToTail() {
        list.append(1);
        list.append(2);
        list.append(3);
        assertEquals(3, list.size());
        assertEquals(3, list.get(2));
    }

    /* ------------------------------------------------------------------
     *  prepend()
     * ------------------------------------------------------------------ */
    @Test
    void prependToEmptyCreatesSingleElement() {
        list.prepend(99);
        assertEquals(1, list.size());
        assertEquals(99, list.get(0));
    }

    @Test
    void prependAddsToHead() {
        list.append(2);
        list.prepend(1);
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(2, list.size());
    }

    /* ------------------------------------------------------------------
     *  deleteFirst() & deleteLast()
     * ------------------------------------------------------------------ */
    @Test
    void deleteFirstOnSingleElementLeavesEmpty() {
        list.append(5);
        list.deleteFirst();
        assertTrue(list.isEmpty());
    }

    @Test
    void deleteLastOnSingleElementLeavesEmpty() {
        list.append(5);
        list.deleteLast();
        assertTrue(list.isEmpty());
    }

    @Test
    void deleteFirstRemovesHead() {
        list.append(1);
        list.append(2);
        list.append(3);
        list.deleteFirst();
        assertEquals(2, list.get(0));
        assertEquals(2, list.size());
    }

    @Test
    void deleteLastRemovesTail() {
        list.append(1);
        list.append(2);
        list.append(3);
        list.deleteLast();
        assertEquals(2, list.size());
        assertEquals(2, list.get(1));
    }

    @Test
    void deleteFirstOnEmptyThrows() {
        assertThrows(NoSuchElementException.class, () -> list.deleteFirst());
    }

    @Test
    void deleteLastOnEmptyThrows() {
        assertThrows(NoSuchElementException.class, () -> list.deleteLast());
    }

    /* ------------------------------------------------------------------
     *  deleteByValue()
     * ------------------------------------------------------------------ */
    @Test
    void deleteByValueRemovesHead() {
        list.append(1);
        list.append(2);
        list.append(3);
        list.deleteByValue(1);
        assertEquals(0, list.indexOf(2));
        assertEquals(2, list.size());
    }

    @Test
    void deleteByValueRemovesMiddle() {
        list.append(1);
        list.append(2);
        list.append(3);
        list.deleteByValue(2);
        assertEquals(1, list.indexOf(3));
        assertEquals(2, list.size());
    }

    @Test
    void deleteByValueRemovesTail() {
        list.append(1);
        list.append(2);
        list.deleteByValue(2);
        assertEquals(1, list.size());
        assertFalse(list.contains(2));
    }

    @Test
    void deleteByValueFirstOccurrenceOnly() {
        list.append(1);
        list.append(1);
        list.append(1);          // three duplicates
        list.deleteByValue(1);   // delete first
        assertEquals(2, list.size());
        assertEquals(0, list.indexOf(1)); // still present
    }

    @Test
    void deleteByValueMissingThrows() {
        list.append(1);
        assertThrows(NoSuchElementException.class, () -> list.deleteByValue(99));
    }

    /* ------------------------------------------------------------------
     *  deleteByIndex()
     * ------------------------------------------------------------------ */
    @Test
    void deleteByIndexRemovesCorrectElement() {
        list.append(1);
        list.append(2);
        list.append(3);

        list.deleteByIndex(1);   // remove the middle value (2)

        assertEquals(2, list.size());
        assertEquals(3, list.get(1));
    }

    @Test
    void deleteByIndexHead() {
        list.append(7);
        list.append(8);
        list.deleteByIndex(0);
        assertEquals(8, list.get(0));
    }

    @Test
    void deleteByIndexTail() {
        list.append(7);
        list.append(8);
        list.deleteByIndex(1);
        assertEquals(1, list.size());
        assertEquals(7, list.get(0));
    }

    @Test
    void deleteByIndexNegativeThrows() {
        list.append(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteByIndex(-1));
    }

    @Test
    void deleteByIndexTooLargeThrows() {
        list.append(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteByIndex(2));
    }

    /* ------------------------------------------------------------------
     *  size(), contains(), indexOf(), get()
     * ------------------------------------------------------------------ */
    @Test
    void indexOfReturnsMinusOneWhenAbsent() {
        list.append(1);
        assertEquals(-1, list.indexOf(99));
    }

    @Test
    void containsReturnsTrueWhenPresent() {
        list.append(1);
        assertTrue(list.contains(1));
    }

    @Test
    void containsReturnsFalseWhenAbsent() {
        assertFalse(list.contains(1));
    }

    @Test
    void getReturnsElementAtIndex() {
        list.append(1);
        list.append(2);
        list.append(3);
        assertEquals(2, list.get(1));
    }

    @Test
    void getNegativeThrows() {
        list.append(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    void getTooLargeThrows() {
        list.append(1);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
    }

    /* ------------------------------------------------------------------
     *  clear()
     * ------------------------------------------------------------------ */
    @Test
    void clearEmptiesList() {
        list.append(1);
        list.append(2);
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    /* ------------------------------------------------------------------
     *  toString()
     * ------------------------------------------------------------------ */
    @Test
    void toStringIncludesElements() {
        list.append(1);
        list.append(2);

        String result = list.toString();

        assertNotNull(result);
        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
    }
}
