package QuadrathicProbing;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended edge-case tests for MyMap (quadratic probing) and MyEntry.
 *
 * Policy toggles — flip these to match your implementation:
 *  - ALLOW_NULL_KEYS:     true if put/get/remove with null keys are allowed.
 *  - ALLOW_NULL_VALUES:   true if null values are allowed.
 *  - RESIZE_SHRINK_BELOW_SIZE_THROWS: true if resize(newCapacity < size()) should throw.
 */
public class MyMapEdgeTests {

    // ===== Policy toggles (adjust to match your code) =====
    private static final boolean ALLOW_NULL_KEYS = false;
    private static final boolean ALLOW_NULL_VALUES = true;
    private static final boolean RESIZE_SHRINK_BELOW_SIZE_THROWS = true;

    // ---------- Helper key types ----------

    /** Forces collisions by returning a fixed hashCode but distinct equals by id. */
    private static final class BadHashKey {
        private final int id;
        private final int fixedHash;
        BadHashKey(int id, int fixedHash) { this.id = id; this.fixedHash = fixedHash; }
        @Override public int hashCode() { return fixedHash; }
        @Override public boolean equals(Object o) {
            return (o instanceof BadHashKey) && ((BadHashKey)o).id == id;
        }
        @Override public String toString() { return "K" + id; }
    }

    /** Always negative hashCode to test index normalization. */
    private static final class NegativeHashKey {
        private final int id;
        NegativeHashKey(int id) { this.id = id; }
        @Override public int hashCode() { return -31 - id; }
        @Override public boolean equals(Object o) {
            return (o instanceof NegativeHashKey) && ((NegativeHashKey)o).id == id;
        }
    }

    // ---------- MyEntry edge tests ----------
    @Nested
    class MyEntryEdges {
        @Test
        void deletedThenOccupied_statusTransitionsAreStable() {
            MyEntry<String, Integer> e = new MyEntry<>("a", 1);
            e.setDeleted();
            assertEquals(SlotStatus.DELETED, e.getStatus());
            e.setOccupied();
            assertEquals(SlotStatus.OCCUPIED, e.getStatus());
        }

        @Test
        void toString_nonEmpty_notNullAndContainsPieces() {
            MyEntry<String, Integer> e = new MyEntry<>("key", 123);
            String s = e.toString();
            assertNotNull(s);
            assertTrue(s.contains("key") && s.contains("123"));
        }
    }

    // ---------- Null policy (configurable) ----------
    @Nested
    class NullPolicy {
        @Test
        void put_nullKey_policy() {
            MyMap<String, Integer> map = new MyMap<>();
            if (ALLOW_NULL_KEYS) {
                map.put(null, 7);
                assertTrue(map.containsKey(null));
                assertEquals(7, map.get(null));
            } else {
                assertThrows(NullPointerException.class, () -> map.put(null, 7));
            }
        }

        @Test
        void get_nullKey_policy() {
            MyMap<String, Integer> map = new MyMap<>();
            if (ALLOW_NULL_KEYS) {
                assertNull(map.get(null)); // absent
                map.put(null, 1);
                assertEquals(1, map.get(null));
            } else {
                assertThrows(NullPointerException.class, () -> map.get(null));
            }
        }

        @Test
        void remove_nullKey_policy() {
            MyMap<String, Integer> map = new MyMap<>();
            if (ALLOW_NULL_KEYS) {
                assertNull(map.remove(null)); // absent
                map.put(null, 2);
                assertEquals(2, map.remove(null));
            } else {
                assertThrows(NullPointerException.class, () -> map.remove(null));
            }
        }

        @Test
        void put_nullValue_policy() {
            MyMap<String, Integer> map = new MyMap<>();
            if (ALLOW_NULL_VALUES) {
                map.put("k", null);
                assertTrue(map.containsKey("k"));
                assertNull(map.get("k"));
                assertEquals(1, map.size());
            } else {
                assertThrows(NullPointerException.class, () -> map.put("k", null));
            }
        }
    }

    // ---------- Collision, probing, tombstones ----------
    @Nested
    class ProbingEdges {

        @Test
        void largeCollisionCluster_wrapAroundAndFindability() {
            MyMap<BadHashKey, String> map = new MyMap<>();
            int fixedHash = 999; // all collide initially

            // Insert fewer than initial capacity to avoid guaranteed overflow without resize.
            // If your implementation auto-resizes, this still passes.
            List<BadHashKey> keys = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                keys.add(new BadHashKey(i, fixedHash));
            }
            for (int i = 0; i < keys.size(); i++) {
                map.put(keys.get(i), "v" + i);
            }
            assertEquals(12, map.size());

            // Remove some "middle" ones to create tombstones
            assertEquals("v3", map.remove(keys.get(3)));
            assertEquals("v7", map.remove(keys.get(7)));
            assertEquals(10, map.size());

            // Verify others are still findable across tombstones
            for (int i = 0; i < keys.size(); i++) {
                if (i == 3 || i == 7) continue;
                assertEquals("v" + i, map.get(keys.get(i)));
            }

            // Reinsert new colliding keys; should reuse tombstones or probe further
            BadHashKey kA = new BadHashKey(100, fixedHash);
            BadHashKey kB = new BadHashKey(101, fixedHash);
            map.put(kA, "A");
            map.put(kB, "B");
            assertEquals(12, map.size());
            assertEquals("A", map.get(kA));
            assertEquals("B", map.get(kB));
        }

        @Test
        void overwriteAfterDelete_sameLogicalKeyShouldReplaceNotGrow() {
            MyMap<String, Integer> map = new MyMap<>();
            map.put("x", 1);
            assertEquals(1, map.size());
            assertEquals(1, map.remove("x"));
            assertEquals(0, map.size());

            // Re-insert "x" (same key) — size becomes 1 again (not 2)
            map.put("x", 2);
            assertEquals(1, map.size());
            assertEquals(2, map.get("x"));
        }

