package Question5;

public class Book extends Item{
    private String author;
    private String ISBN;

    Book(String title, String author, String ISBN){
        super(title);
        this.author = author;
        this.ISBN = ISBN;
    }

    String getBookInfo(){
        return "Title: " + this.getTitle() + ", Author: " + this.author + ", ISBN: " + this.ISBN;
    }

    @Override
    void displayDetails() {
        System.out.println(this.getBookInfo() + ", Available: " + this.isAvailable());
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getISBN() {
        return ISBN;
    }

    public void setISBN(String iSBN) {
        ISBN = iSBN;
    }

    
}
