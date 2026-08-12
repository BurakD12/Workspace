package com.example._5_mapstruct.projection;

public interface BookSummaryView {
    Long getId();
    String getTitle();
    String getIsbn();
    Author getAuthor();

    interface Author {
        String getLastName();

    }    
}

