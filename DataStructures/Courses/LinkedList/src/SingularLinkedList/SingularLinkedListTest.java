package SingularLinkedList;

import org.junit.jupiter.api.*;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SingularLinkedList<Integer> comprehensive tests")
class SingularLinkedListTest {

    private SingularLinkedList<Integer> list;

    @BeforeEach
    void setUp() {
        list = new SingularLinkedList<>();
    }

    // ---------- Constructor / basic state ----------

    @Test
    void constructor_initialState() {
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("null", list.toString(), "Empty list should print \"null\"");
    }

    // ---------- append ----------

    @Test
    @DisplayName("append on empty list inserts the first element")
    void append_onEmpty() {
        list.append(10);
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(10, list.get(0));
    }

    @Test
    @DisplayName("append multiple preserves order and grows size")
    void append_multiple() {
        list.append(1);
        list.append(2);
        list.append(3);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    // ---------- prepend ----------

    @Test
    @DisplayName("prepend on empty list inserts the first element at head")
    void prepend_onEmpty() {
        list.prepend(42);
        assertEquals(1, list.size());
        assertEquals(42, list.get(0));
    }

    @Test
    @DisplayName("prepend multiple pushes elements to the head (reverse order)")
    void prepend_multiple() {
        list.prepend(3);
        list.prepend(2);
        list.prepend(1);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    @DisplayName("mixing prepend and append keeps correct order")
    void prepend_append_mix() {
        list.append(2);      // [2]
        list.prepend(1);     // [1,2]
        list.append(3);      // [1,2,3]
        list.prepend(0);     // [0,1,2,3]
        assertAll(
                () -> assertEquals(4, list.size()),
                () -> assertEquals(0, list.get(0)),
                () -> assertEquals(1, list.get(1)),
                () -> assertEquals(2, list.get(2)),
                () -> assertEquals(3, list.get(3))
        );
    }

    // ---------- deleteByValue ----------

    @Test
    @DisplayName("deleteByValue removes head when value is at head")
    void deleteByValue_head() {
        populate(1, 2, 3);
        list.deleteByValue(1);
        assertEquals(2, list.size());
        assertEquals(2, list.get(0));
    }

    @Test
    @DisplayName("deleteByValue removes tail when value is at tail")
    void deleteByValue_tail() {
        populate(1, 2, 3);
        list.deleteByValue(3);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
    }

    @Test
    @DisplayName("deleteByValue removes middle element")
    void deleteByValue_middle() {
        populate(1, 2, 3, 4);
        list.deleteByValue(3);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(4, list.get(2));
    }

    @Test
    @DisplayName("deleteByValue removes only the first occurrence among duplicates")
    void deleteByValue_firstOfDuplicates() {
        populate(1, 2, 2, 2, 3);
        list.deleteByValue(2);
        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1)); // the second '2'
        assertEquals(2, list.get(2));
        assertEquals(3, list.get(3));
    }

