package Question4;

public class Book extends Item{
    private String author;

    Book(int id, String title, double price, String author){
        super(id, title, price);
        this.author = author;
    }

    @Override
    String getDetails() {
        return "ID: " + this.getId() + ", Title: " + this.getTitle() + ", Price: " + this.getPrice() + ", Author: " + this.author;
    }
}
