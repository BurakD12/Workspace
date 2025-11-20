package Chaining;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge-case tests for MyMap<K,V> and MyEntry<K,V>.
 *
 * Notes:
 * • Null handling is unspecified by the signatures. These tests *adapt*:
 * - If put/get/containsKey/remove with null throws (NPE/IAE), we assert the
 * throw.
 * - If it doesn't throw, we assert consistent Map-like behavior.
 * • No assumptions about toString() format; we only check non-null and
 * stability for
 * the same state.
 */
class MyMapAndMyEntryEdgeCasesTest {

    /* ---------- Helpers ---------- */

    /**
     * Key class with identity field; distinct instances compare equal if id
     * matches.
     */
    private static final class EqKey {
        final int id;

        EqKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (this == o) || (o instanceof EqKey k && k.id == this.id);
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        @Override
        public String toString() {
            return "EqKey(" + id + ")";
        }
    }

    /**
     * Convenience: expect either NPE/IAE or success; returns true if exception
     * thrown.
     */
    private static boolean expectNullRejected(Runnable r) {
        try {
            r.run();
            return false; // no exception: null accepted
        } catch (RuntimeException ex) {
            assertTrue(ex instanceof NullPointerException || ex instanceof IllegalArgumentException,
                    "If null is rejected, it should be via NPE or IAE; got: " + ex);
            return true; // null rejected
        }
    }

    /* ---------- MyMap tests ---------- */
    @Nested
    @DisplayName("MyMap<K,V> edge cases")
    class MyMapEdgeCases {

        @Test
        @DisplayName("Remove on empty map should not reduce size below zero")
        void remove_onEmpty_isSafe() {
            MyMap<String, Integer> map = new MyMap<>();
            assertEquals(0, map.size());
            // Removing an absent key: either throws (if API requires) or is a no-op.
            boolean threw = expectNullRejected(() -> map.remove(null)); // try null first
            if (!threw) {
                // If null allowed, size must remain 0 and map empty.
                assertEquals(0, map.size());
                assertTrue(map.isEmpty());
            }
            // Remove a non-null absent key should not corrupt state
            try {
                map.remove("missing");
                assertEquals(0, map.size());
                assertTrue(map.isEmpty());
            } catch (RuntimeException ex) {
                // If implementation throws for absent keys, assert type/size stability
                assertTrue(ex instanceof IllegalArgumentException || ex instanceof NullPointerException);
                assertEquals(0, map.size());
            }
        }

        @Test
        @DisplayName("Null key handling (put/get/contains/remove) is consistent")
        void nullKey_policyIsConsistent() {
            MyMap<String, String> strMap = new MyMap<>();
            int before = strMap.size();

            boolean rejectedOnPut = expectNullRejected(() -> strMap.put(null, "X"));
            if (rejectedOnPut) {
                assertEquals(before, strMap.size(), "Size must be unchanged if null key rejected");
                // get/contains/remove with null should also be consistent (likely throw)
                assertTrue(expectNullRejected(() -> strMap.get(null)));
                assertTrue(expectNullRejected(() -> strMap.containsKey(null)));
                assertTrue(expectNullRejected(() -> strMap.remove(null)));
            } else {
                // Null key accepted -> must be retrievable and tracked by containsKey
                assertTrue(strMap.containsKey(null));
                assertEquals("X", strMap.get(null));
                assertEquals(before + 1, strMap.size());
                // Overwrite null-key value should not increase size
                strMap.put(null, "Y");
                assertEquals("Y", strMap.get(null));
                assertEquals(before + 1, strMap.size());
                // Removing null key should make it absent
                strMap.remove(null);
                assertFalse(strMap.containsKey(null));
                assertNull(strMap.get(null));
            }
        }

        @Test
        @DisplayName("Null value handling (store/retrieve/overwrite) is consistent")
        void nullValue_policyIsConsistent() {
            MyMap<String, String> map = new MyMap<>();
            int before = map.size();

            boolean rejected = expectNullRejected(() -> map.put("k", null));
            if (rejected) {
                assertEquals(before, map.size(), "Size must not change if null value rejected");
            } else {
                assertTrue(map.containsKey("k"));
                assertNull(map.get("k"));
                assertEquals(before + 1, map.size());
                // Overwrite null -> non-null
                map.put("k", "v");
                assertEquals("v", map.get("k"));
                assertEquals(before + 1, map.size());
                // Overwrite back to null (if allowed)
                boolean rejected2 = expectNullRejected(() -> map.put("k", null));
                if (!rejected2) {
                    assertNull(map.get("k"));
                    assertEquals(before + 1, map.size());
                }
            }
        }

