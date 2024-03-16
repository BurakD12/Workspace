package Question5;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Library{
    private ArrayList<Item> items;

    Library(){
        this.items = new ArrayList<>();
    }

    void addItem(Item item){
        this.items.add(item);
    }

    void displayDetails() {
        for (Item x : items) {
            x.displayDetails();
        }
    }

    void updateItem(String title, Item newItem){
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getTitle().equals(title)) {
                items.add(i, newItem);
            }
        }
    }

    void deleteItem(String title){
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getTitle().equals(title)) {
                items.remove(i);
            }
        }
    }

    List<Item> findItemsByAuthor(String Author){
        ArrayList<Item> finalList = new ArrayList<>();
        for (Item item : items) {
            if (item instanceof Book && ((Book)item).getAuthor().contains(Author)) {
                finalList.add(item);
            }
        }
        return finalList;
    }

    public Map<String, Integer> countItemsByType(){
        Map<String, Integer> countMap = new HashMap<>();
        countMap.put("Book", 0);
        countMap.put("CD", 0);
        countMap.put("DVD", 0);

        for (Item item : items) {
            if(item instanceof Book){
                countMap.put("Book", countMap.get("Book") + 1);
            }
            else if(item instanceof CD){
                countMap.put("CD", countMap.get("CD") + 1);
            }
            else{
                countMap.put("DVD", countMap.get("DVD") + 1);
            }
        }
        return countMap;
    }

    ArrayList<Item> listAvailableItems(){
        ArrayList<Item> finalList = new ArrayList<>();
        for (Item item : items) {
            if(item.isAvailable()){
                finalList.add(item);
            }
        }
        return finalList;
    }



}
