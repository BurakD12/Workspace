package com.library.dao;

import java.util.List;

import com.library.exception.DatabaseException;

public interface GenericDAO<T> {
    void insert(T entity) throws DatabaseException;
    T findById(int id) throws DatabaseException;
    List<T> findAll() throws DatabaseException;
    void delete(int id) throws DatabaseException;
} 
