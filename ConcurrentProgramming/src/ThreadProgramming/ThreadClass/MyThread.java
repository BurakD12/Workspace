package ThreadProgramming.ThreadClass;

public class MyThread extends Thread{
    public MyThread(String name) {
        super(name);
    }

    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            System.out.println(getName() + " " + i);
            try {
                sleep(500);
            } catch (Exception e) {
                
            }
        }
    }
}
