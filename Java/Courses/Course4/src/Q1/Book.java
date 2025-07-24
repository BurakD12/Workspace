package Q1;
public class Book {
    String title;
    String author;
    double price;
    int yearPublished;


    public Book(){
        title = "Unknown";
        author = "Unknown";
        price = 0.0;
        yearPublished = 0;
    }

    public Book(String title, String author, double price, int yearPublished){
        this.title = title;
        this.author = author;
        this.price = price;
        this.yearPublished = yearPublished;
    }

    public Book(Book book){
        this.title = book.title;
        this.author = book.author;
        this.price = book.price;
        this.yearPublished = book.yearPublished;
    }

    public void displayBookDetails(){
        System.out.println("Title: " + title);
        System.out.println("Author: " + author);
        System.out.println("Price: $" + price);
        System.out.println("Year Published: " + yearPublished);
        System.out.println();
    }

}
