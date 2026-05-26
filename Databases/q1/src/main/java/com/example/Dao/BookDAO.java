package com.example.Dao;

import java.util.List;

import com.example.Model.Book;

public interface BookDAO {

    void insert(Book book);
    Book findById(int id);
    List<Book> findByGenre(String genre);
    List<Book> findAll();
    void update(Book book);
    void delete(int id);
    int count();
}
