package Question2;

public class Book extends LibraryItem{
    private String author;
    
    Book(String name, int yearPublished, String author){
        super(name, yearPublished);
        this.author = author;
    }

    @Override
    String getInfo() {
        return "Title: " + this.getTitle() + ", Year: " + this.getYearPublished() + ", Author: " + this.author;
    }

    // @Override
    // public String toString() {
        
    //     return super.toString() + " author=" + author + "]";
    // }

    
}
