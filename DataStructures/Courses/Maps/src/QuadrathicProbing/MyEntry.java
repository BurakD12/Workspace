package QuadrathicProbing;

public class MyEntry<K, V> {
    private K key;
    private V value;
    SlotStatus status;

    public MyEntry() {
        this.key = null;
        this.value = null;
        this.status = SlotStatus.EMPTY;
    }

    public MyEntry(K key, V value) {
        this.key = key;
        this.value = value;
        this.status = SlotStatus.OCCUPIED;
    }

    public K getKey() {
        return this.key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public V getValue() {
        return this.value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public SlotStatus getStatus() {
        return this.status;
    }

    public void setOccupied() {
        this.status = SlotStatus.OCCUPIED;   
    }

    public void setDeleted() {
        this.status = SlotStatus.DELETED;   
    }

    @Override
    public String toString() {
        return this.key + ", " + this.value;
    }
}

enum SlotStatus {
    EMPTY,
    OCCUPIED,
    DELETED
}