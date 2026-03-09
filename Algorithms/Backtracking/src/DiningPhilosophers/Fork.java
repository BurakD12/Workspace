package DiningPhilosophers;

import java.util.concurrent.locks.ReentrantLock;

public class Fork {
    private final int id;
    private Philosopher holder;
    private final ReentrantLock lock;

    public Fork(int id) {
        this.id = id;
        this.lock = new ReentrantLock(true);
        this.holder = null;
    }

    public boolean pickUp(Philosopher philosopher) {
        if (lock.tryLock()) {
            holder = philosopher;
            return true;
        }
        return false;
    } 

    public void putDown(Philosopher philosopher) {
        if (lock.isHeldByCurrentThread()) {
            holder = null;
            lock.unlock();
        }
    }

    public int getId() {
        return id;
    }

    public Philosopher getHolder() {
        return holder;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    
}
