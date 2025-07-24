package Courses;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Scanner;

// Simple serializable class for demonstration
class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private int age;
    
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + "}";
    }
    
    // Getters
    public String getName() { return name; }
    public int getAge() { return age; }
}

public class App {

    static void clearTerminal() throws Exception {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } else {
            new ProcessBuilder("clear").inheritIO().start().waitFor();
        }
    }
    
    public static void main(String[] args) throws Exception {
        clearTerminal();

        // try {
        //     System.out.println(5/0);
        // } catch (ArithmeticException e) {
        //     System.out.println("Caught an arithmetic exception: " + e.getMessage());
        // } 

        // System.out.println("Hello, World!");

        // try {
        //     throw new MyCustomEx("This is a custom exception message.");
        // } catch (MyCustomEx e) {
        //     System.out.println("Caught a custom exception: " + e.getMessage());
        // }

        // How to write into a text file
        File file = new File("src/example.txt");
        
        try{
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            System.out.println("An error occurred while creating the file: " + e.getMessage());
        }

        FileWriter fileWriter = new FileWriter(file, true);
        try {
            fileWriter.write("Hello, World!\n");
            fileWriter.write("This is a test.\n");
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file: " + e.getMessage());
        } finally {
            try {
                fileWriter.close();
            } catch (IOException e) {
                System.out.println("An error occurred while closing the file writer: " + e.getMessage());
            }
        }

        // How to read from a text file
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                System.out.println(line);
            }
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred while reading the file: " + e.getMessage());
        } finally {
            try {
                scanner.close();
            } catch (Exception e) {
                System.out.println("An error occurred while closing the scanner: " + e.getMessage());
            }
        }

        // How to write into a binary file

        File binaryFile = new File("src/example.bin");
        try {
            if (!binaryFile.exists()) {
                binaryFile.createNewFile();
            }
        } catch (IOException e) {
            System.out.println("An error occurred while creating the binary file: " + e.getMessage());
        }

        // Method 1: Writing objects using ObjectOutputStream (for Serializable objects)
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(binaryFile))) {
            // Create some sample objects
            Person person1 = new Person("Alice", 25);
            Person person2 = new Person("Bob", 30);
            
            HashMap<String, String> binaryData = new HashMap<>();
            binaryData.put("key1", "value1");
            binaryData.put("key2", "value2");
            
            // Write objects to binary file
            oos.writeObject(person1);
            oos.writeObject(person2);
            oos.writeObject(binaryData);
            
            System.out.println("Objects written to binary file successfully!");
            
        } catch (IOException e) {
            System.out.println("An error occurred while writing to binary file: " + e.getMessage());
        }
        
        // How to read from a binary file
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(binaryFile))) {
            // Read objects from binary file
            Person readPerson1 = (Person) ois.readObject();
            Person readPerson2 = (Person) ois.readObject();
            @SuppressWarnings("unchecked")
            HashMap<String, String> readData = (HashMap<String, String>) ois.readObject();
            
            System.out.println("Objects read from binary file:");
            System.out.println("Person 1: " + readPerson1);
            System.out.println("Person 2: " + readPerson2);
            System.out.println("HashMap: " + readData);
            
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("An error occurred while reading from binary file: " + e.getMessage());
        }

        // Method 2: Writing raw bytes using FileOutputStream
        File rawBinaryFile = new File("src/raw_example.bin");
        try (FileOutputStream fos = new FileOutputStream(rawBinaryFile)) {
            String data = "This is binary data as bytes";
            byte[] dataBytes = data.getBytes();
            fos.write(dataBytes);
            
            System.out.println("Raw bytes written to binary file successfully!");
            
        } catch (IOException e) {
            System.out.println("An error occurred while writing raw bytes: " + e.getMessage());
        }

        // Reading raw bytes
        try (FileInputStream fis = new FileInputStream(rawBinaryFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead = fis.read(buffer);
            String readData = new String(buffer, 0, bytesRead);
            
            System.out.println("Raw bytes read from file: " + readData);
            
        } catch (IOException e) {
            System.out.println("An error occurred while reading raw bytes: " + e.getMessage());
        }
    }
}
