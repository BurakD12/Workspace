package Question2;

import java.util.ArrayList;

public class LibraryManager implements LibraryOperations{
    private ArrayList<Book> books;

    LibraryManager(){
        this.books = new ArrayList<Book>();
    }

    @Override
    public void addBook(Book book) {
        books.add(book);
    }

    @Override
    public void deleteBook(int id) {
        for (int i = 0; i < books.size(); i++){
            if (books.get(i).getId() == id){
                books.remove(i);
            }
        }
    }

    @Override
    public ArrayList<Book> listBooks() {
        return new ArrayList<>(books);
    }

    @Override
    public void updateBook(int id, Book book) {
        for (int i = 0; i < books.size(); i++){
            if (books.get(i).getId() == id){
                books.remove(i);
                books.add(i, book);
            }
        }
    }

    
}
