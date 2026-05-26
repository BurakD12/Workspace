package com.library.model;

import java.time.LocalDateTime;

public class Loan {
    private int id;
    private Book book;
    private Member member;
    private LocalDateTime loanDate;
    private LocalDateTime returnDate;

    public Loan(){}

    public Loan(Book book, Member member) {
        this.book = book;
        this.member = member;
        this.id = 0;
        this.loanDate = LocalDateTime.now();
        this.returnDate = null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public LocalDateTime getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(LocalDateTime loanDate) {
        this.loanDate = loanDate;
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public boolean isActive() {
        return returnDate == null;
    }

    @Override
    public String toString() {
        return "Loan{id=" + id +
               ", book='" + (book != null ? book.getTitle() : null) + '\'' +
               ", member='" + (member != null ? member.getFullName() : null) + '\'' +
               ", loaned=" + loanDate +
               ", returned=" + returnDate + '}';
    }

    
}
