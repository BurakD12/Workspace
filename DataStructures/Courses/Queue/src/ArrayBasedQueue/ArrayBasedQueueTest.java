package ArrayBasedQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exhaustive tests for ArrayBasedQueue.
 * Assumes QueueOverflowException and QueueUnderflowException are checked exceptions.
 */
class ArrayBasedQueueTest {

    /* ---------- Constructor edge cases ---------- */

    @Test
    void capacityMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new ArrayBasedQueue<>(0));
        assertThrows(IllegalArgumentException.class, () -> new ArrayBasedQueue<>(-3));
    }

    @Test
    void veryLargeCapacityAllowed() {
        ArrayBasedQueue<Integer> big = new ArrayBasedQueue<>(10_000);
        assertTrue(big.isEmpty());
        assertEquals(0, big.size());
        assertFalse(big.isFull());
    }

    /* ---------- Capacity-1 queue ---------- */

    @Nested
    class SingleSlotQueue {
        private ArrayBasedQueue<String> q;

        @BeforeEach
        void setUp() {
            q = new ArrayBasedQueue<>(1);
        }

        @Test
        void emptyStateImmediatelyAfterConstruction() {
            assertTrue(q.isEmpty());
            assertFalse(q.isFull());
            assertEquals(0, q.size());
        }

        @Test
        void cycleThroughFullAndEmptyMultipleTimes() throws Exception {
            // first pass
            q.enqueue("A");
            assertEquals(1, q.size());
            assertTrue(q.isFull());
            assertEquals("A", q.peek());
            assertEquals("A", q.dequeue());
            assertTrue(q.isEmpty());
            assertFalse(q.isFull());

            // second pass – show object can be reused
            q.enqueue("B");
            assertEquals("B", q.dequeue());
            assertTrue(q.isEmpty());
        }

        @Test
        void overflowWhenFull() throws Exception {
            q.enqueue("A");
            assertThrows(QueueOverflowException.class, () -> q.enqueue("B"));
        }

        @Test
        void underflowWhenEmpty() {
            assertThrows(QueueUnderflowException.class, q::dequeue);
            assertThrows(QueueUnderflowException.class, q::peek);
        }
    }

    /* ---------- Capacity > 1 queue ---------- */

    @Nested
    class MultiSlotQueue {
        private ArrayBasedQueue<Integer> q;
        private final int CAP = 3;

        @BeforeEach
        void setUp() {
            q = new ArrayBasedQueue<>(CAP);
        }

        @Test
        void fifoOrderPreserved() throws Exception {
            q.enqueue(1);
            q.enqueue(2);
            q.enqueue(3);

            assertEquals(1, q.dequeue());
            assertEquals(2, q.dequeue());
            assertEquals(3, q.dequeue());
        }

        @Test
        void sizeEmptyFullTransitions() throws Exception {
            assertEquals(0, q.size());

            q.enqueue(10);          // 1
            q.enqueue(20);          // 2
            assertEquals(2, q.size());
            assertFalse(q.isFull());

            q.enqueue(30);          // 3 == CAP
            assertEquals(CAP, q.size());
            assertTrue(q.isFull());

            q.dequeue();            // back to 2
            assertEquals(2, q.size());
            assertTrue(q.isFull());

            q.dequeue();
            q.dequeue();
            assertTrue(q.isEmpty());
            assertEquals(0, q.size());
        }

        @Test
        void enqueueAfterPartialDequeueStillStopsAtCapacity() throws Exception {
            // Fill one short of capacity, dequeue one, then keep enqueuing
            q.enqueue(1);
            q.enqueue(2);
            assertEquals(2, q.size());

            q.dequeue();            // front moves to index 1
            assertEquals(1, q.size());

            q.enqueue(3);           // rear moves to index 2, queue now size 2
            assertEquals(2, q.size());
            assertTrue(q.isFull());

        }

        @Test
        void enqueueAfterDequeueAll_resetsAndAllowsReuse() throws Exception {
            for (int i = 0; i < CAP; i++) q.enqueue(i);   // fill
            for (int i = 0; i < CAP; i++) q.dequeue();    // empty

            assertTrue(q.isEmpty());
            assertEquals(0, q.size());

            // should work again from scratch
            q.enqueue(99);
            assertEquals(1, q.size());
            assertEquals(99, q.dequeue());
            assertTrue(q.isEmpty());
        }

        @Test
        void enqueueNullIsAllowedAndRecognised() throws Exception {
            q.enqueue(null);
            assertNull(q.peek());
            assertNull(q.dequeue());
            assertTrue(q.isEmpty());
        }

        @Test
        void underflowAfterDequeuingLastElement() throws Exception {
            q.enqueue(42);
            q.dequeue();
            assertThrows(QueueUnderflowException.class, q::dequeue);
            assertThrows(QueueUnderflowException.class, q::peek);
        }
    }

    /* ---------- Generic type sanity: custom object ---------- */

    @Test
    void worksWithUserDefinedReferenceType() throws Exception {
        record Person(String name) {}
        ArrayBasedQueue<Person> people = new ArrayBasedQueue<>(2);

        Person alice = new Person("Alice");
        Person bob   = new Person("Bob");

        people.enqueue(alice);
        people.enqueue(bob);

        assertEquals(alice, people.dequeue());
        assertEquals(bob, people.peek());
    }
}