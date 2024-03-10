package Question3;

public class Book {
    private String title;
    private String author;
    private String isbn;

    Book(String title, String author, String isbn){
        this.title = title;
        this.author = author;
        this.isbn = isbn;
    }

    String getISBN(){
        return this.isbn;
    }

    String getTitle(){
        return this.title;
    }

    String getAuthor(){
        return this.author;
    }
}
