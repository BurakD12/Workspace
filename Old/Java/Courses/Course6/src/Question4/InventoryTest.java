package Question4;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import java.util.List;

public class InventoryTest {
    private Inventory inventory;
    private Book book1;
    private Magazine magazine1;

    @Before
    public void setUp() {
        inventory = new Inventory();
        book1 = new Book(1, "Effective Java", 45.00, "Joshua Bloch");
        magazine1 = new Magazine(2, "National Geographic", 15.00, 202101);

        inventory.addItem(book1);
        inventory.addItem(magazine1);
    }

    @Test
    public void testAddItem() {
        // Test adding a new unique item
        Book book2 = new Book(3, "Clean Code", 35.00, "Robert C. Martin");
        assertTrue(inventory.addItem(book2));

        // Test adding an item with a duplicate ID
        assertFalse(inventory.addItem(new Book(1, "Java Concurrency in Practice", 55.00, "Brian Goetz")));
    }

    @Test
    public void testGetItem() {
        // Test getting an existing item
        assertEquals(book1, inventory.getItem(1));

        // Test getting a non-existing item
        assertNull(inventory.getItem(99));
    }

    @Test
    public void testUpdateItem() {
        // Test updating an existing item
        Book updatedBook = new Book(1, "Effective Java 2nd Edition", 50.00, "Joshua Bloch");
        assertTrue(inventory.updateItem(1, updatedBook));

        // Test updating a non-existing item
        assertFalse(inventory.updateItem(99, updatedBook));
    }

    @Test
    public void testDeleteItem() {
        // Test deleting an existing item
        assertTrue(inventory.deleteItem(1));
        assertNull(inventory.getItem(1));

        // Test deleting a non-existing item
        assertFalse(inventory.deleteItem(99));
    }

    @Test
    public void testGetAllItems() {
        List<Item> items = inventory.getAllItems();
        assertEquals(2, items.size());
        assertTrue(items.contains(book1));
        assertTrue(items.contains(magazine1));
    }

    @Test
    public void testSearchItemsByTitle() {
        List<Item> items = inventory.searchItemsByTitle("Effective");
        assertEquals(1, items.size());
        assertTrue(items.contains(book1));
    }

    @Test
    public void testSortItemsByPrice() {
        inventory.addItem(new Book(4, "The Pragmatic Programmer", 20.00, "Andrew Hunt"));
        List<Item> items = inventory.sortItemsByPrice();
        assertEquals(magazine1, items.get(0)); // Should be the cheapest
    }

    @Test
    public void testSortItemsById() {
        List<Item> items = inventory.sortItemsById();
        assertEquals(book1, items.get(0)); // Should have the lowest ID
    }

    @Test
    public void testGetTotalInventoryValue() {
        assertEquals(60.00, inventory.getTotalInventoryValue(), 0.01);
    }

    @Test
    public void testUpdatePrice() {
        // Test updating price of an existing item
        assertTrue(inventory.updatePrice(1, 55.00));
        assertEquals(55.00, book1.getPrice(), 0.01);

        // Test updating price of a non-existing item
        assertFalse(inventory.updatePrice(99, 55.00));
    }
}
