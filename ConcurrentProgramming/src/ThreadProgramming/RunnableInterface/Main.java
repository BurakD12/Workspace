package ThreadProgramming.RunnableInterface;

public class Main {
    public static void main(String[] args) {
        Thread t1 = new Thread(new MyRunnable("t1"));
        Thread t2 = new Thread(new MyRunnable("t2"));
        t1.start();
        t2.start();
    }
}
