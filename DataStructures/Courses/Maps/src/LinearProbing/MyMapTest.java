package LinearProbing;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Assumed baseline behavior for MyMap:
 *  - New map is empty with size 0.
 *  - put(k, v):
 *      • Inserts or overwrites the mapping for k.
 *      • If overwriting, size does NOT increase.
 *  - get(k): returns the mapped value or null if absent.
 *  - containsKey(k): true iff a mapping for k exists.
 *  - remove(k): removes mapping if present; otherwise no-op.
 *  - size()/isEmpty() reflect current number of mappings.
 *  - toString(): returns a non-null String; exact format not asserted.
 *
 * Optional (disabled) tests:
 *  - Support for null keys and/or null values (enable if your contract allows).
 *  - Mutable keys hazard (documented but typically undefined behavior).
 */
class MyMapTest {

    /* ----------------
     * Core happy paths
     * ---------------- */

    @Test
    void newMap_isEmpty_andSizeZero() {
        MyMap<String, Integer> map = new MyMap<>();
        assertTrue(map.isEmpty(), "New map should be empty");
        assertEquals(0, map.size(), "New map size should be 0");
    }

    @Test
    void put_thenGet_andContainsKey() {
        MyMap<String, String> map = new MyMap<>();
        map.put("a", "alpha");

        assertAll(
            () -> assertFalse(map.isEmpty(), "Map should not be empty after put"),
            () -> assertEquals(1, map.size(), "Size should be 1 after single put"),
            () -> assertTrue(map.containsKey("a"), "containsKey should recognize existing key"),
            () -> assertEquals("alpha", map.get("a"), "get should return the stored value"),
            () -> assertNull(map.get("missing"), "get on absent key should return null"),
            () -> assertFalse(map.containsKey("missing"), "containsKey should be false for absent key")
        );
    }

    @Test
    void put_overwritesExistingKey_andKeepsSize() {
        MyMap<Integer, String> map = new MyMap<>();
        map.put(1, "one");
        map.put(1, "uno"); // overwrite

        assertEquals(1, map.size(), "Overwriting same key should not change size");
        assertEquals("uno", map.get(1), "Value should be updated on overwrite");
    }

    @Test
    void multipleKeys_thenRemoveOne() {
        MyMap<Integer, String> map = new MyMap<>();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        assertEquals(3, map.size(), "Expected size after 3 inserts");

        map.remove(2);
        assertAll(
            () -> assertEquals(2, map.size(), "Size should decrease after removing existing key"),
            () -> assertFalse(map.containsKey(2), "Removed key should not be contained"),
            () -> assertNull(map.get(2), "get should return null for removed key"),
            () -> assertTrue(map.containsKey(1) && map.containsKey(3), "Other keys should remain")
        );
    }

    @Test
    void remove_onEmpty_isNoOp_andDoesNotThrow() {
        MyMap<String, Integer> map = new MyMap<>();
        assertDoesNotThrow(() -> map.remove("x"), "Removing from empty map should be a no-op");
        assertEquals(0, map.size(), "Size should remain 0");
        assertTrue(map.isEmpty(), "Map should remain empty");
    }

    @Test
    void remove_nonExistentKey_isNoOp() {
        MyMap<String, Integer> map = new MyMap<>();
        map.put("x", 10);

        assertEquals(1, map.size());
        assertDoesNotThrow(() -> map.remove("missing"), "Removing non-existent key should be a no-op");
        assertEquals(1, map.size(), "Size should be unchanged");
        assertTrue(map.containsKey("x"), "Existing key should remain");
        assertEquals(10, map.get("x"));
    }

    @Test
    void remove_sameKeyTwice_secondIsNoOp() {
        MyMap<String, Integer> map = new MyMap<>();
        map.put("k", 1);
        map.remove("k");
        assertEquals(0, map.size());
        assertDoesNotThrow(() -> map.remove("k"), "Second remove should be a no-op");
        assertFalse(map.containsKey("k"));
        assertNull(map.get("k"));
    }

