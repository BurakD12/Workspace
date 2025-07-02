package Question2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class LibraryItemTest {

    @Test
    public void testBookGetInfo() {
        // Create a new Book object
        Book book = new Book("Java Fundamentals", 2021, "John Doe");

        // Define the expected output
        String expected = "Title: Java Fundamentals, Year: 2021, Author: John Doe";

        // Assert that the actual output from getInfo() matches the expected output
        assertEquals(expected, book.getInfo());
    }

    @Test
    public void testDVDGetInfo() {
        // Create a new DVD object
        DVD dvd = new DVD("Java World", 2022, "Jane Smith");

        // Define the expected output
        String expected = "Title: Java World, Year: 2022, Director: Jane Smith";

        // Assert that the actual output from getInfo() matches the expected output
        assertEquals("The getInfo method should return the correct DVD information.", expected, dvd.getInfo());
    }

    @Test
    public void testBookWithNullValues() {
        Book book = new Book(null, 2021, null);
        String expected = "Title: null, Year: 2021, Author: null";
        assertEquals("Book.getInfo() should handle null values.", expected, book.getInfo());
    }

    @Test
    public void testDVDWithEmptyStrings() {
        DVD dvd = new DVD("", 2022, "");
        String expected = "Title: , Year: 2022, Director: ";
        assertEquals("DVD.getInfo() should handle empty strings.", expected, dvd.getInfo());
    }

    @Test
    public void testBookWithNegativeYear() {
        Book book = new Book("Java Fundamentals", -1, "John Doe");
        String expected = "Title: Java Fundamentals, Year: -1, Author: John Doe";
        assertEquals("Book.getInfo() should correctly display negative years.", expected, book.getInfo());
    }

    @Test
    public void testDVDWithLargeYear() {
        DVD dvd = new DVD("Java World", Integer.MAX_VALUE, "Jane Smith");
        String expected = "Title: Java World, Year: " + Integer.MAX_VALUE + ", Director: Jane Smith";
        assertEquals("DVD.getInfo() should correctly display large year values.", expected, dvd.getInfo());
    }

    @Test
    public void testBookNotNull() {
        Book book = new Book("Java Fundamentals", 2021, "John Doe");
        assertNotNull("Book.getInfo() should not return null.", book.getInfo());
    }
}
