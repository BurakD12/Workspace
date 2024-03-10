package Question5;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class TestVehicleManager {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void cleanUpStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void testCarStart() {
        Car car = new Car("Toyota", "Corolla", 2020);
        car.start();
        assertEquals("Car is starting.\n", outContent.toString());
    }

    @Test
    public void testCarStop() {
        Car car = new Car("Toyota", "Corolla", 2020);
        car.stop();
        assertEquals("Car is stopping.\n", outContent.toString());
    }

    @Test
    public void testCarDisplayInfo() {
        Car car = new Car("Toyota", "Corolla", 2020);
        car.displayInfo();
        assertEquals("Car: Toyota Corolla 2020\n", outContent.toString());
    }

    @Test
    public void testTruckStart() {
        Truck truck = new Truck("Ford", "F-150", 2019, 1000.0);
        truck.start();
        assertEquals("Truck is starting.\n", outContent.toString());
    }

    @Test
    public void testTruckStop() {
        Truck truck = new Truck("Ford", "F-150", 2019, 1000.0);
        truck.stop();
        assertEquals("Truck is stopping.\n", outContent.toString());
    }

    @Test
    public void testTruckDisplayInfo() {
        Truck truck = new Truck("Ford", "F-150", 2019, 1000.0);
        truck.displayInfo();
        assertEquals("Truck: Ford F-150 2019 Payload Capacity: 1000.0\n", outContent.toString());
    }

    @Test
    public void testAddVehicle() {
        VehicleManager manager = new VehicleManager();
        Car car = new Car("Toyota", "Corolla", 2020);
        manager.addVehicle(car);
        // Assuming getVehicles() is a public method that returns the list of vehicles
        assertEquals(1, manager.getVehicles().size());
    }

    @Test
    public void testDisplayAllVehicles() {
        VehicleManager manager = new VehicleManager();
        Car car = new Car("Toyota", "Corolla", 2020);
        Truck truck = new Truck("Ford", "F-150", 2019, 1000.0);
        manager.addVehicle(car);
        manager.addVehicle(truck);
        manager.displayAllVehicles();
        String expectedOutput = "Car: Toyota Corolla 2020\nTruck: Ford F-150 2019 Payload Capacity: 1000.0\n";
        assertEquals(expectedOutput, outContent.toString());
    }

    @Test
    public void testStartAllVehicles() {
        VehicleManager manager = new VehicleManager();
        Car car = new Car("Toyota", "Corolla", 2020);
        Truck truck = new Truck("Ford", "F-150", 2019, 1000.0);
        manager.addVehicle(car);
        manager.addVehicle(truck);
        manager.startAllVehicles();
        String expectedOutput = "Car is starting.\nTruck is starting.\n";
        assertEquals(expectedOutput, outContent.toString());
    }

    @Test
    public void testStopAllVehicles() {
        VehicleManager manager = new VehicleManager();
        Car car = new Car("Toyota", "Corolla", 2020);
        Truck truck = new Truck("Ford", "F-150", 2019, 1000.0);
        manager.addVehicle(car);
        manager.addVehicle(truck);
        manager.stopAllVehicles();
        String expectedOutput = "Car is stopping.\nTruck is stopping.\n";
        assertEquals(expectedOutput, outContent.toString());
    }

    @Test
    public void testFindVehiclesByMake() {
        VehicleManager manager = new VehicleManager();
        Car carToyota = new Car("Toyota", "Corolla", 2020);
        Truck truckFord = new Truck("Ford", "F-150", 2019, 1000.0);
        Car carFord = new Car("Ford", "Focus", 2018);
        manager.addVehicle(carToyota);
        manager.addVehicle(truckFord);
        manager.addVehicle(carFord);
        ArrayList<Object> foundVehicles = manager.findVehiclesByMake("Ford");
        assertEquals(2, foundVehicles.size());
    }
}