    @Test
    void duplicatePuts_doNotIncreaseSize_butUpdateValue() {
        MyMap<String, Integer> map = new MyMap<>();
        map.put("k", 1);
        map.put("k", 2);
        map.put("k", 3);

        assertEquals(1, map.size(), "Only one unique key should be counted");
        assertEquals(3, map.get("k"), "Latest value should be retained");
    }

    @Test
    void manyInserts_sizeCountsUniqueKeys_andOverwritesWork() {
        MyMap<Integer, Integer> map = new MyMap<>();
        for (int i = 0; i < 100; i++) {
            map.put(i, i * i);
        }
        // Overwrite half of them
        for (int i = 0; i < 50; i++) {
            map.put(i, -i);
        }

        assertEquals(100, map.size(), "Size should reflect unique keys only");
        assertEquals(-10, map.get(10), "Overwrite should be visible");
        assertEquals(51 * 51, map.get(51), "Unchanged key should retain original value");
    }

    @Test
    void isEmpty_reflectsStateAfterOperations() {
        MyMap<String, String> map = new MyMap<>();
        assertTrue(map.isEmpty());

        map.put("a", "1");
        assertFalse(map.isEmpty());

        map.remove("a");
        assertTrue(map.isEmpty());
    }

    @Test
    void toString_isNonNullAndNonEmpty_forNonEmptyMap() {
        MyMap<String, String> map = new MyMap<>();
        map.put("a", "1");
        map.put("b", "2");
        String s = map.toString();
        assertNotNull(s, "toString should not return null");
        assertFalse(s.isEmpty(), "toString should not be empty");
    }

    /* -----------
     * Edge cases
     * ----------- */

    @Test
    void toString_onEmpty_isNonNull() {
        MyMap<String, String> map = new MyMap<>();
        String s = map.toString();
        assertNotNull(s, "toString on empty should not be null");
        // format free — may or may not be empty; we don't assert emptiness here
    }

    @Test
    void reinsertingRemovedKey_increasesSizeAgain() {
        MyMap<String, Integer> map = new MyMap<>();
        map.put("x", 1);
        map.remove("x");
        assertEquals(0, map.size());
        map.put("x", 2);
        assertAll(
            () -> assertEquals(1, map.size(), "Reinserting after removal should add back the key"),
            () -> assertTrue(map.containsKey("x")),
            () -> assertEquals(2, map.get("x"))
        );
    }

    @Test
    void overwriteThenRemove_isFullyGone() {
        MyMap<String, String> map = new MyMap<>();
        map.put("k", "v1");
        map.put("k", "v2"); // overwrite
        map.remove("k");
        assertAll(
            () -> assertFalse(map.containsKey("k")),
            () -> assertNull(map.get("k")),
            () -> assertEquals(0, map.size())
        );
    }

    @Test
    void alternatingPutRemove_stability() {
        MyMap<Integer, Integer> map = new MyMap<>();
        for (int i = 0; i < 200; i++) {
            map.put(i, i);
            map.remove(i);
        }
        assertTrue(map.isEmpty(), "Map should be empty after alternating put/remove");
        assertEquals(0, map.size());
    }

    @Test
    void duplicateValuesDifferentKeys_areIndependent() {
        MyMap<String, String> map = new MyMap<>();
        map.put("a", "same");
        map.put("b", "same");

        assertAll(
            () -> assertEquals(2, map.size()),
            () -> assertEquals("same", map.get("a")),
            () -> assertEquals("same", map.get("b"))
        );

        map.remove("a");
        assertAll(
            () -> assertFalse(map.containsKey("a")),
            () -> assertTrue(map.containsKey("b")),
            () -> assertEquals(1, map.size())
        );
    }

    @Test
    void largeKeysAndValues_shouldWork() {
        MyMap<String, String> map = new MyMap<>();
        String bigKey = "k".repeat(10_000);
        String bigVal = "v".repeat(20_000);

        map.put(bigKey, bigVal);
        assertAll(
            () -> assertEquals(1, map.size()),
            () -> assertTrue(map.containsKey(bigKey)),
            () -> assertEquals(bigVal, map.get(bigKey))
        );
    }

