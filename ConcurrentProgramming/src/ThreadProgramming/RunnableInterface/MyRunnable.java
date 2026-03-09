package ThreadProgramming.RunnableInterface;

public class MyRunnable implements Runnable{
    public MyRunnable(String name){
        super();
    }

    @Override
    public void run(){
        for (int i = 0; i < 10; i++) {
            System.out.println(Thread.currentThread().getName() + " - Count: " + (i + 1));
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
        }
    }
}
