package com.library.dao;

import java.util.List;

import com.library.exception.DatabaseException;
import com.library.model.Book;

public interface BookDAO extends GenericDAO<Book>{
    List<Book> findByGenre(String genre) throws DatabaseException;
    void update(Book book) throws DatabaseException;
    int count() throws DatabaseException;
}