    @Test
    void collisionHeavyKeys_allDistinctKeysStillWork() {
        MyMap<BadHashKey, String> map = new MyMap<>();
        int n = 200;
        List<BadHashKey> keys = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BadHashKey k = new BadHashKey(i);
            keys.add(k);
            map.put(k, "v" + i);
        }
        assertEquals(n, map.size(), "All distinct keys must be stored even if hashes collide");

        for (int i = 0; i < n; i++) {
            assertTrue(map.containsKey(keys.get(i)), "containsKey should work under collisions");
            assertEquals("v" + i, map.get(keys.get(i)), "get should return correct value under collisions");
        }

        // Remove every second key
        for (int i = 0; i < n; i += 2) {
            map.remove(keys.get(i));
        }
        assertEquals(n / 2, map.size(), "Size should reflect removals under collisions");

        for (int i = 1; i < n; i += 2) {
            assertTrue(map.containsKey(keys.get(i)));
        }
        for (int i = 0; i < n; i += 2) {
            assertFalse(map.containsKey(keys.get(i)));
        }
    }

    @Test
    void removingAllKeys_resultsInEmptyMap() {
        MyMap<Integer, Integer> map = new MyMap<>();
        for (int i = 0; i < 50; i++) map.put(i, i);
        for (int i = 0; i < 50; i++) map.remove(i);

        assertAll(
            () -> assertTrue(map.isEmpty(), "Map should be empty after removing everything"),
            () -> assertEquals(0, map.size())
        );
    }

    /* ---------------------
     * Helper test fixtures
     * --------------------- */

    /** Key with constant hashCode to force collisions, but proper equals on id. */
    static final class BadHashKey {
        final int id;
        BadHashKey(int id) { this.id = id; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BadHashKey)) return false;
            return id == ((BadHashKey) o).id;
        }
        @Override public int hashCode() { return 42; } // constant on purpose
        @Override public String toString() { return "K(" + id + ")"; }
    }

    /** Mutable key (do NOT use in real maps) for demonstration purposes. */
    static final class MutableKey {
        private int id;
        MutableKey(int id) { this.id = id; }
        void setId(int id) { this.id = id; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MutableKey)) return false;
            return id == ((MutableKey) o).id;
        }
        @Override public int hashCode() { return Objects.hash(id); }
        @Override public String toString() { return "MK(" + id + ")"; }
    }
}

/* ==========================================================
 * MyEntry tests — extended edge cases (nulls, large values)
 * ========================================================== */

class MyEntryTest {

    @Test
    void constructor_andGetters() {
        MyEntry<String, Integer> e = new MyEntry<>("key", 42);
        assertEquals("key", e.getKey());
        assertEquals(42, e.getValue());
    }

    @Test
    void setValue_updatesValueOnly() {
        MyEntry<Integer, String> e = new MyEntry<>(7, "seven");
        e.setValue("SEVEN");
        assertEquals("SEVEN", e.getValue(), "Value should update");
        assertEquals(7, e.getKey(), "Key should remain unchanged");
    }

    @Test
    void toString_isNonNullAndNonEmpty() {
        MyEntry<String, String> e = new MyEntry<>("k", "v");
        String s = e.toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    @Test
    void veryLargeKeyAndValue_shouldBeHeld() {
        String bigKey = "k".repeat(5000);
        String bigVal = "v".repeat(8000);
        MyEntry<String, String> e = new MyEntry<>(bigKey, bigVal);
        assertEquals(bigKey, e.getKey());
        assertEquals(bigVal, e.getValue());
    }

    @Test
    @Disabled("Enable if MyEntry supports null values")
    void nullValue_supported() {
        MyEntry<String, String> e = new MyEntry<>("k", null);
        assertNull(e.getValue());
        e.setValue("now-not-null");
        assertEquals("now-not-null", e.getValue());
    }

    @Test
    @Disabled("Enable if MyEntry supports null keys")
    void nullKey_supported() {
        MyEntry<String, Integer> e = new MyEntry<>(null, 1);
        assertNull(e.getKey());
        assertEquals(1, e.getValue());
    }
}