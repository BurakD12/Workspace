package Q1;

public class App {
    public static void main(String[] args) throws Exception {
        Book book = new Book();
        Book book2 = new Book("The Java Handbook", "John Doe", 39.99, 2020);
        Book copyOfBook2 = new Book(book2);
        
        book.displayBookDetails();
        book2.displayBookDetails();
        copyOfBook2.displayBookDetails();
    }
}