        @Test
        @DisplayName("Distinct objects that are equal() should refer to the same entry (overwrite semantics)")
        void equalButDistinctKeys_overwriteSameEntry() {
            MyMap<EqKey, String> map = new MyMap<>();
            EqKey k1 = new EqKey(7);
            EqKey k2 = new EqKey(7); // equals to k1 but different instance

            map.put(k1, "first");
            assertEquals(1, map.size());
            map.put(k2, "second"); // should overwrite same logical key
            assertEquals(1, map.size(), "Overwriting equal key should not grow size");
            assertEquals("second", map.get(k1));
            assertEquals("second", map.get(k2));
            assertTrue(map.containsKey(k1));
            assertTrue(map.containsKey(k2));
        }

        @Test
        @DisplayName("Idempotent removal: removing the same key twice keeps state sane")
        void remove_sameKeyTwice_idempotentOrConsistent() {
            MyMap<Integer, String> map = new MyMap<>();
            map.put(1, "a");
            map.put(2, "b");
            assertEquals(2, map.size());

            map.remove(1);
            assertEquals(1, map.size());
            assertFalse(map.containsKey(1));
            assertNull(map.get(1));

            // Second removal may be a no-op or may throw; either way size must remain
            // stable
            try {
                map.remove(1);
                assertEquals(1, map.size());
            } catch (RuntimeException ex) {
                assertTrue(ex instanceof IllegalArgumentException || ex instanceof NullPointerException);
                assertEquals(1, map.size());
            }
        }

        @Test
        @DisplayName("Heavy insert/overwrite/remove mix remains consistent")
        void heavyMixedOperations_consistency() {
            MyMap<Integer, Integer> map = new MyMap<>();

            // Insert a bunch
            int N = 1000;
            for (int i = 0; i < N; i++)
                map.put(i, i * 10);
            assertEquals(N, map.size());
            assertFalse(map.isEmpty());

            // Overwrite every 3rd key
            for (int i = 0; i < N; i += 3)
                map.put(i, i * 100);
            assertEquals(N, map.size(), "Overwriting must not change distinct key count");

            // Remove every 4th key
            int removed = 0;
            for (int i = 0; i < N; i += 4) {
                map.remove(i);
                removed++;
            }
            assertEquals(N - removed, map.size());

            // Spot-check survivors and removals
            for (int i = 0; i < N; i++) {
                if (i % 4 == 0) {
                    assertFalse(map.containsKey(i));
                    assertNull(map.get(i));
                } else {
                    assertTrue(map.containsKey(i));
                    Integer v = map.get(i);
                    assertNotNull(v, "Non-removed keys should have a value (we never stored null here)");
                }
            }

            // Remove everything
            for (int i = 0; i < N; i++) {
                try {
                    map.remove(i);
                } catch (RuntimeException ignored) {
                    /* acceptable */ }
            }
            assertTrue(map.isEmpty());
            assertEquals(0, map.size());
        }

        @Test
        @DisplayName("toString(): non-null, stable for same state, changes after mutation (if format reflects content)")
        void toString_stabilityAndChangeOnMutation() {
            MyMap<String, String> map = new MyMap<>();
            String s0 = map.toString();
            assertNotNull(s0);

            map.put("a", "A");
            map.put("b", "B");
            String s1 = map.toString();
            String s2 = map.toString();
            assertEquals(s1, s2, "Stable for the same state");

            // If implementation reflects content in toString(), it should likely differ
            // from empty
            if (!s0.equals(s1)) {
                // Optional: then after a further mutation, it should likely differ again
                map.remove("a");
                String s3 = map.toString();
                if (!s1.equals(s3)) {
                    // Looks content-based; ensure stability again
                    assertEquals(s3, map.toString());
                }
            }
        }

