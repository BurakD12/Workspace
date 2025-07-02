package Question1;

public class Book {
    String title; // Title of the book
    String author; // The authors name
    int yearPublished; // The year it was published
    boolean isAvailable; // flag check for whether or not the book is available
    

    Book(){
        this.title = "Unknown";
        this.author = "Unknown";
        this.yearPublished = 2024;
        this.isAvailable = true;
    }

    Book(String title, String author, int year){
        this.title = title;
        this.author = author;
        this.yearPublished = year;
        this.isAvailable = true;
    }

    void borrowBook(){
        this.isAvailable = false;
    }

    void dispInfo(){
        System.out.println("This book's title is: " + this.title + ".");
        System.out.println("This author of this book is: " + this.author + ".");
        System.out.println("This book was published in: " + this.yearPublished + ".");
        System.out.println("Availability: " + this.isAvailable);
        System.out.println();
    }


}
