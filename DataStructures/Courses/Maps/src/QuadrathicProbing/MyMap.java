package QuadrathicProbing;

import java.util.HashSet;
import java.util.Set;

public class MyMap<K, V> {
    private MyEntry<K, V>[] table;
    private int size;
    private static final int INITIAL_CAPACITY = 16;
    private static final float MAX_ALLOWED_LOAD_FACTOR = 0.75f;
    private static final float MIN_ALLOWED_LOAD_FACTOR = 0.25f;
    private static final int RESIZE_FACTOR = 2;
    private static final int COLLISION_CONSTANT_1 = 17;
    private static final int COLLISION_CONSTANT_2 = 23;
    
    public MyMap() {
        this.table = new MyEntry[INITIAL_CAPACITY];
        this.size = 0;
        for (int i = 0; i < table.length; i++) {
            table[i] = new MyEntry<>();
        }
    }

    public void put(K key, V value) {
        int i = 0;
        int position = hash(key, i);
        MyEntry<K, V> newEntry = new MyEntry<K, V>(key, value);
        int firstDeletedSlot = -1;
        
        // Probe until we find EMPTY or a matching key
        // Limit iterations to table.length to avoid infinite loops
        while (i < table.length && table[position].getStatus() != SlotStatus.EMPTY) {
            if (table[position].getStatus() == SlotStatus.OCCUPIED) {
                // Found matching key - overwrite it
                if (table[position].getKey().equals(key)) {
                    table[position] = newEntry;
                    return; // Don't increment size for overwrite
                }
            } else if (table[position].getStatus() == SlotStatus.DELETED) {
                // Remember first deleted slot for potential reuse
                if (firstDeletedSlot == -1) {
                    firstDeletedSlot = position;
                }
            }
            i++;
            position = hash(key, i);
        }
        
        // Key not found - insert at first deleted slot or current empty slot
        if (firstDeletedSlot != -1) {
            table[firstDeletedSlot] = newEntry;
        } else if (i < table.length) {
            // Found an empty slot
            table[position] = newEntry;
        } else {
            // Table is full (no empty slots found) - need to resize first
            size = 0;
            resize(table.length * RESIZE_FACTOR);
            put(key, value); // Retry insertion after resize
            return;
        }
        size++;
        
        float loadFactor = (1.0f * size) / table.length;
        if (loadFactor >= MAX_ALLOWED_LOAD_FACTOR) {
            size = 0;
            resize(table.length * RESIZE_FACTOR);
        }
    }
    

    public V get(K key) {
        int i = 0;
        int position = hash(key, i);        
        while (i < table.length && table[position].getStatus() != SlotStatus.EMPTY) {
            if (table[position].getStatus() == SlotStatus.OCCUPIED) {
                if (table[position].getKey().equals(key)) {
                    return table[position].getValue();
                } else {
                    i++;
                    position = hash(key, i);
                }
            } else {
                i++;
                position = hash(key, i);                
            }
        }
        return null;
    }

    public V remove(K key) {
        int i = 0;
        int position = hash(key, i);
        V removedVal = null;
        while (i < table.length && table[position].getStatus() != SlotStatus.EMPTY) {
            if (table[position].getStatus() == SlotStatus.OCCUPIED) {
                if (table[position].getKey().equals(key)) {
                    removedVal = table[position].getValue();
                    table[position] = new MyEntry<>();
                    table[position].setDeleted(); 
                    size--;
                break;
                } else {
                    i++;
                    position = hash(key, i);
                }  
            } else {
                i++;
                position = hash(key, i);
            }
        
        }
        float loadFactor = (1.0f * size) / table.length;
        if (loadFactor <= MIN_ALLOWED_LOAD_FACTOR && table.length > INITIAL_CAPACITY) {
            size = 0;
            resize(table.length / RESIZE_FACTOR);
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
        int i = 0;
        int position = hash(key, i);        
        while (i < table.length && table[position].getStatus() != SlotStatus.EMPTY) {
            if (table[position].getStatus() == SlotStatus.OCCUPIED){
                if (table[position].getKey().equals(key)) {
                    return true;
                } else {
                    i++;
                    position = hash(key, i);
                }
            } else {
                i++;
                position = hash(key, i);
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

    public int hash(K key, int iterator) {
        long hashValue = Math.abs(key.hashCode());
        long offset = COLLISION_CONSTANT_1 * (long)iterator + COLLISION_CONSTANT_2 * (long)iterator * (long)iterator;
        long result = (hashValue + offset) % table.length;
        // Ensure non-negative result (Java's % can return negative values)
        if (result < 0) {
            result += table.length;
        }
        return (int)result;
    }
}