package Question4;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Inventory{
    ArrayList<Item> items;

    Inventory() {
        this.items = new ArrayList<>();
    }

    boolean addItem(Item item){
        for (Item x : items){
            if (item.getId() == x.getId()){
                System.out.println("This item already exists!");
                return false;
            }
        }
        items.add(item);
        return true;
    }

    Item getItem(int id){
        for (Item x : items){
            if (x.getId() == id){
                return x;
            }
        }
        return null;
    }

    boolean updateItem(int id, Item item){
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == id) {
                items.set(i, item);
                return true;
            }
        }
        return false;               
    }

    boolean deleteItem(int id){
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == id) {
                items.remove(i);
                return true;
            }
        }
        return false;
    }
    
    ArrayList<Item> getAllItems(){
        return items;
    }

    public <T extends Item> List<T> getItemsByType(Class<T> type) {
        return items.stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
    }

    ArrayList<Item> searchItemsByTitle(String title){
        ArrayList<Item> subList = new ArrayList<>();
        for (Item x : items){
            if (x.getTitle().contains(title)){
                subList.add(x);
            }
        }
        return subList;
    }

    ArrayList<Item> sortItemsByPrice() {
        ArrayList<Item> sortedList = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i); // 50
            if (sortedList.isEmpty())
                sortedList.add(item);
            else {
                boolean isInserted = false;
                for (int j = 0; j < sortedList.size(); j++) {
                    Item itemTarget = sortedList.get(j);
                    if (item.getPrice() <= itemTarget.getPrice()) {
                        sortedList.add(j, item);
                        isInserted = true;
                        break;
                    }
                }
                if (!isInserted) {
                    sortedList.add(item);
                }
            }
        }
        return sortedList;
    }

    ArrayList<Item> sortItemsById() {
        ArrayList<Item> sortedList = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i); // 50
            if (sortedList.isEmpty())
                sortedList.add(item);
            else {
                boolean isInserted = false;
                for (int j = 0; j < sortedList.size(); j++) {
                    Item itemTarget = sortedList.get(j);
                    if (item.getId() <= itemTarget.getId()) {
                        sortedList.add(j, item);
                        isInserted = true;
                    }
                }
                if (!isInserted) {
                    sortedList.add(item);
                }
            }
        }
        return sortedList;
    }

    double getTotalInventoryValue(){
        double sum = 0;
        for (Item item : this.items) {
            sum += item.price;
        }
        return sum;
    }

    boolean updatePrice(int id, double newPrice){
        for (int i = 0; i < this.items.size(); i++) {
            if (items.get(i).id == id){
                items.get(i).price = newPrice;
                return true;
            }
        }
        return false;
    }

}
