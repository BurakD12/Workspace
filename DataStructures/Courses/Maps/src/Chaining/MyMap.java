package Chaining;

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
    }

    public void put(K key, V value) {
        int position = hash(key);
        MyEntry<K, V> newEntry = new MyEntry<>(key, value);
        if (table[position] == null) {
            table[position] = newEntry;
            size++;
        } else {
            MyEntry<K, V> currentNode = table[position];
            while (currentNode.getNext() != null) {
                if (currentNode.getKey() == key) {
                    currentNode.setValue(value);
                    return;
                }
                currentNode = currentNode.getNext();
            }
            if (currentNode.getKey().equals(key)) {
                currentNode.setValue(value);
            } else {
                currentNode.setNext(newEntry);
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
        MyEntry<K, V> currentNode = table[position];
        while (currentNode != null) {
            if (currentNode.getKey().equals(key)) {
                return currentNode.getValue();
            } else {
                currentNode = currentNode.getNext();
            }
        }
        return null;
    }

    public V remove(K key) {
        int position = hash(key);
        V removedValue = null;
        MyEntry<K, V> currentNode = table[position];
        if (currentNode == null){
            return removedValue;
        } else if (currentNode.getKey().equals(key)) {
            removedValue = currentNode.getValue();
            table[position] = currentNode.getNext();
            size--;
        } else {
            while (currentNode.getNext() != null) {
                if (currentNode.getNext().getKey().equals(key)) {
                    removedValue = currentNode.getNext().getValue();
                    currentNode.setNext(currentNode.getNext().getNext());
                    size--;
                    break;
                } else {
                    currentNode = currentNode.getNext();
                }
            }
        }
        float loadFactor = (1.0f * size) / table.length;
        if (loadFactor <= MIN_ALLOWED_LOAD_FACTOR && table.length > INITIAL_CAPACITY) {
            size = 0;
            resize(table.length / RESIZE_FACTOR);
        }
        return removedValue;
    }

    public int size() {
        return size;
    }

    public void resize(int newCapacity) {
        Set<MyEntry<K, V>> entries = new HashSet<>();
        for (int i = 0; i < table.length; i++) {
            if (table[i] != null) {
                MyEntry<K, V> currentNode = table[i];
                while (currentNode != null) {
                    entries.add(currentNode);
                    currentNode = currentNode.getNext();
                }
            }
        }

        this.table = new MyEntry[newCapacity];

        for (MyEntry<K, V> entry : entries) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public boolean containsKey(K key) {
        int position = hash(key);
        MyEntry<K, V> currentNode = table[position];
        while (currentNode != null) {
            if (currentNode.getKey().equals(key)) {
                return true;
            }
            currentNode = currentNode.getNext();
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