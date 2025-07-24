package Exercise.Ex3;

public class Book {
    private int bookId;
    private String title;
    private String author;
    private boolean isAvailable;

    public Book(){
        this.bookId = 000;
        this.title = "Unknown";
        this.author = "Unknown";
        this.isAvailable = true;
    }

    public Book(int bookId, String title, String author, boolean isAvailable){
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.isAvailable = isAvailable;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

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

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }


    public void displayBookInfo(){
        if (this.isAvailable == true) {
        
        System.out.println("The books ID is: " + this.bookId +
        "\nThe books title is: " + this.title + 
        "\nThe books author is: " + this.author + 
        "\nThe book is available.");
        }

        else if (this.isAvailable == false){
        System.out.println("The books ID is: " + this.bookId +
        "\nThe books title is: " + this.title + 
        "\nThe books author is: " + this.author + 
        "\nThe book is not available.");
        }

    }

    public void borrowBook(){
        this.isAvailable = false;
    }

    public void returnBook(){
        this.isAvailable = true;
    }

}
