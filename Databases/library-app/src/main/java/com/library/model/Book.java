package com.library.model;

public class Book {
    private int id;
    private String title;
    private String author;
    private String genre;
    private int publishYear;
    private boolean isAvailable;

    public Book(){}

    public Book(String title, String author, String genre, int publishYear) {
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.publishYear = publishYear;
        this.id = 0;
        this.isAvailable = true;
    }
    
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
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
    public String getGenre() {
        return genre;
    }
    public void setGenre(String genre) {
        this.genre = genre;
    }
    public int getPublishYear() {
        return publishYear;
    }
    public void setPublishYear(int publishYear) {
        this.publishYear = publishYear;
    }
    public boolean isAvailable() {
        return isAvailable;
    }
    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    @Override
    public String toString() {
        return "Book{id=" + id +
               ", title='" + title + '\'' +
               ", author='" + author + '\'' +
               ", genre='" + genre + '\'' +
               ", year=" + publishYear +
               ", available=" + isAvailable + '}';
    }

        
}
