package com.library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.library.exception.DatabaseException;
import com.library.model.Book;
import com.library.util.ConnectionPool;

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
    public void insert(Book entity) throws DatabaseException {
        String sql = "INSERT INTO books (title, author, genre, publish_year, available) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConnectionPool.getConnection();
                PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getTitle());
            ps.setString(2, entity.getAuthor());
            ps.setString(3, entity.getGenre());
            ps.setInt(4, entity.getPublishYear());
            ps.setBoolean(5, entity.isAvailable());
            ps.executeUpdate(sql);

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entity.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error inserting book: " + e.getMessage(), e);
        }
    }

    @Override
    public Book findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM books WHERE id = ?";
        try (Connection con = ConnectionPool.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding the book: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Book> findAll() throws DatabaseException {
        String sql = "SELECT * FROM books ORDER BY id";
        try (Connection con = ConnectionPool.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            ArrayList<Book> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding all books: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM books WHERE id=?";
        try (Connection con = ConnectionPool.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting the book: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Book> findByGenre(String genre) throws DatabaseException {
        String sql = "SELECT * FROM books WHERE genre ILIKE ? ORDER BY title";
        try (Connection con = ConnectionPool.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, genre);
            ArrayList<Book> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding books by the genre: " + genre + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Book book) throws DatabaseException {
        String sql = "UPDATE books SET title=?, author=?, genre=?, publish_year=?, available=? WHERE id=?";
        try (Connection con = ConnectionPool.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getGenre());
            ps.setInt(4, book.getPublishYear());
            ps.setBoolean(5, book.isAvailable());
            ps.setInt(6, book.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error updating the book: " + e.getMessage(), e);
        }
    }

    @Override
    public int count() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM books";
        try (Connection con = ConnectionPool.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new DatabaseException("Error finding the count: " + e.getMessage(), e);
        }
    }

}
