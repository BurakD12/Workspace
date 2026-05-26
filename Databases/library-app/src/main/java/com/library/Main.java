package com.library;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.library.dao.BookDAOImpl;
import com.library.dao.LoanDAOImpl;
import com.library.dao.MemberDAOImpl;
import com.library.exception.DatabaseException;
import com.library.model.Book;
import com.library.model.Member;
import com.library.service.BookService;
import com.library.service.LoanService;


public class Main {
    public static void main(String[] args) {
        BookDAOImpl bookDAO = new BookDAOImpl();
        MemberDAOImpl memberDAO = new MemberDAOImpl();
        LoanDAOImpl loanDAO = new LoanDAOImpl();
        BookService bookService = new BookService(bookDAO);
        LoanService loanService = new LoanService(loanDAO, bookDAO, memberDAO);
        Scanner sc = new Scanner(System.in);

        while (true) {
            printMenu();
            int option;

            try {
                option = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input, please enter a valid number!");
                continue;
            }

            try {
                switch (option) {
                    case 1 -> handleAddBook(sc, bookService);
                    case 2 -> handleListBooks(bookService);
                    case 3 -> handleFindBookById(sc, bookService);
                    case 4 -> handleFindBooksByGenre(sc, bookService);
                    case 5 -> handleToggleAvailability(sc, bookService);
                    case 6 -> handleDeleteBook(sc, bookService);
                    case 7 -> System.out.println("Total books: " + bookService.countBooks());
                    case 8 -> handleRegisterMember(sc, memberDAO);
                    case 9 -> handleListMembers(memberDAO);
                    case 10 -> handleFindMemberById(sc, memberDAO);
                    case 11 -> handleDeleteMember(sc, memberDAO);
                    case 12 -> handleBorrowBook(sc, loanService);
                    case 13 -> handleReturnBook(sc, loanService);
                    case 14 -> handleLoanHistory(sc, loanService);
                    case 0 -> {
                        System.out.println("Goodbye");
                        sc.close();
                        return;
                    }
                    default -> System.out.println("Invalid option, please try again!");
                }
            } catch (DatabaseException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private static void handleAddBook(Scanner sc, BookService bookService) throws DatabaseException {
        System.out.print("Enter title: > ");
        String title = sc.nextLine();
        System.out.print("Enter author: > ");
        String author = sc.nextLine();
        System.out.print("Enter genre: > ");
        String genre = sc.nextLine();
        System.out.print("Enter publish year: > ");

        int year;
        try {
            year = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return;
        }

        Book book = new Book(title, author, genre, year);
        bookService.addBook(book);
        System.out.println("Book added! (ID: " + book.getId() + ")");
    }
    
    private static void handleListBooks(BookService bookService) throws DatabaseException {
        List<Book> books = bookService.getAllBooks();
        if (books.isEmpty()) {
            System.out.println("No books found.");
            return;
        }
        printBookTable(books);
    }

    private static void handleFindBookById(Scanner sc, BookService bookService) throws DatabaseException {
        System.out.println("Enter book ID: > ");
        int id;
        try {
            id = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return;
        }
        Book book = bookService.getBookById(id);
        if (book == null) {
            System.out.println("Book not found with ID: " + id);
        } else {
            System.out.println(book);
        }
    }
    private static void handleFindBooksByGenre(Scanner sc, BookService bookService) throws DatabaseException {
        System.out.println("Enter book genre: > ");
        String genre = sc.nextLine();
        List<Book> books = bookService.getBooksByGenre(genre);
        if (books.isEmpty()) {
            System.out.println("No books found for the genre: " + genre);
            return;
        }
        printBookTable(books);
    }
    
    private static void handleToggleAvailability(Scanner sc, BookService bookService) throws DatabaseException {
        System.out.println("Enter book ID: > ");
        int id;
        try {
            id = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return;
        }
        Book book = bookService.getBookById(id);
        if (book == null) {
            System.out.println("Book not found with ID: " + id);
            return;
        }
        boolean availability = book.isAvailable();
        book.setAvailable(!availability);
        bookService.updateBook(book);
    }

    private static void handleDeleteBook(Scanner sc, BookService bookService) throws DatabaseException {
        System.out.println("Enter book ID: > ");
        int id;
         try {
            id = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return;
        }
        Book book = bookService.getBookById(id);
        if (book == null) {
            System.out.println("Book not found with ID: " + id);
            return;
        }
        bookService.deleteBook(id);
    }
    private static void handleRegisterMember(Scanner sc, MemberDAOImpl memberDAO) throws DatabaseException {
        System.out.println("Enter full name: > ");
        String fullName = sc.nextLine();
        System.out.println("Enter email: > ");
        String email = sc.nextLine();

        Member member = new Member(fullName, email);
        memberDAO.insert(member);
        System.out.println("Member added! (ID: " + member.getId() + ")");
    }
    private static void handleListMembers(MemberDAOImpl memberDAO) {}
    private static void handleFindMemberById(Scanner sc, MemberDAOImpl memberDAO) {}
    private static void handleDeleteMember(Scanner sc, MemberDAOImpl memberDAO) {}
    private static void handleBorrowBook(Scanner sc, LoanService loanService) {}
    private static void handleReturnBook(Scanner sc, LoanService loanService) {}
    private static void handleLoanHistory(Scanner sc, LoanService loanService) {}

    // ──────────────────────────── Helpers ──────────────────────────────────

    private static void printMenu() {
        String menu = "\n=== Library Management System ===\n" +
            "\n--- Books ---\n" +
            "1.  Add New Book\n" +
            "2.  List All Books\n" +
            "3.  Find Book by ID\n" +
            "4.  Find Books by Genre\n" +
            "5.  Update Book Availability\n" +
            "6.  Delete Book\n" +
            "7.  Count All Books\n" +
            "\n--- Members ---\n" +
            "8.  Register New Member\n" +
            "9.  List All Members\n" +
            "10. Find Member by ID\n" +
            "11. Delete Member\n" +
            "\n--- Loans ---\n" +
            "12. Borrow Book\n" +
            "13. Return Book\n" +
            "14. View Member Loan History\n" +
            "\n0. Exit\n" +
            "\nSelect option: > ";
        System.out.print(menu);
    }

    private static void printBookTable(List<Book> books) {
        System.out.printf("%-5s | %-22s | %-17s | %-17s | %-4s | %-9s%n",
            "ID", "Title", "Author", "Genre", "Year", "Available");
        System.out.println("-".repeat(86));
        for (Book b : books) {
            System.out.printf("%-5d | %-22s | %-17s | %-17s | %-4d | %-9s%n",
                b.getId(), b.getTitle(), b.getAuthor(), b.getGenre(),
                b.getPublishYear(), b.isAvailable() ? "Yes" : "No");
        }
    }
}