package Question2;

import java.util.List;

public class LibraryApplication {
    public static void main(String[] args) {
        LibraryManager libraryManager = new LibraryManager();

        // Adding books
        libraryManager.addBook(new FictionBook(1, "The Great Gatsby", "F. Scott Fitzgerald", 15.99));
        libraryManager.addBook(new NonFictionBook(2, "A Brief History of Time", "Stephen Hawking", 18.99));

        // Updating a book
        libraryManager.updateBook(1, new FictionBook(1, "The Great Gatsby", "F. Scott Fitzgerald", 12.99));

        // Deleting a book
        libraryManager.deleteBook(2);

        // Listing all books
        List<Book> books = libraryManager.listBooks();
        System.out.println("Books in the library:");
        for (Book book : books) {
            System.out.println(book);
        }
    }
}
