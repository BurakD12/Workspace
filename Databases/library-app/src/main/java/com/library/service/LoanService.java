package com.library.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.library.dao.BookDAO;
import com.library.dao.LoanDAO;
import com.library.dao.MemberDAO;
import com.library.exception.BookNotAvailableException;
import com.library.exception.DatabaseException;
import com.library.exception.RecordNotFoundException;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.util.ConnectionPool;

public class LoanService {
    private LoanDAO loanDAO;
    private BookDAO bookDAO;
    private MemberDAO memberDAO;

    public LoanService(LoanDAO loanDAO, BookDAO bookDAO, MemberDAO memberDAO) {
        this.loanDAO = loanDAO;
        this.bookDAO = bookDAO;
        this.memberDAO = memberDAO;
    }

    public Loan borrowBook(int bookId, int memberId) throws DatabaseException {
        Book book = bookDAO.findById(bookId);
        if (book == null) {
            throw new RecordNotFoundException("Book with ID " + bookId + " not found");
        }

        Member member = memberDAO.findById(memberId);
        if (member == null) {
            throw new RecordNotFoundException("Member with ID " + memberId + " not found");
        }

        if (!book.isAvailable()) {
            throw new BookNotAvailableException("Book '" + book.getTitle() + "' is not currently available");
        }

        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            conn.setAutoCommit(false);

            Loan loan = new Loan(book, member);
            loanDAO.insert(loan, conn);
            book.setAvailable(false);
            bookDAO.update(book);
            conn.commit();
            return loan;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new DatabaseException("Failed to borrow book", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }
    
    public void returnBook(int loanId) throws DatabaseException {
        Loan loan = loanDAO.findById(loanId);
        if(loan == null) {
            throw new RecordNotFoundException("Loan with ID " + loanId + " not found");
        }
        if (loan.isActive() == false) {
            throw new IllegalStateException("Loan with ID: " + loanId + " has already been returned");
        }
        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();
            conn.setAutoCommit(false);

            loanDAO.markReturned(loanId, conn);
            Book book = loan.getBook();
            book.setAvailable(true);
            bookDAO.update(book);
            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new DatabaseException("Failed to return book", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close(); 
                } catch (SQLException ignored) {}
            }
        }
    }

    public List<Loan> getLoansForMember(int memberId) throws DatabaseException {
        return loanDAO.findByMember(memberId);
    }
}
