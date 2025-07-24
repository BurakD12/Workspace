package Q1;

public class Book {
    private String bookId;
    private String title;
    private String author;
    private boolean isAvailable;

    public Book(String bookId, String title, String author){
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.isAvailable = true;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("The books ID is: ").append(this.bookId);
        sb.append("\nThe books title is: ").append(this.title);
        sb.append("\nThe books author is: ").append(this.author);
        if (this.isAvailable){
            sb.append("\nThe books is available.");
        }
        else {
            sb.append("\nThe book isn't available.");
        }
        return sb.toString();
    }
}
