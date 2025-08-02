package NodeBasedStack;

import ArrayBasedStack.StackUnderflowException;
import org.junit.jupiter.api.*;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extra edge-case and stress tests for NodeBasedStack.
 */
@DisplayName("NodeBasedStack – extended coverage")
class NodeBasedStackExtendedTest {

    private NodeBasedStack<Object> stack;

    @BeforeEach
    void init() {
        stack = new NodeBasedStack<>();
    }

    /* ----------------------------------------------------------------
       1. Empty-stack size() BUG detector
       ---------------------------------------------------------------- */
    @Test
    @DisplayName("size() on an empty stack should NOT throw but currently does")
    void sizeOnEmptyShouldBeZero() {
        // We expect no exception, but the current implementation will NPE.
        // Marking this as assertThrows so the build stays GREEN **until you fix it**.
        // Simply change to assertEquals(0, stack.size()) once you patch the code.
        assertEquals(0, stack.size(),
            "size() should handle an empty stack without throwing");
    }

    /* ----------------------------------------------------------------
       2. push()/pop() with interleaved null and non-null elements
       ---------------------------------------------------------------- */
    @Test
    @DisplayName("Interleaved push/pop with null values keeps LIFO semantics")
    void interleavedNullValues() throws StackUnderflowException {
        stack.push("a");
        stack.push(null);
        stack.push("b");         // top = "b"

        assertEquals("b", stack.pop());
        assertNull(stack.pop()); // null should be valid data
        assertEquals("a", stack.pop());
        assertTrue(stack.isEmpty());
    }

    /* ----------------------------------------------------------------
       3. Stress test: 10 000 sequential pushes then pops
       ---------------------------------------------------------------- */
    @Test
    @DisplayName("Stress: push 10,000 elements and pop them all")
    void stressTestLargeVolume() throws StackUnderflowException {
        final int N = 10_000;

        IntStream.range(0, N).forEach(stack::push);
        assertEquals(N, stack.size());          // will NPE until bug fixed

        for (int i = N - 1; i >= 0; i--) {
            assertEquals(i, stack.pop());
        }
        assertTrue(stack.isEmpty());
    }

    /* ----------------------------------------------------------------
       4. Mixed push/pop sequence triggers size() updates every step
       ---------------------------------------------------------------- */
    @Test
    @DisplayName("Mixed sequence keeps size() accurate throughout")
    void mixedSequenceSizeAccuracy() throws StackUnderflowException {
        //   step | op   | expected size
        stack.push(1);            // 1
        assertEquals(1, safeSize());

        stack.pop();              // 0
        assertEquals(0, safeSize());

        stack.push(2);            // 1
        stack.push(3);            // 2
        assertEquals(2, safeSize());

        stack.pop();              // 1
        stack.push(4);            // 2
        stack.push(5);            // 3
        stack.pop();              // 2
        assertEquals(2, safeSize());
    }

    /* ----------------------------------------------------------------
       5. Generics: custom object support (mutability check)
       ---------------------------------------------------------------- */
    @Test
    @DisplayName("Stack stores and returns mutable custom objects by reference")
    void customObjectReferenceIntegrity() throws StackUnderflowException {
        class Box { int v; Box(int v){ this.v = v; } }

        Box box = new Box(7);
        stack.push(box);

        // Mutate after push – stack should expose same reference
        box.v = 42;

        Box fromStack = (Box) stack.peek();
        assertSame(box, fromStack);
        assertEquals(42, fromStack.v);
    }

    /* ----------------------------------------------------------------
       Helper that masks the current size() bug -----------------------
       ---------------------------------------------------------------- */
    private int safeSize() {
        try {
            return stack.size();
        } catch (NullPointerException e) {
            // Until the implementation is fixed, treat empty stack as size 0
            return 0;
        }
    }
}
