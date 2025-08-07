package PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.Collectors;

import org.junit.jupiter.api.*;

import ArrayBasedQueue.QueueOverflowException;
import ArrayBasedQueue.QueueUnderflowException;

@DisplayName("PriorityQueue – exhaustive & edge-case tests (max-priority)")
class PriorityQueueEdgeCasesTest {

    /* ----------------------------- Helpers ------------------------------ */

    private static <T extends Comparable<T>> List<T> drain(PriorityQueue<T> pq) throws QueueUnderflowException {
        List<T> out = new ArrayList<>();
        while (!pq.isEmpty()) out.add(pq.dequeue());
        return out;
    }

    /** Expect non-increasing (descending with ties allowed) order. */
    private static <T extends Comparable<T>> void assertNonIncreasing(List<T> a) {
    for (int i = 1; i < a.size(); i++) {
        final int idx = i; // effectively final copy for the lambda
        assertTrue(a.get(i - 1).compareTo(a.get(i)) >= 0,
            () -> "Order violation at index " + idx + ": " + a.get(idx - 1) + " < " + a.get(idx));
    }
}

    /** Multiset equality: same element counts regardless of order. */
    private static <T> void assertSameMultiset(Collection<T> expected, Collection<T> actual) {
        Map<T, Long> c1 = expected.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        Map<T, Long> c2 = actual.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        assertEquals(c1, c2, "Output multiset differs from input multiset");
    }

    /* ------------------------ Constructor & Basics ----------------------- */

    @Nested
    class ConstructorAndState {
        @Test
        void capacityZeroOrNegative_throws() {
            assertThrows(IllegalArgumentException.class, () -> new PriorityQueue<Integer>(0));
            assertThrows(IllegalArgumentException.class, () -> new PriorityQueue<Integer>(-1));
        }

        @Test
        void freshQueue_isEmptySize0NotFull() {
            PriorityQueue<Integer> pq = new PriorityQueue<>(4);
            assertTrue(pq.isEmpty());
            assertEquals(0, pq.size());
            assertFalse(pq.isFull());
        }

        @Test
        void capacityOne_boundary() throws Exception {
            PriorityQueue<Integer> pq = new PriorityQueue<>(1);
            assertTrue(pq.isEmpty());
            pq.enqueue(7);
            assertTrue(pq.isFull());
            assertEquals(1, pq.size());
            assertEquals(7, pq.peek());
            assertThrows(QueueOverflowException.class, () -> pq.enqueue(8));
            assertEquals(7, pq.dequeue());
            assertTrue(pq.isEmpty());
            assertEquals(0, pq.size());
            assertThrows(QueueUnderflowException.class, pq::dequeue);
            assertThrows(QueueUnderflowException.class, pq::peek);
        }
    }

    /* ---------------------------- Null handling -------------------------- */

    @Nested
    class NullHandling {
        @Test
        void enqueueNull_throwsNPEorIAE() {
            PriorityQueue<Integer> pq = new PriorityQueue<>(3);
            assertThrows(RuntimeException.class, () -> { // Accept NPE or IAE
                try { pq.enqueue(null); }
                catch (NullPointerException | IllegalArgumentException ex) { throw ex; }
            });
            assertTrue(pq.isEmpty(), "Queue should remain empty after failed enqueue(null)");
            assertEquals(0, pq.size());
        }
    }

    /* ----------------------------- Overflow ------------------------------ */

    @Nested
    class OverflowBehavior {
        @Test
        void fillToCapacity_thenOverflow() throws Exception {
            PriorityQueue<Integer> pq = new PriorityQueue<>(3);
            pq.enqueue(10); pq.enqueue(20); pq.enqueue(30);
            assertTrue(pq.isFull());
            assertEquals(3, pq.size());
            assertThrows(QueueOverflowException.class, () -> pq.enqueue(40));
            // Ensure state intact after exception
            assertTrue(pq.isFull());
            assertEquals(30, pq.peek()); // max still there
        }

        @Test
        void recoverAfterOverflow_tryAgainAfterDequeue() throws Exception {
            PriorityQueue<Integer> pq = new PriorityQueue<>(2);
            pq.enqueue(1); pq.enqueue(2);
            assertThrows(QueueOverflowException.class, () -> pq.enqueue(3));
            assertEquals(2, pq.dequeue()); // remove max
            assertTrue(pq.isFull());
            assertThrows(QueueOverflowException.class, () -> pq.enqueue(3));
            // Now expect 3 then 1
            assertEquals(1, pq.dequeue());
            assertTrue(pq.isEmpty());
        }
    }

