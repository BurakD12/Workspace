package Question3;

import java.util.ArrayList;

public class Library {
    private ArrayList<Book> books;
    private ArrayList<LibraryUser> users;

    Library() {
        books = new ArrayList<>();
        users = new ArrayList<>();
    }

    void add_Book(Book book) {
        books.add(book);
    }

    void remove_Book(String isbn){
        // for (int i = 0; i < books.size(); i++){
        //     if (books.get(i).getISBN() == isbn){
        //         books.remove(i);
        //     }
        // }

        for(Book b : books){
            if (b.getISBN().equalsIgnoreCase(isbn)) {
                books.remove(b);
            }
        }
    }

    void add_User(LibraryUser user){
        users.add(user);
    }

    void find_Books_By_Author(String author){
        for (Book a : books) {
            if (a.getAuthor().equalsIgnoreCase(author)) {
                System.out.println(a.getTitle());
            }
        }
    }

    void display_All_Books(){
        for (Book a : books) {
            System.out.println("The title of this book is: " + a.getTitle());
            System.out.println("The author of this book is: " + a.getAuthor());
            System.out.println("The ISBN of this book is: " + a.getISBN());
            System.out.println();
        }
    }
}
