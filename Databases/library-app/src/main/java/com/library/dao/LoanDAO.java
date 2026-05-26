package com.library.dao;

import java.sql.Connection;
import java.util.List;

import com.library.exception.DatabaseException;
import com.library.model.Loan;

public interface LoanDAO {
    void insert(Loan loan, Connection con) throws DatabaseException;
    Loan findById(int id) throws DatabaseException;
    List<Loan> findByMember(int memberId) throws DatabaseException;
    Loan findActiveByBook(int bookId) throws DatabaseException;
    void markReturned(int loanId, Connection con) throws DatabaseException;
}