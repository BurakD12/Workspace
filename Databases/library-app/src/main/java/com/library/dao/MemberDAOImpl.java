package com.library.dao;

import com.library.exception.DatabaseException;
import com.library.model.Member;
import com.library.util.ConnectionPool;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemberDAOImpl implements MemberDAO {

    private Member mapRow(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setId(rs.getInt("id"));
        m.setFullName(rs.getString("full_name"));
        m.setEmail(rs.getString("email"));
        m.setJoinedAt(rs.getDate("joined_at").toLocalDate());
        return m;
    }

    @Override
    public void insert(Member member) throws DatabaseException {
        String sql = "INSERT INTO members (full_name, email, joined_at) VALUES (?, ?, ?)";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, member.getFullName());
            ps.setString(2, member.getEmail());
            ps.setDate(3, Date.valueOf(member.getJoinedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) member.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error inserting member", e);
        }
    }

    @Override
    public Member findById(int id) throws DatabaseException {
        String sql = "SELECT * FROM members WHERE id = ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding member by ID", e);
        }
    }

    @Override
    public Member findByEmail(String email) throws DatabaseException {
        String sql = "SELECT * FROM members WHERE email = ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding member by email", e);
        }
    }

    @Override
    public List<Member> findAll() throws DatabaseException {
        String sql = "SELECT * FROM members ORDER BY id";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Member> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching all members", e);
        }
    }

    @Override
    public void update(Member member) throws DatabaseException {
        String sql = "UPDATE members SET full_name=?, email=? WHERE id=?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, member.getFullName());
            ps.setString(2, member.getEmail());
            ps.setInt(3, member.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error updating member", e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        String sql = "DELETE FROM members WHERE id=?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting member", e);
        }
    }
}
