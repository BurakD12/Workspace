package DiningPhilosophers;

import java.util.Scanner;

/**
 * Main class for the Dining Philosophers simulation.
 * Creates philosophers and forks, manages the simulation, and displays status.
 */
public class DiningPhilosophers {
    private static final int NUM_PHILOSOPHERS = 5;
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_CLEAR = "\033[H\033[2J";
    
    // Colors for different states
    private static final String ANSI_THINKING = "\u001B[34m";  // Blue
    private static final String ANSI_HUNGRY = "\u001B[33m";    // Yellow
    private static final String ANSI_EATING = "\u001B[32m";    // Green
    private static final String ANSI_TITLE = "\u001B[36m";     // Cyan
    private static final String ANSI_BORDER = "\u001B[35m";    // Magenta

    private Philosopher[] philosophers;
    private Fork[] forks;
    private volatile boolean running;
    private Thread displayThread;

    public DiningPhilosophers() {
        philosophers = new Philosopher[NUM_PHILOSOPHERS];
        forks = new Fork[NUM_PHILOSOPHERS];
        running = true;

        // Create forks
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            forks[i] = new Fork(i);
        }

        // Create philosophers
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            Fork leftFork = forks[i];
            Fork rightFork = forks[(i + 1) % NUM_PHILOSOPHERS];
            philosophers[i] = new Philosopher(i, leftFork, rightFork);
        }
    }

    public void start() {
        System.out.println(ANSI_CLEAR);
        printHeader();
        
        // Start all philosophers
        for (Philosopher philosopher : philosophers) {
            philosopher.start();
        }

        // Start display thread
        displayThread = new Thread(this::displayLoop);
        displayThread.setDaemon(true);
        displayThread.start();

        // Wait for user input to stop
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n" + ANSI_BOLD + "Press ENTER to stop the simulation..." + ANSI_RESET);
        scanner.nextLine();
        
        stop();
        scanner.close();
    }

    private void stop() {
        running = false;
        
        // Stop all philosophers
        for (Philosopher philosopher : philosophers) {
            philosopher.stopPhilosopher();
        }

        // Wait for philosophers to finish
        for (Philosopher philosopher : philosophers) {
            try {
                philosopher.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("\n" + ANSI_BOLD + ANSI_TITLE + "Simulation stopped!" + ANSI_RESET);
        printFinalStats();
    }

    private void displayLoop() {
        while (running) {
            try {
                Thread.sleep(100); // Update every 100ms
                System.out.print(ANSI_CLEAR);
                printHeader();
                printTable();
                printStatus();
                Thread.sleep(900); // Total 1 second between updates
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void printHeader() {
        String border = "═".repeat(70);
        System.out.println(ANSI_BORDER + ANSI_BOLD + border + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_TITLE + 
            "                    🍽️  DINING PHILOSOPHERS PROBLEM  🍽️" + ANSI_RESET);
        System.out.println(ANSI_BORDER + ANSI_BOLD + border + ANSI_RESET + "\n");
    }

    private void printTable() {
        System.out.println(ANSI_BOLD + "           Circular Table Layout:" + ANSI_RESET);
        System.out.println();
        
        // Circular table representation
        //        P0
        //    F4      F0
        // P4            P1
        //    F3      F1
        //        P2
        //    F2
        
        System.out.println("                    " + getPhilosopherStatus(0));
        System.out.println();
        System.out.println("            🍴 F4        🍴 F0");
        System.out.println();
        System.out.println("     " + getPhilosopherStatus(4) + "                    " + getPhilosopherStatus(1));
        System.out.println();
        System.out.println("            🍴 F3        🍴 F1");
        System.out.println();
        System.out.println("                    " + getPhilosopherStatus(2));
        System.out.println();
        System.out.println("                    🍴 F2");
        System.out.println();
    }

    private String getPhilosopherStatus(int index) {
        Philosopher p = philosophers[index];
        String stateColor;
        String stateIcon;
        
        switch (p.getPhilosopherState()) {
            case THINKING:
                stateColor = ANSI_THINKING;
                stateIcon = "💭";
                break;
            case HUNGRY:
                stateColor = ANSI_HUNGRY;
                stateIcon = "🤤";
                break;
            case EATING:
                stateColor = ANSI_EATING;
                stateIcon = "🍽️";
                break;
            default:
                stateColor = ANSI_RESET;
                stateIcon = "❓";
        }
        
        return String.format("%s[P%d] %s %s%s", 
            stateColor, p.getPhilosopherId(), stateIcon, p.getPhilosopherState(), ANSI_RESET);
    }

    private void printStatus() {
        System.out.println();
        System.out.println(ANSI_BOLD + "Detailed Status:" + ANSI_RESET);
        System.out.println("─".repeat(70));
        
        for (Philosopher p : philosophers) {
            String stateColor;
            switch (p.getPhilosopherState()) {
                case THINKING:
                    stateColor = ANSI_THINKING;
                    break;
                case HUNGRY:
                    stateColor = ANSI_HUNGRY;
                    break;
                case EATING:
                    stateColor = ANSI_EATING;
                    break;
                default:
                    stateColor = ANSI_RESET;
            }
            
            System.out.printf("%sPhilosopher %d: %s%-10s%s | Eaten: %3d times | Thought: %3d times%s%n",
                ANSI_BOLD, p.getPhilosopherId(), stateColor, p.getPhilosopherState(), ANSI_RESET, 
                p.getEatCount(), p.getThinkCount(), ANSI_RESET);
        }
        
        System.out.println("─".repeat(70));
    }

    private void printFinalStats() {
        System.out.println("\n" + ANSI_BOLD + ANSI_TITLE + "Final Statistics:" + ANSI_RESET);
        System.out.println("═".repeat(70));
        
        int totalEats = 0;
        int totalThinks = 0;
        
        for (Philosopher p : philosophers) {
            System.out.printf("Philosopher %d: Eaten %d times, Thought %d times%n",
                p.getPhilosopherId(), p.getEatCount(), p.getThinkCount());
            totalEats += p.getEatCount();
            totalThinks += p.getThinkCount();
        }
        
        System.out.println("─".repeat(70));
        System.out.printf(ANSI_BOLD + "Total: %d eating sessions, %d thinking sessions%n" + ANSI_RESET,
            totalEats, totalThinks);
        System.out.println("═".repeat(70));
    }

    public static void main(String[] args) {
        DiningPhilosophers simulation = new DiningPhilosophers();
        simulation.start();
    }
}

