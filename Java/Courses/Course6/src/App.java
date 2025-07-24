import java.util.HashMap;
import java.util.Map.Entry;

public class App {
    public static void main(String[] args) throws Exception {
        // How to create a HashMap in Java
        HashMap<String, String> country_capitals = new HashMap<>();

        // Adding key-value pairs to the HashMap
        country_capitals.put("USA", "Washington, D.C.");
        country_capitals.put("Canada", "Ottawa");
        country_capitals.put("Mexico", "Mexico City");
        country_capitals.put("UK", "London");
        country_capitals.put("France", "Paris");

        // Retrieving a value from the HashMap
        // String capitalOfUSA = country_capitals.get("USA");
        // System.out.println("The capital of USA is: " + capitalOfUSA);

        // // Checking if a key exists in the HashMap
        // if (country_capitals.containsKey("Turkey")) {
        //     System.out.println("Canada's capital is: " + country_capitals.get("Canada"));
        // } else {
        //     System.out.println("Capital of Canada not found.");
        // }

        // // Iterating over the HashMap
        // System.out.println("List of countries and their capitals:");
        // for (String country : country_capitals.keySet()) {
        //     System.out.println(country + ": " + country_capitals.get(country));
        // }

        // // Iterating using entrySet
        // System.out.println("Using entrySet to iterate:");
        // for (Entry<String, String> entry : country_capitals.entrySet()) {
        //     System.out.println(entry.getKey() + ": " + entry.getValue());
        // }

        // Removing a key-value pair from the HashMap
        country_capitals.remove("Mexico");
        System.out.println("After removing Mexico, the capitals are:");
        for (String country : country_capitals.keySet()) {
            System.out.println(country + ": " + country_capitals.get(country));
        }

        // Checking the size of the HashMap
        System.out.println("Total countries in the map: " + country_capitals.size());

        // Clearing the HashMap
        country_capitals.clear();
        System.out.println("After clearing, the map size is: " + country_capitals.size());

        // Checking if the HashMap is empty
        if (country_capitals.isEmpty()) {
            System.out.println("The HashMap is empty now.");
        } else {
            System.out.println("The HashMap still has elements.");
        }

        // Example of using a HashMap with custom objects
        HashMap<Integer, String> studentGrades = new HashMap<>();
        studentGrades.put(101, "A");
        studentGrades.put(102, "B+");
        studentGrades.put(103, "A-");
        System.out.println("Student Grades:");
        for (Entry<Integer, String> entry : studentGrades.entrySet()) {
            System.out.println("Student ID: " + entry.getKey() + ", Grade: " + entry.getValue());
        }
    }
}