    /* ----------------------------- Underflow ----------------------------- */

    @Nested
    class UnderflowBehavior {
        @Test
        void dequeueOnEmpty_throws() {
            PriorityQueue<Integer> pq = new PriorityQueue<>(2);
            assertThrows(QueueUnderflowException.class, pq::dequeue);
        }

        @Test
        void peekOnEmpty_throws() {
            PriorityQueue<Integer> pq = new PriorityQueue<>(2);
            assertThrows(QueueUnderflowException.class, pq::peek);
        }

        @Test
        void repeatedUnderflowDoesNotCorruptState() {
            PriorityQueue<Integer> pq = new PriorityQueue<>(2);
            assertThrows(QueueUnderflowException.class, pq::dequeue);
            assertThrows(QueueUnderflowException.class, pq::peek);
            assertTrue(pq.isEmpty());
            assertEquals(0, pq.size());
        }
    }

    /* ---------------------- Ordering & Duplicates ------------------------ */

    @Nested
    class OrderingAndDuplicates {
        @Test
        void ascendingInsertion_stillDequeuesDescending() throws Exception {
            PriorityQueue<Integer> pq = new PriorityQueue<>(6);
            for (int v : new int[]{1,2,3,4,5,6}) pq.enqueue(v);
            List<Integer> out = drain(pq);
            assertEquals(List.of(6,5,4,3,2,1), out);
        }

        @Test
        void descendingInsertion_retainsDescendingOutput() throws Exception {
            PriorityQueue<Integer> pq = new PriorityQueue<>(6);
            for (int v : new int[]{6,5,4,3,2,1}) pq.enqueue(v);
            List<Integer> out = drain(pq);
            assertEquals(List.of(6,5,4,3,2,1), out);
        }

        @Test
        void duplicates_preservedCounts_orderNonIncreasing() throws Exception {
            PriorityQueue<Integer> pq = new PriorityQueue<>(10);
            List<Integer> in = List.of(5,5,3,3,3,7,7,1,1,7);
            for (int v : in) pq.enqueue(v);
            List<Integer> out = drain(pq);
            assertSameMultiset(in, out);
            assertNonIncreasing(out);
        }

        @Test
        void extremeIntegerValues() throws Exception {
            PriorityQueue<Integer> pq = new PriorityQueue<>(5);
            List<Integer> in = List.of(Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE);
            for (int v : in) pq.enqueue(v);
            List<Integer> out = drain(pq);
            assertEquals(List.of(Integer.MAX_VALUE, 1, 0, -1, Integer.MIN_VALUE), out);
        }
    }

    /* -------------------------- Peek semantics --------------------------- */

    @Nested
    class PeekSemantics {
        @Test
        void peekIdempotent_doesNotRemove() throws Exception {
            PriorityQueue<Integer> pq = new PriorityQueue<>(3);
            pq.enqueue(2); pq.enqueue(9); pq.enqueue(5);
            assertEquals(9, pq.peek());
            assertEquals(9, pq.peek());
            assertEquals(3, pq.size());
            assertEquals(9, pq.dequeue());
            assertEquals(2, pq.size());
        }

        @Test
        void peekTracksNewMaxAfterDequeueOrEnqueue() throws Exception {
            PriorityQueue<Integer> pq = new PriorityQueue<>(5);
            pq.enqueue(4); pq.enqueue(10); pq.enqueue(6);
            assertEquals(10, pq.peek());
            assertEquals(10, pq.dequeue());
            assertEquals(6, pq.peek());
            pq.enqueue(100);
            assertEquals(100, pq.peek());
        }
    }

    /* ----------------------- Interleaving operations --------------------- */

    @Nested
    class Interleaving {
        @Test
        void alternatingEnqueueDequeueMaintainsInvariants() throws Exception {
            PriorityQueue<Integer> pq = new PriorityQueue<>(6);
            pq.enqueue(5);                // [5]
            assertEquals(5, pq.peek());
            pq.enqueue(1);                // [5,1]
            assertEquals(5, pq.dequeue()); // [1]
            pq.enqueue(3);                // [3,1]
            pq.enqueue(4);                // [4,3,1]
            assertEquals(4, pq.peek());
            pq.enqueue(2);                // [4,3,2,1] full
            assertEquals(4, pq.dequeue()); // [3,2,1]
            assertFalse(pq.isFull());
            pq.enqueue(9);                // [9,3,2,1]
            assertEquals(9, pq.peek());
            List<Integer> out = drain(pq);
            assertEquals(List.of(9,3,2,1), out);
            assertTrue(pq.isEmpty());
        }

