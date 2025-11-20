package LinearProbing;

import java.util.HashSet;
import java.util.Set;

public class MyMap<K, V> {
    private MyEntry<K, V>[] table;
    private int size;
    private static final int INITIAL_CAPACITY = 16;
    private static final float MAX_ALLOWED_LOAD_FACTOR = 0.75f;
    private static final float MIN_ALLOWED_LOAD_FACTOR = 0.25f;
    private static final int RESIZE_FACTOR = 2;
    
    public MyMap() {
        this.table = new MyEntry[INITIAL_CAPACITY];
        this.size = 0;
        for (int i = 0; i < table.length; i++) {
            table[i] = new MyEntry<>();
        }
    }

    public void put(K key, V value) {
        int position = hash(key);
        MyEntry<K, V> newEntry = new MyEntry<K, V>(key, value);
        if (table[position].getStatus() != SlotStatus.OCCUPIED) {
            table[position] = newEntry;
            size++;
        } else {
            if (table[position].getKey().equals(key)){
                table[position] = newEntry;
            } else {
                while (table[position].getStatus() == SlotStatus.OCCUPIED) {
                    if (table[position].getKey().equals(key)) {
                        table[position] = newEntry;
                        break;
                    } else {
                        position++;
                        position = position % table.length;
                    }
                }
                table[position] = newEntry;
                size++;
            }
        }
        float loadFactor = (1.0f * size) / table.length;
        if (loadFactor >= MAX_ALLOWED_LOAD_FACTOR) {
            size = 0;
            resize(table.length * RESIZE_FACTOR);
        }
    }
    

    public V get(K key) {
        int position = hash(key);        
        while (table[position].getStatus() != SlotStatus.EMPTY) {
            if (table[position].getStatus() == SlotStatus.OCCUPIED) {
                if (table[position].getKey().equals(key)) {
                    return table[position].getValue();
                } else {
                    position++;
                    position = position % table.length;
                }
            } else {
                position++;
                position = position % table.length;                
            }
        }
        return null;
    }

    public V remove(K key) {
        int position = hash(key);
        V removedVal = null;
        while (table[position].getStatus() != SlotStatus.EMPTY) {
            if (table[position].getStatus() == SlotStatus.OCCUPIED) {
                if (table[position].getKey().equals(key)) {
                    removedVal = table[position].getValue();
                    table[position] = new MyEntry<>();
                    table[position].setDeleted(); 
                    size--;
                break;
                } else {
                    position++;
                    position = position % table.length;
                }  
            } else {
                position++;
                position = position % table.length;
            }
        
        }
        float loadFactor = (1.0f * size) / table.length;
        if (loadFactor >= MAX_ALLOWED_LOAD_FACTOR) {
            size = 0;
            resize(table.length * RESIZE_FACTOR);
        }
        return removedVal;
    }

    public int size() {
        return size;
    }

    public void resize(int newCapacity) {
        Set<MyEntry<K, V>> entries = new HashSet<>();
        for (int i = 0; i < table.length; i++) {
            if (table[i].getStatus() == SlotStatus.OCCUPIED){
                MyEntry<K, V> currEntry = table[i];
                entries.add(currEntry);
            }
        }

        this.table = new MyEntry[newCapacity];
        for (int i = 0; i < table.length; i++) {
            table[i] = new MyEntry<>();
        }

        for (MyEntry<K, V> entry : entries) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public boolean containsKey(K key) {
        int position = hash(key);        
        while (table[position].getStatus() != SlotStatus.EMPTY) {
            if (table[position].getStatus() == SlotStatus.OCCUPIED){
                if (table[position].getKey().equals(key)) {
                    return true;
                } else {
                    position++;
                    position = position % table.length;
                }
            } else {
                position++;
                position = position % table.length;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        if (size > 0) {
            return false;
        }
        return true;
    }

    public int hash(K key) {
        int finalKey = key.hashCode() % table.length;
        return finalKey;
    }
}