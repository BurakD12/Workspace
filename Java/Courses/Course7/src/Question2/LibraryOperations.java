package Question2;

import java.util.ArrayList;

public interface LibraryOperations {
    void addBook(Book book);
    void updateBook(int id, Book book);
    void deleteBook(int id);
    ArrayList<Book> listBooks();
}
