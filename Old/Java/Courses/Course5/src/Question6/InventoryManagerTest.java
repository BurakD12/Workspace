package Question6;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class InventoryManagerTest {
    private InventoryManager manager;
    private Product product;
    private Electronics electronicItem;
    private FoodItem foodItem;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setUp() {
        System.setOut(new PrintStream(outContent));
        manager = new InventoryManager();
        product = new Product("Generic Product", 20.00, 15);
        electronicItem = new Electronics("Laptop", 999.99, 10, 24);
        foodItem = new FoodItem("Apple", 0.99, 100, "2024-12-31");

        manager.addProduct(product);
        manager.addProduct(electronicItem);
        manager.addProduct(foodItem);
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void testAddProduct() {
        assertEquals(3, manager.getProducts().size());
    }

    @Test
    public void testRestockProduct() {
        product.restock(10);
        assertEquals(25, product.getQuantity());
    }

    @Test
    public void testSellProduct() {
        assertTrue(product.sell(5));
        assertEquals(10, product.getQuantity());
    }

    @Test
    public void testSellInsufficientStock() {
        assertFalse(product.sell(20));
        assertEquals(15, product.getQuantity()); // Quantity remains unchanged
    }

    @Test
    public void testDisplayInfo() {
        product.displayInfo();
        electronicItem.displayInfo();
        foodItem.displayInfo();

        String expectedOutput = "Product: Generic Product, Price: 20.0, Quantity: 15\n" +
                "Product: Laptop, Price: 999.99, Quantity: 10, Warranty Period: 24 months\n" +
                "Product: Apple, Price: 0.99, Quantity: 100, Expiration Date: 2024-12-31\n";
        assertEquals(expectedOutput, outContent.toString());
    }

    @Test
    public void testGetTotalInventoryValue() {
        double expectedValue = product.getPrice() * product.getQuantity() +
                electronicItem.getPrice() * electronicItem.getQuantity() +
                foodItem.getPrice() * foodItem.getQuantity();
        assertEquals(expectedValue, manager.getTotalInventoryValue(), 0.001);
    }
    
    @Test
    public void testElectronicsRestockAndSell() {
        electronicItem.restock(5);
        assertEquals(15, electronicItem.getQuantity());

        assertTrue(electronicItem.sell(10));
        assertEquals(5, electronicItem.getQuantity());
    }

    @Test
    public void testFoodItemRestockAndSell() {
        foodItem.restock(50);
        assertEquals(150, foodItem.getQuantity());

        assertTrue(foodItem.sell(20));
        assertEquals(130, foodItem.getQuantity());
    }

    @Test
    public void testInsufficientStockForElectronics() {
        assertFalse(electronicItem.sell(50)); // Trying to sell more than available
        assertEquals(10, electronicItem.getQuantity()); // Quantity remains unchanged
    }

    @Test
    public void testInsufficientStockForFoodItem() {
        assertFalse(foodItem.sell(200)); // Trying to sell more than available
        assertEquals(100, foodItem.getQuantity()); // Quantity remains unchanged
    }

    @Test
    public void testDisplayInfoForAllProducts() {
        product.displayInfo();
        assertEquals("Product: Generic Product, Price: 20.0, Quantity: 15\n", outContent.toString());

        outContent.reset(); // Clear the output stream

        electronicItem.displayInfo();
        assertEquals("Product: Laptop, Price: 999.99, Quantity: 10, Warranty Period: 24 months\n",
                outContent.toString());

        outContent.reset(); // Clear the output stream

        foodItem.displayInfo();
        assertEquals("Product: Apple, Price: 0.99, Quantity: 100, Expiration Date: 2024-12-31\n",
                outContent.toString());
    }
}