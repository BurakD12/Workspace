package Q2;

public class Main {
    public static void main(String[] args) {
        Library library = new Library();
        Book book1 = new Book();
        Book book2 = new Book("Title", "No One", 5.99, 2000);
        Book book3 = new Book(book2);
        library.addBook(book1);
        library.addBook(book2);
        library.addBook(new Book("Nutuk", "Atatürk", 49.99, 1936));
        Book[] books = library.getBooks();

        for (int i = 0; i < library.getSize(); i++) {
            library.getBooks()[i].displayBookDetails();
        }
        library.removeBook("Title");
        System.out.println("List After Removal: ");
        for (int i = 0; i < library.getSize(); i++) {
            library.getBooks()[i].displayBookDetails();
        }

        library.updateBook("Unknown", new Book("Uncharted", "Ubisoft", 59.99, 2008));
        System.out.println("Updated List: ");
        for (int i = 0; i < library.getSize(); i++) {
            library.getBooks()[i].displayBookDetails();
        }

        System.out.println("Book by title: " + library.getBook("Nutuk").getTitle());
    }
    
}
