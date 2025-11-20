package Chaining;


public class MyEntry<K, V> {
    private K key;
    private V value; 
    private MyEntry<K, V> next; // For chaining in case of collisions

    public MyEntry(K key, V value) {
        this.key = key;
        this.value = value;
        this.next = null;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public MyEntry<K, V> getNext() {
        return next;
    }

    public void setNext(MyEntry<K, V> next) {
        this.next = next;
    }

    @Override
    public String toString() {
        return "MyEntry{" +
                "key=" + key +
                ", value=" + value +
                '}';    
    }
}