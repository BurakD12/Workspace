package com.example.Dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.management.RuntimeErrorException;

import com.example.Model.Book;
import com.example.Util.DatabaseConnection;

public class BookDAOImpl implements BookDAO {
    
    private Book mapRow(ResultSet rs) throws SQLException {
        Book book = new Book();
        
        book.setId(rs.getInt("id"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setGenre(rs.getString("genre"));
        book.setPublishYear(rs.getInt("publish_year"));
        book.setAvailable(rs.getBoolean("available"));

        return book;
    }

    @Override
    public void insert(Book book) {
        String sql = "INSERT INTO books (title, author, genre, publish_year, available) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection(); 
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getGenre());
            ps.setInt(4, book.getPublishYear());
            ps.setBoolean(5, book.isAvailable());

            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    book.setId(generatedKeys.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error inserting book: " + e.getMessage(), e);
        }
    }

    @Override
    public Book findById(int id) {
        String sql = "SELECT * FROM books WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)){
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                } 
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding book by ID: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Book> findAll() {
        String sql = "SELECT * FROM books ORDER BY id";
        List<Book> books = new ArrayList<Book>();
        try (Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()) {
                    books.add(mapRow(rs));
                }
            } 
        } catch (Exception e) {
            throw new RuntimeException("Error finding all books: " + e.getMessage(), e);
        }
        return books;
    }

    @Override
    public List<Book> findByGenre(String genre) {
            String sql = "SELECT * FROM books WHERE genre ILIKE ? ORDER BY title";
        List<Book> books = new ArrayList<Book>();
        try (Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, genre);
            try (ResultSet rs = ps.executeQuery()){
                while (rs.next()) {
                    books.add(mapRow(rs));
                }
            } 
        } catch (Exception e) {
            throw new RuntimeException("Error finding corresponding books: " + e.getMessage(), e);
        }
        return books;

    }

    @Override
    public void update(Book book) {
        String sql = "UPDATE books SET title = ?, author = ?, genre = ?, publish_year = ?, available = ? WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getGenre());
            ps.setInt(4, book.getPublishYear());
            ps.setBoolean(5, book.isAvailable());
            ps.setInt(6, book.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating book", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM books WHERE id = ?";
        try (Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)){
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting book", e);
        }
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM books";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting books", e);
        }
        return 0;
    }
}
