package DiningPhilosophers;

import java.util.Random;

public class Philosopher extends Thread {
    private final int id;
    private final Fork rightFork;
    private final Fork leftFork;
    private final Random random;
    private volatile boolean running;
    private int eatCount;
    private int thinkCount;
    private State state;

    public enum State {
        THINKING,
        HUNGRY,
        EATING
    }

    public Philosopher(int id, Fork rightFork, Fork leftFork) {
        this.id = id;
        this.rightFork = rightFork;
        this.leftFork = leftFork;
        this.random = new Random();
        this.running = true;
        this.eatCount = 0;
        this.thinkCount = 0;
        this.state = state.THINKING;
    }

    @Override
    public void run() {
        while (true) {
            think();
            eat();
        }
    }

    private void eat() {
        state = state.HUNGRY;

        // Deadlock prevention: pick up the lower-numbered fork first
        Fork firstFork;
        Fork secondFork;
        if (leftFork.getId() < rightFork.getId()) {
            firstFork = leftFork;
            secondFork = rightFork;
        } else {
            firstFork = rightFork;
            secondFork = leftFork;
        }

        // Try to pcik up first fork
        while (!firstFork.pickUp(this) && running) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (!running) {
            firstFork.putDown(this);
            return;
        }

        while (!secondFork.pickUp(this) && running) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (!running) {
            secondFork.putDown(this);
            return;
        }

        // Both forks acquired, start eating
        state = state.EATING;
        eatCount++;

        try {
            int eatTime = 1000 + random.nextInt(2000);
            Thread.sleep(eatTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Put down forks
            firstFork.putDown(this);
            secondFork.putDown(this);
        }
    }

    private void think() {
        state = state.THINKING;
        thinkCount++;
        int thinkTime = 1000 + random.nextInt(2000);
        try {
            Thread.sleep(thinkTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getPhilosopherId() {
        return id;
    }

    public int getEatCount() {
        return eatCount;
    }

    public int getThinkCount() {
        return thinkCount;
    }

    public State getPhilosopherState() {
        return state;
    }
    
    public void stopPhilosopher() {
        running = false;
        this.interrupt();
    }
}
