package com.library.service;

import java.util.List;

import com.library.dao.BookDAO;
import com.library.exception.DatabaseException;
import com.library.exception.RecordNotFoundException;
import com.library.model.Book;

public class BookService {
    private BookDAO bookDAO;

    public BookService(BookDAO bookDAO) {
        this.bookDAO = bookDAO;    
    }


    public void addBook(Book book) throws DatabaseException {
        if (book.getTitle().trim().isEmpty() || 
            book.getAuthor().trim().isEmpty() || 
            book.getGenre().trim().isEmpty()) {
            throw new IllegalArgumentException("Title, author, and genre must not be blank");
        }
        bookDAO.insert(book);
    }

    public List<Book> getAllBooks() throws DatabaseException {
        return bookDAO.findAll();
    }

    public Book getBookById(int id) throws DatabaseException {
        Book book = bookDAO.findById(id);
        if (book == null) {
            throw new RecordNotFoundException("Book with ID " + id + " not found.");
        }
        return book;
    }

    public List<Book> getBooksByGenre(String genre) throws DatabaseException {
        return bookDAO.findByGenre(genre);
    }

    public void updateBook(Book book) throws DatabaseException {
        if (book.getTitle().trim().isEmpty() || 
            book.getAuthor().trim().isEmpty() || 
            book.getGenre().trim().isEmpty()) {
            throw new IllegalArgumentException("Title, author, and genre must not be blank");
        }
        bookDAO.update(book);
    }

    public void deleteBook(int id) throws DatabaseException {
        bookDAO.delete(id);
    }

    public int countBooks() throws DatabaseException {
        return bookDAO.count();
    }
}
