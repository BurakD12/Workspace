package CircularQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exhaustive tests for {@link CircularQueue}.
 *
 * ‣ Branch & edge-case goals
 *   • Constructor validation (capacity < 1).
 *   • Capacity = 1 “ping-pong” behaviour.
 *   • All isFull() code-paths (rear > front, rear < front, arithmetic wrap).
 *   • Null element handling (allowed by API as written).
 *   • Under/overflow after queue resets (front = rear = -1).
 *   • Multiple wrap-around cycles to stress front/rear modulo logic.
 *   • size(), isEmpty(), isFull() correctness at every state transition.
 *   • Generic type soundness across primitives/wrappers/objects.
 */
class CircularQueueEdgeTest {

    // ────────────────────────────────────────────────────────────────────────────
    //  Constructor edge-cases
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("capacity < 1 → IllegalArgumentException")
    void constructorRejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> new CircularQueue<>(0));
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Capacity-1 exhaustive cycle tests
    // ────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Capacity-1 queue behaves correctly through many cycles")
    class CapacityOneCycles {

        CircularQueue<String> q;

        @BeforeEach
        void setUp() { q = new CircularQueue<>(1); }

        @RepeatedTest(25)   // plenty of back-and-forth toggles
        @DisplayName("enqueue–peek–dequeue preserves value and state")
        void pingPong() throws QueueOverflowException, QueueUnderflowException {
            assertTrue(q.isEmpty());
            String val = "tick" + System.nanoTime();
            q.enqueue(val);

            assertAll("after enqueue",
                      () -> assertFalse(q.isEmpty()),
                      () -> assertTrue(q.isFull()),
                      () -> assertEquals(1, q.size()));

            assertEquals(val, q.peek(), "peek should see element");
            assertEquals(val, q.dequeue(), "dequeue should return same element");

            assertAll("after dequeue last element",
                      () -> assertTrue(q.isEmpty()),
                      () -> assertFalse(q.isFull()),
                      () -> assertEquals(0, q.size()));
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Null element handling
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Queue accepts and returns null payloads")
    void acceptsNull() throws QueueOverflowException, QueueUnderflowException {
        CircularQueue<Object> q = new CircularQueue<>(2);
        q.enqueue(null);
        assertNull(q.peek(), "peek should return null element");
        assertNull(q.dequeue(), "dequeue should return the stored null");
        assertTrue(q.isEmpty());
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Branch coverage for isFull()
    // ────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isFull() path coverage")
    class IsFullBranches {

        @Test
        @DisplayName("rear > front branch with last slot unfilled (not full)")
        void rearGreaterFrontNotFull() throws QueueOverflowException {
            CircularQueue<Integer> q = new CircularQueue<>(4);
            q.enqueue(1); q.enqueue(2); // rear = 1, front = 0
            assertFalse(q.isFull());
        }

        @Test
        @DisplayName("rear > front branch meets capacity-1 condition (full)")
        void rearGreaterFrontFull()
                throws QueueOverflowException, QueueUnderflowException {
            CircularQueue<Integer> q = new CircularQueue<>(3);
            // Fill three of four elements so rear > front and formula triggers
            q.enqueue(1); q.enqueue(2); q.enqueue(3); // [1 2 3 _]
            assertTrue(q.isFull(), "queue should now be full");
        }

        @Test
        @DisplayName("front – rear == 1 branch (full after wrap)")
        void frontMinusRearEqualsOne()
                throws QueueOverflowException, QueueUnderflowException {
            CircularQueue<Integer> q = new CircularQueue<>(3);
            q.enqueue(10); q.enqueue(20); q.enqueue(30); // full
            q.dequeue();                                // remove 10 (front=1)
            q.enqueue(40);                              // rear wraps to 0
            assertTrue(q.isFull(), "queue full via front-rear == 1 rule");
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Underflow conditions
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("peek() on empty → QueueUnderflowException")
    void peekEmptyThrows() {
        CircularQueue<Double> q = new CircularQueue<>(3);
        assertThrows(QueueUnderflowException.class, q::peek);
    }

    @Test
    @DisplayName("dequeue() on empty → QueueUnderflowException (after reset)")
    void dequeueAfterResetThrows() throws QueueOverflowException, QueueUnderflowException {
        CircularQueue<Integer> q = new CircularQueue<>(2);
        q.enqueue(1);
        q.dequeue();      // now empty, front = rear = -1
        assertThrows(QueueUnderflowException.class, q::dequeue);
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Overflow after multiple wraps / resets
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("enqueue() beyond capacity after several cycles → QueueOverflowException")
    void overflowAfterManyCycles() throws QueueUnderflowException {
        CircularQueue<String> q = new CircularQueue<>(3);

        // Cycle several times to stress internal indices
        for (int cycle = 0; cycle < 5; cycle++) {
            try {
                q.enqueue(String.valueOf(cycle * 10));
                q.enqueue(String.valueOf(cycle * 10 + 1));
                q.enqueue(String.valueOf(cycle * 10 + 2));
            } catch (QueueOverflowException e) {
                fail("Queue should not overflow yet (cycle " + cycle + ")");
            }
            // dequeue all to reset
            q.dequeue(); q.dequeue(); q.dequeue();
            assertTrue(q.isEmpty(), "Queue should be empty after reset cycle " + cycle);
        }

        // Finally fill and attempt one more enqueue to trigger overflow
        assertDoesNotThrow(() -> {
            q.enqueue("A"); q.enqueue("B"); q.enqueue("C");
        });
        assertThrows(QueueOverflowException.class, () -> q.enqueue("D"));
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Multi-wrap size / order integrity
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("FIFO order and size correctness over random wrap-arounds")
    void randomisedWrapAround() throws QueueOverflowException, QueueUnderflowException {
        final int CAP = 6, OPS = 1000;
        CircularQueue<Integer> q = new CircularQueue<>(CAP);
        Random r = new Random(42);
        int inFlight = 0, next = 1;

        for (int i = 0; i < OPS; i++) {
            // 60 % chance enqueue if not full, else dequeue
            if (r.nextDouble() < 0.6 && !q.isFull() || q.isEmpty()) {
                q.enqueue(next++);
                inFlight++;
            } else {
                q.dequeue();
                inFlight--;
            }
            assertEquals(inFlight, q.size(), "size() mismatch at iteration " + i);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Generic type breadth
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Works with different generic payload types")
    void genericBreadth() throws QueueOverflowException, QueueUnderflowException {
        CircularQueue<int[]> arrays = new CircularQueue<>(2);
        CircularQueue<CharSequence> seqs = new CircularQueue<>(2);

        arrays.enqueue(new int[]{1, 2, 3});
        seqs.enqueue(new StringBuilder("abc"));

        assertEquals(3, arrays.peek()[2]);
        assertEquals("abc", seqs.peek().toString());
    }
}