        @Test
        void sizeNeverNegative_evenOnExceptions() {
            PriorityQueue<Integer> pq = new PriorityQueue<>(1);
            assertEquals(0, pq.size());
            assertThrows(QueueUnderflowException.class, pq::dequeue);
            assertEquals(0, pq.size());
            assertThrows(QueueUnderflowException.class, pq::peek);
            assertEquals(0, pq.size());
        }
    }

    /* -------------------------- Generic types ---------------------------- */

    @Nested
    class GenericTypes {
        @Test
        void strings_lexicographicMaxFirst() throws Exception {
            PriorityQueue<String> pq = new PriorityQueue<>(6);
            // mix of lowercase/uppercase to ensure natural ordering is used
            pq.enqueue("banana");
            pq.enqueue("Apple");
            pq.enqueue("carrot");
            pq.enqueue("apple");
            List<String> out = drain(pq);
            // Natural String order puts uppercase before lowercase; just check non-increasing and multiset
            assertNonIncreasing(out);
            assertSameMultiset(List.of("banana","Apple","carrot","apple"), out);
        }

        @Test
        void customComparable_reversePriorityType() throws Exception {
            PriorityQueue<ReverseInt> pq = new PriorityQueue<>(6);
            // ReverseInt.compareTo defines higher natural order for smaller numeric value
            // That means max-priority by natural order will behave as MIN by numeric value.
            List<ReverseInt> in = List.of(new ReverseInt(5), new ReverseInt(2),
                                          new ReverseInt(9), new ReverseInt(2));
            for (ReverseInt r : in) pq.enqueue(r);
            List<ReverseInt> out = drain(pq);
            // Expect non-increasing by ReverseInt's compareTo, i.e., by ascending numeric value (ties allowed)
            assertNonIncreasing(out);
            // Extract numeric values to verify ascending numerics: 2,2,5,9
            List<Integer> nums = out.stream().map(r -> r.value).toList();
            assertEquals(List.of(2,2,5,9), nums);
        }

        /** Comparable where smaller numeric value is considered "greater" in natural order. */
        private static final class ReverseInt implements Comparable<ReverseInt> {
            final int value;
            ReverseInt(int v) { this.value = v; }
            @Override public int compareTo(ReverseInt o) {
                // reverse: smaller value => larger in natural order
                return Integer.compare(o.value, this.value);
            }
            @Override public String toString() { return "R(" + value + ")"; }
            @Override public boolean equals(Object o) { return (o instanceof ReverseInt r) && r.value == value; }
            @Override public int hashCode() { return Integer.hashCode(value); }
        }
    }

    /* ---------------------- Property-style robustness -------------------- */

    @Nested
    class PropertyChecks {
        @Test
        @DisplayName("Random bag property: drain equals sorted descending & same multiset")
        void randomBag_sortedDescendingEqualsDrain() throws Exception {
            int n = 200; // keep small for unit speed; increase if you like
            int cap = n;
            PriorityQueue<Integer> pq = new PriorityQueue<>(cap);
            Random rnd = new Random(12345); // deterministic
            List<Integer> in = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                int v = rnd.nextInt(1000) - 500; // include negatives
                in.add(v);
                pq.enqueue(v);
            }
            List<Integer> out = drain(pq);
            List<Integer> sorted = new ArrayList<>(in);
            sorted.sort(Comparator.reverseOrder());
            assertEquals(sorted, out, "Drain should match reverse-sorted input exactly");
            assertSameMultiset(in, out);
            assertTrue(pq.isEmpty());
            assertEquals(0, pq.size());
        }

        @Test
        @DisplayName("Stress duplicates and plateaus")
        void manyDuplicates_plateaus() throws Exception {
            int cap = 200;
            PriorityQueue<Integer> pq = new PriorityQueue<>(cap);
            // create big plateaus of same priority
            int[] vals = new int[cap];
            Arrays.fill(vals, 7);
            for (int i = 0; i < cap; i += 10) vals[i] = 9;
            for (int i = 5; i < cap; i += 10) vals[i] = -1;

            List<Integer> in = new ArrayList<>(cap);
            for (int v : vals) { pq.enqueue(v); in.add(v); }
            List<Integer> out = drain(pq);
            assertNonIncreasing(out);
            assertSameMultiset(in, out);
        }
    }
}