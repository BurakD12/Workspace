package Q1;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        Library library = new Library();

        boolean running = true;
        while (running) {
            String menuChoice = menu();
            switch (menuChoice) {
                case "1" -> {
                    System.out.println("Enter the ID for the new book: ");
                    String bookId = scan.nextLine();
                    if (library.getBook(bookId) != null) {
                        System.out.println("The book with this ID already exists.");
                    }
                    else {
                        System.out.println("Enter the books title: ");
                        String bookTitle = scan.nextLine();
                        System.out.println("Enter the books author: ");
                        String bookAuthor = scan.nextLine();

                        library.addBook(new Book(bookId, bookTitle, bookAuthor));
                    }
                }

                case "2" -> {
                    System.out.println("Enter the ID for the new member: ");
                    String memberId = scan.nextLine();

                    if (library.getMember(memberId) != null){
                        System.out.println("The member with this ID already exists.");
                    }
                    else {
                        System.out.println("Enter the new members name: ");
                        String name = scan.nextLine();

                        library.addMember(new Member(memberId, name));
                    }
                }

                case "3" -> {
                    System.out.println("Enter ID of the book to be borrowed: ");
                    String bookId = scan.nextLine();
                    if (library.getBook(bookId) != null){
                        if (!library.getBook(bookId).isAvailable()){
                            System.out.println("The book isn't available.");
                        }
                        else {
                            System.out.println("Enter the ID of the member that wants to borrow the book: ");
                            String memberId = scan.nextLine();
                            if (library.getMember(memberId) != null){
                                library.borrowBook(memberId, bookId);
                                System.out.println("Book successfully borrowed.");
                                library.getBook(bookId).setAvailable(false);
                            }
                            else {
                                System.out.println("The member with the ID: " + memberId + " doesn't exist.");
                            }
                        }
                    }
                    else {
                        System.out.println("Book couldn't be found.");
                    }
                }

                case "4" -> {
                    System.out.println("Enter the ID of the book to be returned: ");
                    String bookId = scan.nextLine();
                    if (library.getBook(bookId) != null){
                        if (library.getBook(bookId).isAvailable()){
                            System.out.println("The book has already been returned.");
                        }
                        else {
                            System.out.println("Enter the ID of the member that is returning the book: ");
                            String memberId = scan.nextLine();
                            if (library.getMember(memberId) != null){
                                library.returnBook(memberId, bookId);
                                library.getBook(bookId).setAvailable(true);
                                System.out.println("Book successfully returned.");
                            }
                            else {
                                System.out.println("The member with the ID: " + memberId + " doesn't exist.");
                            }
                        }
                    }
                    else {
                        System.out.println("Book couldn't be found.");
                    }    
                }

                case "5" -> {
                    System.out.println("Enter the ID for the book that will be updated: ");
                    String bookId = scan.nextLine();

                    if (library.getBook(bookId) == null){
                        System.out.println("The book with this ID doesn't exist.");
                    }
                    else {
                        System.out.println("Enter the updated title: ");
                        String bookTitle = scan.nextLine();
                        System.out.println("Enter the updated author: ");
                        String bookAuthor = scan.nextLine();

                        library.updateBook(bookId, bookTitle, bookAuthor);
                        System.out.println("Book successfully updated.");
                    }
                }

                case "6" -> {
                    System.out.println("Enter the ID of the member to be removed: ");
                    String memberId = scan.nextLine();

                    if (library.getMember(memberId) == null){
                        System.out.println("Member couldn't be found.");
                    }
                    else {
                        library.removeMember(memberId);
                        System.out.println("Member successfully removed.");
                    }
                }
            
                default -> {
                    System.out.println("Invalid input, please try again.");
                }

            }
        }
    }

    public static String menu(){
        Scanner scan = new Scanner(System.in);
        
        System.out.println("Press 1 to add a book:\nPress 2 to add a member:\nPress 3 to borrow a book:\nPress 4 to return a book:\nPress 5 to update a member:\nPress 6 to delete a member:");

        return scan.nextLine();
    }
}
