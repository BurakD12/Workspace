package Question3;

public class Main {
    public static void main(String[] args) {
        Library Lib = new Library();
        Lib.add_Book(new Book("Book1", "AuthorA", "1111"));
        Lib.add_Book(new Book("Book2", "AuthorB", "2222"));
        Lib.add_Book(new Book("Book3", "AuthorA", "3333"));

        Lib.add_User(new LibraryUser("User1", "1234"));
        Lib.add_User(new LibraryUser("User2", "1235"));
        Lib.add_User(new LibraryUser("User3", "1236"));

        System.out.println("List of books: ");
        Lib.display_All_Books();

        System.out.println("\nFinding books by author: ");
        Lib.find_Books_By_Author("AuthorA");

        System.out.println("\nRemoving book 2222:");
        Lib.remove_Book("2222");
        System.out.println("Updated list of books: ");
        Lib.display_All_Books();
    }
}