        @Test
        @DisplayName("Size never becomes negative; monotone under valid removals")
        void size_neverNegative() {
            MyMap<Integer, Integer> map = new MyMap<>();
            assertEquals(0, map.size());
            // Try some removes first
            try {
                map.remove(999);
            } catch (RuntimeException ignored) {
            }
            assertEquals(0, map.size());

            // Add then over-remove
            map.put(1, 1);
            map.put(2, 2);
            assertEquals(2, map.size());

            map.remove(1);
            map.remove(2);
            assertEquals(0, map.size());

            // Extra removals should not drop below 0
            try {
                map.remove(1);
            } catch (RuntimeException ignored) {
            }
            try {
                map.remove(2);
            } catch (RuntimeException ignored) {
            }
            assertEquals(0, map.size(), "Size must not be negative");
        }

        @Test
        @DisplayName("Values can be updated many times without leaking keys")
        void heavyOverwrites_noKeyLeak() {
            MyMap<String, Integer> map = new MyMap<>();
            map.put("k", 1);
            for (int i = 2; i <= 100; i++)
                map.put("k", i);
            assertEquals(1, map.size());
            assertTrue(map.containsKey("k"));
            assertEquals(100, map.get("k"));
        }

        @Test
        @DisplayName("Interleaved keys remain independent")
        void interleavedKeys_independent() {
            MyMap<String, String> map = new MyMap<>();
            map.put("A", "1");
            map.put("B", "2");
            map.put("C", "3");
            map.put("A", "1b");
            map.put("B", "2b");

            assertAll(
                    () -> assertEquals("1b", map.get("A")),
                    () -> assertEquals("2b", map.get("B")),
                    () -> assertEquals("3", map.get("C")),
                    () -> assertEquals(3, map.size()));

            map.remove("B");
            assertAll(
                    () -> assertFalse(map.containsKey("B")),
                    () -> assertNull(map.get("B")),
                    () -> assertEquals("1b", map.get("A")),
                    () -> assertEquals("3", map.get("C")),
                    () -> assertEquals(2, map.size()));
        }
    }

    /* ---------- MyEntry tests ---------- */
    @Nested
    @DisplayName("MyEntry<K,V> edge cases")
    class MyEntryEdgeCases {

        @Test
        @DisplayName("Null value handling in entry is consistent")
        void entry_nullValue_policy() {
            MyEntry<String, String> e = new MyEntry<>("k", "v");
            assertEquals("k", e.getKey());
            assertEquals("v", e.getValue());

            // Setting value to null: either allowed or rejected
            boolean rejected = expectNullRejected(() -> e.setValue(null));
            if (!rejected) {
                assertNull(e.getValue());
                // Key must remain unchanged
                assertEquals("k", e.getKey());
            }
        }

        @Test
        @DisplayName("Setting value multiple times keeps key immutable")
        void entry_keyImmutable_valueMutable() {
            MyEntry<Integer, String> e = new MyEntry<>(123, "a");
            e.setValue("b");
            e.setValue("c");
            assertAll(
                    () -> assertEquals(123, e.getKey()),
                    () -> assertEquals("c", e.getValue()));
        }

        @Test
        @DisplayName("toString(): non-null and stable for the same state")
        void entry_toStringStable() {
            MyEntry<String, Integer> e = new MyEntry<>("x", 10);
            String s1 = e.toString();
            String s2 = e.toString();
            assertNotNull(s1);
            assertEquals(s1, s2);

            // If toString reflects value, mutating the value may change it.
            String before = e.toString();
            e.setValue(99);
            String after = e.toString();
            if (!before.equals(after)) {
                assertEquals(after, e.toString(), "Still stable after mutation for the same state");
            }
        }

        @Test
        @DisplayName("Entries can be collected and updated without aliasing issues")
        void entry_listUpdate() {
            List<MyEntry<String, Integer>> list = new ArrayList<>();
            list.add(new MyEntry<>("a", 1));
            list.add(new MyEntry<>("b", 2));
            list.add(new MyEntry<>("c", 3));

            // Update values
            list.forEach(e -> e.setValue(e.getValue() + 10));

            assertAll(
                    () -> assertEquals(11, list.get(0).getValue()),
                    () -> assertEquals(12, list.get(1).getValue()),
                    () -> assertEquals(13, list.get(2).getValue()),
                    () -> assertEquals("a", list.get(0).getKey()),
                    () -> assertEquals("b", list.get(1).getKey()),
                    () -> assertEquals("c", list.get(2).getKey()));
        }
    }
}