    @Test
    @DisplayName("deleteByValue on value not found does nothing")
    void deleteByValue_notFound() {
        populate(1, 2, 3);
        list.deleteByValue(99);
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("deleteByValue on empty list does nothing (no throw)")
    void deleteByValue_onEmpty() {
        assertDoesNotThrow(() -> list.deleteByValue(1));
        assertEquals(0, list.size());
    }

    // ---------- deleteByIndex ----------

    @Test
    @DisplayName("deleteByIndex removes head (index 0)")
    void deleteByIndex_head() {
        populate(1, 2, 3);
        list.deleteByIndex(0);
        assertEquals(2, list.size());
        assertEquals(2, list.get(0));
    }

    @Test
    @DisplayName("deleteByIndex removes middle")
    void deleteByIndex_middle() {
        populate(1, 2, 3, 4);
        list.deleteByIndex(2); // remove 3
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(4, list.get(2));
    }

    @Test
    @DisplayName("deleteByIndex removes last")
    void deleteByIndex_last() {
        populate(1, 2, 3);
        list.deleteByIndex(2);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
    }

    @Test
    @DisplayName("deleteByIndex with negative index throws")
    void deleteByIndex_negativeThrows() {
        populate(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteByIndex(-1));
    }

    @Test
    @DisplayName("deleteByIndex >= size throws")
    void deleteByIndex_outOfRangeThrows() {
        populate(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteByIndex(3));
    }

    @Test
    @DisplayName("deleteByIndex on empty throws")
    void deleteByIndex_onEmptyThrows() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteByIndex(0));
    }

    // ---------- deleteFirst ----------

    @Test
    @DisplayName("deleteFirst removes the head")
    void deleteFirst_normal() {
        populate(10, 20, 30);
        list.deleteFirst();
        assertEquals(2, list.size());
        assertEquals(20, list.get(0));
    }

    @Test
    @DisplayName("deleteFirst on single element list empties it")
    void deleteFirst_singleElement() {
        populate(10);
        list.deleteFirst();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    @DisplayName("deleteFirst on empty list throws")
    void deleteFirst_onEmptyThrows() {
        assertThrows(NoSuchElementException.class, () -> list.deleteFirst());
    }

    // ---------- deleteLast ----------

    @Test
    @DisplayName("deleteLast removes the last element")
    void deleteLast_normal() {
        populate(10, 20, 30);
        list.deleteLast();
        assertEquals(2, list.size());
        assertEquals(20, list.get(1));
    }

    @Test
    @DisplayName("deleteLast on single element list empties it")
    void deleteLast_singleElement() {
        populate(10);
        list.deleteLast();
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("deleteLast on empty list throws")
    void deleteLast_onEmptyThrows() {
        assertThrows(NoSuchElementException.class, () -> list.deleteLast());
    }

    // ---------- size & isEmpty ----------

    @Test
    @DisplayName("size() reflects number of elements, isEmpty() toggles correctly")
    void size_isEmpty_behaviour() {
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());

        list.append(1);
        assertEquals(1, list.size());
        assertFalse(list.isEmpty());

        list.deleteFirst();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    // ---------- contains ----------

    @Test
    @DisplayName("contains returns true for present values and false otherwise")
    void contains_basic() {
        populate(1, 2, 3);
        assertTrue(list.contains(1));
        assertTrue(list.contains(2));
        assertTrue(list.contains(3));
        assertFalse(list.contains(99));
    }

    // ---------- clear ----------

    @Test
    @DisplayName("clear empties list and is idempotent")
    void clear_basic() {
        populate(1, 2, 3);
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());

        // idempotent
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    // ---------- indexOf ----------

    @Test
    @DisplayName("indexOf returns index of first occurrence")
    void indexOf_firstOccurrence() {
        populate(1, 2, 2, 3);
        assertEquals(1, list.indexOf(2));
    }

    @Test
    @DisplayName("indexOf returns -1 for not-found value")
    void indexOf_notFound() {
        populate(1, 2, 3);
        assertEquals(-1, list.indexOf(99));
    }

    // ------------------------- get -------------------------
    @Test
    @DisplayName("get retrieves elements by index")
    void get_valid() {
        populate(10, 20, 30);
        assertEquals(Integer.valueOf(10), list.get(0));
        assertEquals(Integer.valueOf(20), list.get(1));
        assertEquals(Integer.valueOf(30), list.get(2));
    }

    @Test
    @DisplayName("get throws IndexOutOfBoundsException for invalid indices")
    void get_negativeThrows() {
        populate(10, 20);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    @DisplayName("get throws IndexOutOfBoundsException for out-of-range indices")
    void get_outOfRangeThrows() {
        populate(10, 20);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
    }

    // ---------- toString ----------

    @Test
    @DisplayName("toString formats elements in order with ' -> ' and ends with 'null'")
    void toString_format() {
        populate(1, 2, 3);

        String s = list.toString();              // "1 -> 2 -> 3 -> null"
        assertTrue(s.endsWith("null"));

        // split and verify ordering + terminator
        String[] parts = s.split(" -> ");
        assertArrayEquals(new String[] { "1", "2", "3", "null" }, parts,
                "Elements and terminator should appear in sequence separated by ' -> '");
    }

    // ---------- Helpers ----------

    private void populate(Integer... values) {
        for (Integer v : values) {
            list.append(v);
        }
    }
}
