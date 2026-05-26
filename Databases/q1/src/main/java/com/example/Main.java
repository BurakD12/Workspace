package com.example;

import java.util.List;
import java.util.Scanner;

import com.example.Dao.BookDAO;
import com.example.Dao.BookDAOImpl;
import com.example.Model.Book;

public class Main {
    public static void main(String[] args) {
        BookDAOImpl dao = new BookDAOImpl();
        Scanner scan = new Scanner(System.in);
        boolean flag = true;

        while (flag) {
            System.out.println("\n=== Library Book Manager ===");
            System.out.println("1. Add New Book");
            System.out.println("2. List All Books");
            System.out.println("3. Find Book by ID");
            System.out.println("4. Find Books by Genre");
            System.out.println("5. Update Book Availability");
            System.out.println("6. Delete Book");
            System.out.println("7. Count All Books");
            System.out.println("0. Exit");
            System.out.print("\nSelect option: ");

            if (!scan.hasNextInt()) {
                System.out.println("Invalid input. Please enter a number.");
                scan.next();
                continue;
            }

            int option = scan.nextInt();
            scan.nextLine(); // consume newline

            switch (option) {
                case 1:
                    System.out.print("Enter title: ");
                    String title = scan.nextLine();
                    System.out.print("Enter author: ");
                    String author = scan.nextLine();
                    System.out.print("Enter genre: ");
                    String genre = scan.nextLine();
                    System.out.print("Enter publish year: ");
                    if (!scan.hasNextInt()) {
                        System.out.println("Invalid input. Please enter a number.");
                        scan.next();
                        break;
                    }
                    int year = scan.nextInt();
                    scan.nextLine();
                    Book newBook = new Book(title, author, genre, year);
                    dao.insert(newBook);
                    System.out.println("Book added successfully! (ID: " + newBook.getId() + ")");
                    break;

                case 2:
                    List<Book> allBooks = dao.findAll();
                    System.out.printf("%-4s| %-28s| %-21s| %-16s| %-5s| %s%n",
                            "ID", "Title", "Author", "Genre", "Year", "Available");
                    System.out.println("----|----------------------------|----------------------|-----------------|------|----------");
                    for (Book b : allBooks) {
                        System.out.printf("%-4d| %-28s| %-21s| %-16s| %-5d| %s%n",
                                b.getId(), b.getTitle(), b.getAuthor(),
                                b.getGenre(), b.getPublishYear(), b.isAvailable() ? "Yes" : "No");
                    }
                    break;

                case 3:
                    System.out.print("Enter book ID: ");
                    if (!scan.hasNextInt()) {
                        System.out.println("Invalid input. Please enter a number.");
                        scan.next();
                        break;
                    }
                    int findId = scan.nextInt();
                    scan.nextLine();
                    Book found = dao.findById(findId);
                    if (found == null) {
                        System.out.println("No book found with ID " + findId + ".");
                    } else {
                        System.out.println(found);
                    }
                    break;

                case 4:
                    System.out.print("Enter genre: ");
                    String searchGenre = scan.nextLine();
                    List<Book> genreBooks = dao.findByGenre(searchGenre);
                    if (genreBooks.isEmpty()) {
                        System.out.println("No books found in genre '" + searchGenre + "'.");
                    } else {
                        System.out.println("Books in genre '" + searchGenre + "':");
                        for (Book b : genreBooks) {
                            System.out.printf("  - [ID: %d] %s — %s (%d) | Available: %s%n",
                                    b.getId(), b.getTitle(), b.getAuthor(),
                                    b.getPublishYear(), b.isAvailable() ? "Yes" : "No");
                        }
                    }
                    break;

                case 5:
                    System.out.print("Enter book ID to toggle availability: ");
                    if (!scan.hasNextInt()) {
                        System.out.println("Invalid input. Please enter a number.");
                        scan.next();
                        break;
                    }
                    int toggleId = scan.nextInt();
                    scan.nextLine();
                    Book toggleBook = dao.findById(toggleId);
                    if (toggleBook == null) {
                        System.out.println("No book found with ID " + toggleId + ".");
                    } else {
                        boolean oldVal = toggleBook.isAvailable();
                        toggleBook.setAvailable(!oldVal);
                        dao.update(toggleBook);
                        System.out.println("Book '" + toggleBook.getTitle() + "' availability changed: " + oldVal + " -> " + !oldVal);
                    }
                    break;

                case 6:
                    System.out.print("Enter book ID to delete: ");
                    if (!scan.hasNextInt()) {
                        System.out.println("Invalid input. Please enter a number.");
                        scan.next();
                        break;
                    }
                    int deleteId = scan.nextInt();
                    scan.nextLine();
                    dao.delete(deleteId);
                    System.out.println("Book with ID " + deleteId + " deleted successfully.");
                    break;

                case 7:
                    System.out.println("Total books in library: " + dao.count());
                    break;

                case 0:
                    flag = false;
                    System.out.println("Goodbye!");
                    break;

                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
        scan.close();
    }
}