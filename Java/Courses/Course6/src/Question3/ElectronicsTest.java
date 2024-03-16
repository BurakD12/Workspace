package Question3;

import static org.junit.Assert.*;
import org.junit.Test;
import java.time.LocalDate;

public class ElectronicsTest {

    // Test cases for Laptop
    @Test
    public void testLaptopAttributes() {
        LocalDate purchaseDate = LocalDate.now();
        Laptop laptop = new Laptop("Lenovo", 1500.00, "123456789", purchaseDate, 256, 10);
        assertEquals("Lenovo", laptop.getBrand());
        assertEquals(1500.00, laptop.getPrice(), 0.01);
        assertEquals("123456789", laptop.getSerialNumber());
        assertEquals(purchaseDate, laptop.getPurchaseDate());
        assertEquals(256, laptop.getStorageSize());
        assertEquals(10, laptop.getBatteryLife());
    }

    @Test
    public void testLaptopDiscount() {
        Laptop laptop = new Laptop("Lenovo", 1500.00, "123456789", LocalDate.now(), 256, 10);
        assertEquals(1200.00, laptop.discountPrice(0.2), 0.01); // 20% discount
    }

    @Test
    public void testLaptopWarranty() {
        Laptop laptop = new Laptop("Lenovo", 1500.00, "123456789", LocalDate.now().minusYears(1).minusDays(1), 256, 10);
        assertTrue(laptop.warrantyCheck());
    }

    // Test cases for Smartphone
    @Test
    public void testSmartphoneAttributes() {
        Smartphone phone = new Smartphone("Apple", 999.99, "987654321", LocalDate.now(), 5.8, 12, true);
        assertEquals("Apple", phone.getBrand());
        assertEquals(999.99, phone.getPrice(), 0.01);
        assertEquals("987654321", phone.getSerialNumber());
        assertEquals(5.8, phone.getScreenSize(), 0.01);
        assertEquals(12, phone.getCameraMegapixels(), 0.01);
        assertTrue(phone.isDualSim());
    }

    @Test
    public void testSmartphoneDiscount() {
        Smartphone phone = new Smartphone("Apple", 999.99, "987654321", LocalDate.now(), 5.8, 12, false);
        assertEquals(899.991, phone.discountPrice(0.1), 0.001); // 10% discount
    }

    @Test
    public void testSmartphoneWarranty() {
        Smartphone phone = new Smartphone("Apple", 999.99, "987654321", LocalDate.now().minusMonths(11), 5.8, 12,
                false);
        assertTrue(phone.warrantyCheck());
    }

    // Test cases for Tablet
    @Test
    public void testTabletAttributes() {
        Tablet tablet = new Tablet("Samsung", 600.00, "55555555", LocalDate.now(), 10.1, true, true);
        assertEquals("Samsung", tablet.getBrand());
        assertEquals(600.00, tablet.getPrice(), 0.01);
        assertEquals("55555555", tablet.getSerialNumber());
        assertEquals(10.1, tablet.getScreenSize(), 0.01);
        assertTrue(tablet.hasCellular());
        assertTrue(tablet.canDock());
    }

    @Test
    public void testTabletDiscount() {
        Tablet tablet = new Tablet("Samsung", 600.00, "55555555", LocalDate.now(), 10.1, false, false);
        assertEquals(540.00, tablet.discountPrice(0.1), 0.01); // 10% discount
    }

    @Test
    public void testTabletWarranty() {
        Tablet tablet = new Tablet("Samsung", 600.00, "55555555", LocalDate.now().minusMonths(6), 10.1, false, false);
        assertTrue(tablet.warrantyCheck());
    }
}
