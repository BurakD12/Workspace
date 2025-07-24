package Exercise.Ex3;

import java.util.ArrayList;

public class LibraryManager {
    private ArrayList<Member> members;
    private ArrayList<Book> books;

    public LibraryManager(){
        this.members = new ArrayList<Member>();
        this.books = new ArrayList<Book>();
    }

    public ArrayList<Member> getMembers() {
        return members;
    }

    public void setMembers(ArrayList<Member> members) {
        this.members = members;
    }

    public ArrayList<Book> getBooks() {
        return books;
    }

    public void setBooks(ArrayList<Book> books) {
        this.books = books;
    }

    public void addMember(Member m){
        this.members.add(m);
    }

    public void viewAllMembers(){
        for (int i = 0; i < this.members.size(); i++) {
            this.members.get(i).displayDetails();
            System.out.println();
        }
    }

    public Member findMemberById(int id){
        for (int i = 0; i < this.members.size(); i++) {
            if (this.members.get(i).getId() == id){
                return this.members.get(i);
            }
        }
        return null;
    }

    public void updateMember(int id, String newName, String newEmail, String newType, int newMaxBooksAllowed){
        for (int i = 0; i < this.members.size(); i++) {
            if (this.members.get(i).getId() == id){
                this.members.get(i).setName(newName);
                this.members.get(i).setEmail(newEmail);
                this.members.get(i).setMembershipType(newType);
                this.members.get(i).setMaxBooksAllowed(newMaxBooksAllowed);
                return;
            }
        }
        System.out.println("Member with ID: " + id + " not found.");
    }

    public void deleteMember(int id){
        for (int i = 0; i < this.members.size(); i++) {
            if (this.members.get(i).getId() == id){
                this.members.remove(i);
                return;
            }
        }
        System.out.println("The member with ID: " + id + " couldn't be found.");
    }

    public void addBook(Book b){
        this.books.add(b);
    }

    void viewAllBooks(){
        for (int i = 0; i < this.books.size(); i++) {
            this.books.get(i).displayBookInfo();
            System.out.println();
        }
    }

    public Book findBookById(int bookId){
        for (int i = 0; i < this.books.size(); i++) {
            if (this.books.get(i).getBookId() == bookId){
                return this.books.get(i);
            }
        }
        return null;
    }

    public void updateBook(int bookId, String newTitle, String newAuthor){
        for (int i = 0; i < this.books.size(); i++) {
            if (this.books.get(i).getBookId() == bookId){
                this.books.get(i).setTitle(newTitle);
                this.books.get(i).setAuthor(newAuthor);
                return;
            }
        }
        System.out.println("Book with ID: " + bookId + " couldn't be found.");
    }

    public void deleteBook(int bookId){
        for (int i = 0; i < this.books.size(); i++) {
            if (this.books.get(i).getBookId() == bookId){
                this.books.remove(i);
                return;
            }
        }
        System.out.println("Book with ID: " + bookId + " couldn't be found.");
    }

    public void borrowBook(int bookId){
        for (int i = 0; i < this.books.size(); i++) {
            if (this.books.get(i).getBookId() == bookId){
                this.books.get(i).borrowBook();
                return;
            }
        }
        System.out.println("Book with ID: " + bookId + " couldn't be found.");
    }

    public void returnBook(int bookId){
        for (int i = 0; i < this.books.size(); i++) {
            if (this.books.get(i).getBookId() == bookId){
                this.books.get(i).returnBook();
                return;
            }
        }
        System.out.println("Book with ID: " + bookId + " couldn't be found.");
    }
}
