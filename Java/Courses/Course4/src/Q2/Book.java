package Q2;

public class Book {
    private String title;
    private String author;
    private double price;
    private int yearPublished;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getYearPublished() {
        return yearPublished;
    }

    public void setYearPublished(int yearPublished) {
        this.yearPublished = yearPublished;
    }

    Book(){
        title = "Unknown";
        author = "Unknown";
        price = 0.0;
        yearPublished = 0;
    }

    Book(String title, String author, double price, int yearPublished){
        this.title = title;
        this.author = author;
        this.price = price;
        this.yearPublished = yearPublished;
    }

    Book(Book book){
        this.title = book.title;
        this.author = book.author;
        this.price = book.price;
        this.yearPublished = book.yearPublished;
    }

    public void displayBookDetails(){
        System.out.println(title);
        System.out.println(author);
        System.out.println(price);
        System.out.println(yearPublished);
        System.out.println();
    }
}
