package com.example._6_security.projection;

public interface BookSummaryView {
    Long getId();

    String getTitle();

    String getIsbn();

    Author getAuthor();

    interface Author {
        String getLastName();

    }
}
