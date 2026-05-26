package com.library.model;

import java.time.LocalDate;

public class Member {
    private int id;
    private String fullName;
    private String email;
    private LocalDate joinedAt;

    public Member(){}

    public Member(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
        this.id = 0;
        this.joinedAt = LocalDate.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDate joinedAt) {
        this.joinedAt = joinedAt;
    }

    @Override
    public String toString() {
        return "Member{id=" + id +
               ", name='" + fullName + '\'' +
               ", email='" + email + '\'' +
               ", joined=" + joinedAt + '}';
    }

    
}
