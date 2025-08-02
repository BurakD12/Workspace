/*  File: ArrayBasedStackTest.java
 *
 *  IMPORTANT
 *  ---------
 *  • These tests use JUnit-Jupiter (JUnit 5) and require the following Maven / Gradle
 *    dependency (or equivalent) on the test class-path:
 *
 *        <dependency>
 *            <groupId>org.junit.jupiter</groupId>
 *            <artifactId>junit-jupiter</artifactId>
 *            <version>5.11.0</version>
 *            <scope>test</scope>
 *        </dependency>
 *
 *  • They also rely on your custom exceptions:
 *        – ArrayBasedStack.StackOverflowException
 *        – ArrayBasedStack.StackUnderflowException
 *    Adjust the imports if those classes live elsewhere.
 */

package ArrayBasedStack;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ConstantConditions") // we *want* to test pushing null, etc.
class ArrayBasedStackTest {

    /* --------------------------  Helper methods  -------------------------- */

    /** Pushes the supplied items in order and returns the same stack for fluency. */
    private <T> ArrayBasedStack<T> fill(ArrayBasedStack<T> stack, T... items) throws Exception {
        for (T t : items) stack.push(t);
        return stack;
    }

    /** Verifies size, isEmpty and isFull all at once. */
    private <T> void assertState(ArrayBasedStack<T> s, int expectedSize, int capacity) {
        assertEquals(expectedSize, s.size(), "size()");
        assertEquals(expectedSize == 0, s.isEmpty(), "isEmpty()");
        assertEquals(expectedSize == capacity, s.isFull(), "isFull()");
    }

    /* --------------------------  Constructor  ----------------------------- */

    @Nested @DisplayName("Constructor")
    class ConstructorTests {

        @ParameterizedTest
        @ValueSource(ints = {1, 10, 128})
        @DisplayName("accepts positive capacity")
        void acceptsPositiveCapacity(int cap) {
            assertDoesNotThrow(() -> {
                ArrayBasedStack<Integer> s = new ArrayBasedStack<>(cap);
                assertState(s, 0, cap);
            });
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -16})
        @DisplayName("rejects zero or negative capacity")
        void rejectsNonPositiveCapacity(int cap) {
            assertThrows(IllegalArgumentException.class,
                         () -> new ArrayBasedStack<>(cap),
                         "Should reject capacity ≤ 0");
        }
    }

    /* --------------------------  push()  ---------------------------------- */

    @Nested @DisplayName("push(T)")
    class PushTests {

        @Test
        @DisplayName("pushes one element onto an empty stack")
        void pushSingle() throws Exception {
            ArrayBasedStack<String> s = new ArrayBasedStack<>(3);
            s.push("A");
            assertState(s, 1, 3);
            assertEquals("A", s.peek());
        }

        @Test
        @DisplayName("push maintains LIFO order")
        void pushOrder() throws Exception {
            ArrayBasedStack<Integer> s = new ArrayBasedStack<>(3);
            fill(s, 1, 2, 3);          // top == 3
            assertEquals(3, s.peek());
        }

        @Test
        @DisplayName("push to full capacity then overflow")
        void pushOverflow() throws Exception {
            ArrayBasedStack<Integer> s = new ArrayBasedStack<>(2);
            fill(s, 42, 43);
            assertState(s, 2, 2);
            assertThrows(StackOverflowException.class, () -> s.push(99));
        }

        @Test
        @DisplayName("push null (allowed or rejected)")
        void pushNull() throws Exception {
            ArrayBasedStack<String> s = new ArrayBasedStack<>(1);
            try {
                s.push(null);
                // If allowed, null should now be at the top.
                assertNull(s.peek());
            } catch (NullPointerException | IllegalArgumentException e) {
                // Accept either behaviour as long as it is documented.
            }
        }
    }

    /* --------------------------  pop()  ----------------------------------- */

    @Nested @DisplayName("pop()")
    class PopTests {

        @Test
        @DisplayName("pop returns last-in element and shrinks size")
        void popNormal() throws Exception {
            ArrayBasedStack<Integer> s = new ArrayBasedStack<>(3);
            fill(s, 10, 20);
            int top = s.pop();
            assertEquals(20, top);
            assertState(s, 1, 3);
            assertEquals(10, s.peek());
        }

        @Test
        @DisplayName("pop until empty then underflow")
        void popUnderflow() throws Exception {
            ArrayBasedStack<String> s = new ArrayBasedStack<>(1);
            fill(s, "only");
            assertEquals("only", s.pop());
            assertState(s, 0, 1);
            assertThrows(StackUnderflowException.class, s::pop);
        }
    }

    /* --------------------------  peek()  ---------------------------------- */

    @Nested @DisplayName("peek()")
    class PeekTests {

        @Test
        @DisplayName("peek does not remove element")
        void peekDoesNotRemove() throws Exception {
            ArrayBasedStack<Integer> s = new ArrayBasedStack<>(2);
            fill(s, 7);
            assertEquals(7, s.peek());
            assertState(s, 1, 2); // size unchanged
        }

        @Test
        @DisplayName("peek on empty stack throws underflow")
        void peekUnderflow() {
            ArrayBasedStack<Integer> s = new ArrayBasedStack<>(5);
            assertThrows(StackUnderflowException.class, s::peek);
        }
    }

    /* ------------------  size(), isEmpty(), isFull()  ---------------------- */

    @Nested @DisplayName("size / isEmpty / isFull")
    class QueryTests {

        @Test
        @DisplayName("size grows and shrinks predictably")
        void sizeAcrossMutations() throws Exception {
            int cap = 4;
            ArrayBasedStack<Integer> s = new ArrayBasedStack<>(cap);

            // 0 → cap
            for (int i = 1; i <= cap; i++) {
                s.push(i);
                assertState(s, i, cap);
            }

            // cap → 0
            for (int i = cap - 1; i >= 0; i--) {
                s.pop();
                assertState(s, i, cap);
            }
        }

        @ParameterizedTest(name = "after pushing {0} items into capacity {1}")
        @CsvSource({
            "0,5", "1,5", "4,5", "5,5"
        })
        void predicatesMatchSize(int pushes, int cap) throws Exception {
            ArrayBasedStack<Integer> s = new ArrayBasedStack<>(cap);
            IntStream.range(0, pushes).forEach(i -> assertDoesNotThrow(() -> s.push(i)));
            assertState(s, pushes, cap);
        }
    }

    /* -----------------------  Generic parameter T  ------------------------ */

    @Nested @DisplayName("Generics sanity check")
    class GenericTests {

        @Test
        @DisplayName("stack works with multiple reference types")
        void genericTypes() throws Exception {
            ArrayBasedStack<String> ss = new ArrayBasedStack<>(2);
            ArrayBasedStack<Double> ds = new ArrayBasedStack<>(2);

            ss.push("hi");      ds.push(3.14);
            assertEquals("hi", ss.peek());
            assertEquals(3.14, ds.peek());
        }
    }
}
