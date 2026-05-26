package com.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class Main {

    public static void main(String[] args) {
        try (Connection connection = MyDriverManager.getConnection()){
            if (connection != null) {
                System.out.println("Connected to PostgreSQL successfully!");
                System.out.println("Database: " + connection.getCatalog());
                System.out.println("Schema: " + connection.getSchema());

                String sql = """
                        INSERT INTO students (first_name, last_name, email, age, gpa, enrolled)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """;
                PreparedStatement pstmt = connection.prepareStatement(sql);

                pstmt.setString(1, "Ali");
                pstmt.setString(2, "Yilmaz");
                pstmt.setString(3, "ali.yilmaz@example.com");
                pstmt.setInt(4, 20);
                pstmt.setBigDecimal(5, new java.math.BigDecimal("3.75"));
                pstmt.setBoolean(6, true);

                int rowsAffected = pstmt.executeUpdate();
                System.out.println(rowsAffected + " row(s) inserted.");

            }
        } catch (SQLException e) {
            System.err.println("Connection failed!");
            System.err.println("Error: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
        }
    }
}