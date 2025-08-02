import java.util.ArrayList;

public class MyArrayList {

    public static void main(String[] args) {
        // 1. How to create an array list
        ArrayList<String> list = new ArrayList<>();

        // 2. How to add elements to the array list
        list.add("Apple");
        list.add("Banana");
        list.add("Cherry");

        // 3. How to access elements in the array list
        System.out.println("First element: " + list.get(0)); // Output: Apple
        System.out.println("Second element: " + list.get(1)); // Output: Banana

        // 4. How to remove an element from the array list
        list.remove("Banana");
        System.out.println("After removing Banana: " + list);

        // 5. How to check the size of the array list
        System.out.println("Size of the list: " + list.size()); // Output: 2

        // 6. How to iterate over the elements in the array list
        System.out.println("Iterating over the list:");
        for (String fruit : list) {
            System.out.println(fruit);
        }   

        // 7. How to check if the array list contains an element
        if (list.contains("Apple")) {
            System.out.println("The list contains Apple.");
        } else {
            System.out.println("The list does not contain Apple.");
        }

        // 8. How to clear the array list
        list.clear();
        System.out.println("After clearing the list: " + list);

        // 9. How to check if the array list is empty
        if (list.isEmpty()) {
            System.out.println("The list is empty.");
        } else {
            System.out.println("The list is not empty.");
        }

        // 10. How to convert an array list to an array
        String[] array = list.toArray(new String[0]);
        System.out.println("Array from list: " + java.util.Arrays.toString(array));

        // 11. How to sort the array list
        list.add("Orange");
        list.add("Grapes");
        list.sort(String::compareTo);
        System.out.println("Sorted list: " + list);
        
        // 12. How to find the index of an element
        int index = list.indexOf("Orange");
        System.out.println("Index of Orange: " + index); // Output: Index of Orange 

        // 13. How to check if the array list is equal to another array list
        ArrayList<String> anotherList = new ArrayList<>();
        anotherList.add("Grapes");
        anotherList.add("Orange");
        boolean isEqual = list.equals(anotherList);
        System.out.println("Is the list equal to another list? " + isEqual); // Output: false

        // 14. How to get a sublist from the array list
        ArrayList<String> sublist = new ArrayList<>(list.subList(0, 2));
        System.out.println("Sublist: " + sublist); // Output: Sublist with first two elements   

        // 15. How to replace an element in the array list
        list.set(0, "Pineapple");
        System.out.println("After replacing first element: " + list); // Output: List with Pineapple
        
    }
}