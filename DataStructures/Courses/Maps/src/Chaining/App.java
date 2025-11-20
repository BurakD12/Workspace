package Chaining;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello".hashCode());
        System.out.println("Hello".hashCode());

        String str1 = new String("Hello");
        String str2 = new String("Hello");

        System.out.println(str1.hashCode());
        System.out.println(str2.hashCode());
    }
}