        @Test
        void removeTwice_secondReturnIsNull() {
            MyMap<Integer, String> map = new MyMap<>();
            map.put(5, "five");
            assertEquals("five", map.remove(5));
            assertNull(map.remove(5));
        }

        @Test
        void negativeHashCodes_supported() {
            MyMap<NegativeHashKey, String> map = new MyMap<>();
            NegativeHashKey a = new NegativeHashKey(1);
            NegativeHashKey b = new NegativeHashKey(2);
            map.put(a, "A");
            map.put(b, "B");
            assertEquals("A", map.get(a));
            assertEquals("B", map.get(b));
            assertTrue(map.containsKey(a));
            assertTrue(map.containsKey(b));
        }
    }

    // ---------- Resizing edge cases ----------
    @Nested
    class ResizeEdges {

        @Test
        void shrinkingAndGrowing_preservesAllEntries() {
            MyMap<Integer, Integer> map = new MyMap<>();
            for (int i = 0; i < 40; i++) map.put(i, i*i);

            // Safe shrink (to >= size) OR skip if your resize forbids equal-to-size (you can adjust).
            int safeCapacity = 64;
            map.resize(safeCapacity);
            for (int i = 0; i < 40; i++) assertEquals(i*i, map.get(i));

            // Grow further
            map.resize(256);
            for (int i = 0; i < 40; i++) assertEquals(i*i, map.get(i));

            // Overwrite a few and ensure size doesn’t change
            int before = map.size();
            map.put(5, -1);
            map.put(17, -2);
            assertEquals(-1, map.get(5));
            assertEquals(-2, map.get(17));
            assertEquals(before, map.size());
        }
    }

    // ---------- Hash function sanity (edge-ish) ----------
    @Nested
    class HashSanity {
        @Test
        void hash_isDeterministicAndVariesForSomeIterators() {
            MyMap<String, Integer> map = new MyMap<>();
            String key = "alpha";
            int h0 = map.hash(key, 0);
            int h0b = map.hash(key, 0);
            assertEquals(h0, h0b, "determinism required");
            Set<Integer> seen = new HashSet<>();
            for (int i = 0; i < 12; i++) seen.add(map.hash(key, i));
            assertTrue(seen.size() >= 2, "probing should vary across i at least sometimes");
        }

        @Test
        void hash_handlesLargeIteratorValues() {
            MyMap<String, Integer> map = new MyMap<>();
            String key = "beta";
            // Very large i should not cause exceptions or negative indices after normalization
            int hBig = map.hash(key, Integer.MAX_VALUE / 2);
            int hBigger = map.hash(key, Integer.MAX_VALUE);
            // We can’t assert modulo capacity without accessing table size, but we can at least ensure determinism
            assertEquals(hBig, map.hash(key, Integer.MAX_VALUE / 2));
            assertEquals(hBigger, map.hash(key, Integer.MAX_VALUE));
        }
    }

    // ---------- Stress / fuzz vs HashMap ----------
    @Nested
    class FuzzSmall {
        @Test
        void randomOps_compareAgainstHashMap_basicsOnly() {
            MyMap<Integer, String> map = new MyMap<>();
            Map<Integer, String> ref = new HashMap<>();
            Random rnd = new Random(12345);

            for (int step = 0; step < 1000; step++) {
                int op = rnd.nextInt(3); // 0=put,1=remove,2=get
                int k = rnd.nextInt(200);
                switch (op) {
                    case 0: {
                        String v = "v" + rnd.nextInt(1000);
                        map.put(k, v);
                        ref.put(k, v);
                        break;
                    }
                    case 1: {
                        String got = map.remove(k);
                        String refGot = ref.remove(k);
                        assertEquals(refGot, got, "remove() return mismatch at k=" + k);
                        break;
                    }
                    case 2: {
                        String got = map.get(k);
                        String refGot = ref.get(k);
                        assertEquals(refGot, got, "get() value mismatch at k=" + k);
                        break;
                    }
                }
            }

            // Final consistency: size and membership for all keys touched
            assertEquals(ref.size(), map.size(), "size() mismatch after fuzz");
            for (int k = 0; k < 200; k++) {
                assertEquals(ref.containsKey(k), map.containsKey(k), "containsKey() mismatch at k=" + k);
                assertEquals(ref.get(k), map.get(k), "final get() mismatch at k=" + k);
            }
        }
    }

    // ---------- Basic idempotency & emptiness checks ----------
    @Nested
    class IdempotencyAndEmpty {
        @Test
        void removeFromEmpty_isSafe() {
            MyMap<String, Integer> map = new MyMap<>();
            assertTrue(map.isEmpty());
            assertNull(map.remove("x"));
            assertTrue(map.isEmpty());
            assertEquals(0, map.size());
        }

        @Test
        void putSameKeyManyTimes_sizeStaysOne() {
            MyMap<String, Integer> map = new MyMap<>();
            for (int i = 0; i < 50; i++) map.put("dup", i);
            assertEquals(1, map.size());
            assertEquals(49, map.get("dup"));
        }

        @Test
        void containsKeyReflectsRemovals() {
            MyMap<Integer, Integer> map = new MyMap<>();
            map.put(1, 1);
            map.put(2, 2);
            assertTrue(map.containsKey(1));
            assertTrue(map.containsKey(2));
            map.remove(1);
            assertFalse(map.containsKey(1));
            assertTrue(map.containsKey(2));
            map.remove(2);
            assertFalse(map.containsKey(2));
            assertTrue(map.isEmpty());
        }
    }
}