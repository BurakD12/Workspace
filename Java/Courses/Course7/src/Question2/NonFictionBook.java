package Question2;

public class NonFictionBook extends Book{
    NonFictionBook(int id, String title, String author, double price){
        super(id, title, author, price);
    }

    @Override
    String bookType() {
        return "Non-Fiction";
    }

    
}
