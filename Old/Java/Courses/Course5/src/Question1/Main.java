package Question1;

public class Main {
    public static void main(String[] args) {
        // Creating a book object using the no-argument constructor
        Book book1 = new Book();
        System.out.println("Book 1 Details (Before borrowing):");
        book1.dispInfo();
        
        // Borrowing book1
        book1.borrowBook();
        System.out.println("Book 1 Details (After borrowing):");
        book1.dispInfo();
        
        // Creating a book object using the parameterized constructor
        Book book2 = new Book("The Great Gatsby", "F. Scott Fitzgerald", 1925);
        System.out.println("\nBook 2 Details (Before borrowing):");
        book2.dispInfo();
        
        // Borrowing book2
        book2.borrowBook();
        System.out.println("Book 2 Details (After borrowing):");
        book2.dispInfo();

    }
}
