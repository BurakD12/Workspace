package Question2;

public class FictionBook extends Book{
    FictionBook(int id, String title, String author, double price){
        super(id, title, author, price);
    }

    @Override
    String bookType() {
        return "Fiction";
    }

    
}
