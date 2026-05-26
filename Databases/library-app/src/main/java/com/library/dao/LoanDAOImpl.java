package com.library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.library.exception.DatabaseException;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.util.ConnectionPool;

public class LoanDAOImpl implements LoanDAO{

    public Loan mapRow(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getInt("book_id"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setGenre(rs.getString("genre"));
        book.setPublishYear(rs.getInt("publish_year"));
        book.setAvailable(rs.getBoolean("available"));

        Member member = new Member();
        member.setId(rs.getInt("member_id"));
        member.setFullName(rs.getString("full_name"));
        member.setEmail(rs.getString("email"));
        member.setJoinedAt(rs.getDate("joined_at").toLocalDate());

        Loan loan = new Loan();
        loan.setId(rs.getInt("loan_id"));
        loan.setBook(book);
        loan.setMember(member);
        loan.setLoanDate(rs.getTimestamp("loan_date").toLocalDateTime());
        Timestamp returnTimestamp = rs.getTimestamp("return_date");
        loan.setReturnDate(returnTimestamp == null ? null : returnTimestamp.toLocalDateTime());
        return loan;
    }

    private static final String BASE_SQL =
        "SELECT l.id AS loan_id, l.loan_date, l.return_date, " +
        "b.id AS book_id, b.title, b.author, b.genre, b.publish_year, b.available, " +
        "m.id AS member_id, m.full_name, m.email, m.joined_at " +
        "FROM loans l " +
        "JOIN books b ON b.id = l.book_id " +
        "JOIN members m ON m.id = l.member_id ";

    @Override
    public void insert(Loan loan, Connection con) throws DatabaseException {
        String sql = "INSERT INTO loans (book_id, member_id, loan_date) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)){
            ps.setInt(1, loan.getBook().getId());
            ps.setInt(2, loan.getMember().getId());
            ps.setTimestamp(3, Timestamp.valueOf(loan.getLoanDate()));
            ps.executeUpdate(sql);

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    loan.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error inserting the new loan record: " + e.getMessage(), e);
        }
    }

    @Override
    public Loan findById(int id) throws DatabaseException {
        String sql = BASE_SQL + "WHERE l.id = ?";
        try (Connection con = ConnectionPool.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)){
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding the loan record by id: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Loan> findByMember(int memberId) throws DatabaseException {
        String sql = BASE_SQL + "WHERE l.member_id = ? ORDER BY l.loan_date DESC";
        try (Connection con = ConnectionPool.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)){
            ArrayList<Loan> list = new ArrayList<>();
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding the loan records for the given member: " + e.getMessage(), e);
        }
    }

    @Override
    public Loan findActiveByBook(int bookId) throws DatabaseException {
        String sql = BASE_SQL + "WHERE l.book_id = ? AND l.return_date IS NULL";
        try (Connection con = ConnectionPool.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)){
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding the active loan for the given book: " + e.getMessage(), e);
        }
    }

    @Override
    public void markReturned(int loanId, Connection con) throws DatabaseException {
        String sql = "UPDATE loans SET return_date = NOW() WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)){
            ps.setInt(1, loanId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error marking loan as returned: " + e.getMessage(), e);
        }
    }
    
}
