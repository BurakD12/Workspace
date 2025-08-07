package NodeBasedQueue;

import ArrayBasedQueue.QueueUnderflowException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extra-edge-cases and property-style tests for {@link NodeBasedQueue}.
 *
 *  ◉  Cross-checked random command stream (vs java.util.ArrayDeque)
 *  ◉  Parameterised FIFO for lengths 2‒256
 *  ◉  Post-exception invariants (empty queue remains untouched)
 *  ◉  Multiple null payloads (if allowed)
 *  ◉  Reference identity round-trip for custom objects
 *  ◉  Size never negative in random stress
 *  ◉  (Disabled) Concurrency smoke test
 */
class NodeBasedQueueExtendedTest {

    private NodeBasedQueue<Integer> queue;

    @BeforeEach
    void init() {
        queue = new NodeBasedQueue<>();
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Property-style check: randomised command stream
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("randomised enqueue/dequeue stream mirrors ArrayDeque")
    void randomCommandStreamMatchesReference() throws QueueUnderflowException {
        final int OPS = 50_000;
        Random rnd = new Random(0xBEEFCAFE);
        Deque<Integer> ref = new ArrayDeque<>();

        for (int i = 0; i < OPS; i++) {
            boolean doEnq = rnd.nextBoolean() || ref.isEmpty();   // always enqueue if ref is empty
            if (doEnq) {
                int v = rnd.nextInt();
                queue.enqueue(v);
                ref.addLast(v);
            } else {
                assertEquals(ref.removeFirst(), queue.dequeue());
            }

            // invariants aligned every step
            assertEquals(ref.size(), queue.size(), "size invariant mismatch");
            assertEquals(ref.isEmpty(), queue.isEmpty(), "emptiness invariant mismatch");
            if (!ref.isEmpty()) {
                assertEquals(ref.peekFirst(), queue.peek(), "peek value mismatch");
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Parameterised FIFO check for various lengths
    // ──────────────────────────────────────────────────────────────────────
    @ParameterizedTest(name = "FIFO order for {0} elements")
    @ValueSource(ints = {2, 3, 10, 64, 256})
    void fifoForVariousLengths(int n) throws QueueUnderflowException {
        IntStream.range(0, n).forEach(queue::enqueue);
        for (int expected = 0; expected < n; expected++) {
            assertEquals(expected, queue.dequeue(), "FIFO violated at index " + expected);
        }
        assertTrue(queue.isEmpty());
    }

    // ──────────────────────────────────────────────────────────────────────
    //  State after *failed* operations must remain consistent
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("post-underflow state remains empty & size==0")
    void stateAfterUnderflow() {
        assertAll(
                () -> assertThrows(QueueUnderflowException.class, queue::peek),
                () -> assertTrue(queue.isEmpty()),
                () -> assertEquals(0, queue.size()),

                () -> assertThrows(QueueUnderflowException.class, queue::dequeue),
                () -> assertTrue(queue.isEmpty()),
                () -> assertEquals(0, queue.size())
        );
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Idempotent peek() check
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("multiple consecutive peek() calls do not mutate queue")
    void repeatedPeekIsIdempotent() throws QueueUnderflowException {
        queue.enqueue(7);
        for (int i = 0; i < 100; i++) {
            assertEquals(7, queue.peek());
            assertEquals(1, queue.size());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Multiple null payloads
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("queue handles consecutive null elements or explicitly forbids them")
    void consecutiveNulls() throws QueueUnderflowException {
        try {
            queue.enqueue(null);
            queue.enqueue(null);

            assertNull(queue.dequeue());
            assertNull(queue.dequeue());
            assertTrue(queue.isEmpty());
        } catch (NullPointerException npe) {
            // If implementation forbids nulls, we must consistently fail
            NodeBasedQueue<Object> nullQ = new NodeBasedQueue<>();
            assertThrows(NullPointerException.class, () -> nullQ.enqueue(null));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Reference-identity preservation with custom objects
    // ──────────────────────────────────────────────────────────────────────
    static class Payload {
        final int id;
        Payload(int id) { this.id = id; }
    }

    @Test
    @DisplayName("reference identity round-trip for user objects")
    void referenceIdentity() throws QueueUnderflowException {
        Payload p = new Payload(99);
        NodeBasedQueue<Payload> queue = new NodeBasedQueue<>();
        queue.enqueue(p);
        WeakReference<Payload> tracked = new WeakReference<>(p);

        assertSame(p, queue.peek());
        assertSame(p, queue.dequeue());

        // After dequeue the queue no longer owns the object; GC *may* collect it.
        p = null;
        System.gc();
        assertNull(tracked.get(), "Object should become weakly reachable after dequeue");
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Size never dips negative in random stress
    // ──────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("size never negative under churn")
    void sizeNeverNegative() throws QueueUnderflowException {
        Random rnd = new Random();
        int acc = 0; // shadow size

        for (int i = 0; i < 20_000; i++) {
            if (rnd.nextBoolean() || acc == 0) {
                queue.enqueue(i);
                acc++;
            } else {
                queue.dequeue();
                acc--;
            }
            assertTrue(queue.size() >= 0, "size became negative");
        }
    }
}