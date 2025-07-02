package Question5;

import org.junit.Before;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import java.util.List;

public class LibraryTest {
    private Library library;
    private Book book1, book2, book3;
    private CD cd1, cd2;
    private DVD dvd1, dvd2;

    @Before
    void setUp() {
        library = new Library();
        book1 = new Book("Effective Java", "Joshua Bloch", "0321356683");
        book2 = new Book("Java Concurrency in Practice", "Brian Goetz", "0321349601");
        book3 = new Book("Clean Code", "Robert C. Martin", "9780132350884");
        cd1 = new CD("Random Access Memories", "Daft Punk", 13);
        cd2 = new CD("Discovery", "Daft Punk", 14);
        dvd1 = new DVD("Inception", "Christopher Nolan", 148);
        dvd2 = new DVD("Interstellar", "Christopher Nolan", 169);

        library.addItem(book1);
        library.addItem(book2);
        library.addItem(book3);
        library.addItem(cd1);
        library.addItem(cd2);
        library.addItem(dvd1);
        library.addItem(dvd2);
    }
    @Test
    void testAddItem() {
        assertEquals(4, library.listAvailableItems().size());
    }

    @Test
    void testUpdateItem() {
        Book updatedBook = new Book("Effective Java 2nd Edition", "Joshua Bloch", "0321356683");
        library.updateItem("Effective Java", updatedBook);
        assertEquals("Effective Java 2nd Edition", library.listAvailableItems().get(0).getTitle());
    }

    @Test
    void testDeleteItem() {
        library.deleteItem("Java Concurrency in Practice");
        assertEquals(3, library.listAvailableItems().size());
    }

    @Test
    void testCheckOutAndReturnItem() {
        book1.checkOut();
        assertFalse(book1.isAvailable());
        book1.returnItem();
        assertTrue(book1.isAvailable());
    }

    @Test
    void testFindItemsByAuthor() {
        List<Book> booksByJoshua = library.findItemsByAuthor("Joshua Bloch");
        assertEquals(1, booksByJoshua.size());
        assertEquals("Effective Java", booksByJoshua.get(0).getTitle());
    }

    @Test
    void testFindItemsByArtist() {
        List<CD> cdsByDaftPunk = library.findItemsByArtist("Daft Punk");
        assertEquals(1, cdsByDaftPunk.size());
        assertEquals("Random Access Memories", cdsByDaftPunk.get(0).getTitle());
    }

    @Test
    void testCountItemsByType() {
        Map<String, Integer> count = library.countItemsByType();
        assertEquals(2, count.get("Book"));
        assertEquals(1, count.get("CD"));
        assertEquals(1, count.get("DVD"));
    }

    @Test
    void testListAvailableItems() {
        assertEquals(4, library.listAvailableItems().size());
        book1.checkOut();
        assertEquals(3, library.listAvailableItems().size());
    }

    @Test
    void testCheckOutItems() {
        List<String> titles = List.of("Effective Java", "Random Access Memories");
        library.checkOutItems(titles);
        assertFalse(book1.isAvailable());
        assertFalse(cd1.isAvailable());
    }

    @Test
    void testReturnItems() {
        List<String> titles = List.of("Effective Java", "Random Access Memories");
        library.checkOutItems(titles);
        library.returnItems(titles);
        assertTrue(book1.isAvailable());
        assertTrue(cd1.isAvailable());
    }

    @Test
    void testUpdateNonexistentItem() {
        Book nonExistentBook = new Book("Non Existent", "Unknown", "0000000000");
        library.updateItem("Non Existent", nonExistentBook);
        assertNull(library.findItemByTitle("Non Existent"));
    }

    @Test
    void testDeleteNonexistentItem() {
        int initialSize = library.listAvailableItems().size();
        library.deleteItem("Non Existent");
        assertEquals(initialSize, library.listAvailableItems().size());
    }

    @Test
    void testFindItemsByNonexistentAuthor() {
        assertTrue(library.findItemsByAuthor("Nonexistent Author").isEmpty());
    }

    @Test
    void testFindItemsByNonexistentArtist() {
        assertTrue(library.findItemsByArtist("Nonexistent Artist").isEmpty());
    }

    @Test
    void testCheckOutNonexistentItems() {
        List<String> titles = List.of("Nonexistent Book", "Nonexistent CD");
        library.checkOutItems(titles);
        assertTrue(library.listAvailableItems().containsAll(List.of(book1, book2, book3, cd1, cd2, dvd1, dvd2)));
    }

    @Test
    void testReturnNonexistentItems() {
        List<String> titles = List.of("Nonexistent Book", "Nonexistent CD");
        library.returnItems(titles);
        assertTrue(library.listAvailableItems().containsAll(List.of(book1, book2, book3, cd1, cd2, dvd1, dvd2)));
    }

    @Test
    void testAddDuplicateItem() {
        int initialSize = library.listAvailableItems().size();
        library.addItem(book1);
        assertEquals(initialSize, library.listAvailableItems().size(), "Duplicate items should not be added.");
    }

    @Test
    void testFindItemByTitle() {
        assertNotNull(library.findItemByTitle("Effective Java"));
        assertNull(library.findItemByTitle("Nonexistent Book"));
    }

    @Test
    void testFindItemsByType() {
        assertEquals(3, library.findItemsByType("Book").size());
        assertEquals(2, library.findItemsByType("CD").size());
        assertEquals(2, library.findItemsByType("DVD").size());
        assertTrue(library.findItemsByType("Nonexistent Type").isEmpty());
    }
}
