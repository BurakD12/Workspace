package Exercise.Ex3;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        LibraryManager libraryManager = new LibraryManager();
        boolean running = true;
        while (running) {
            String menuChoice = menu();
            switch (menuChoice) {
                case "1" -> {
                    System.out.println();
                    System.out.println("Enter the ID of the new member: ");
                    int id = scan.nextInt();
                    System.out.println("Enter the name of the new member: ");
                    String name = scan.nextLine();
                    name = scan.nextLine();
                    System.out.println("Enter the email of the new member: ");
                    String email = scan.nextLine();
                    System.out.println("If the new member is a student type 1 / If the new member is an adult type 2: ");
                    String memberTypeChoice = scan.nextLine();
                    boolean isChoiceMade = false;
                    String membershipType;
                    int maxBooksAllowed;
                    while (isChoiceMade == false) {
                        if (memberTypeChoice.equals("1")){
                            membershipType = "Student";
                            maxBooksAllowed = 3;
                            if (libraryManager.findMemberById(id) == null){
                            libraryManager.addMember(new Member(id, name, email, membershipType, maxBooksAllowed));
                            }
                            else {
                            System.out.println("Member with the same ID already exists within the system.");
                            System.out.println();
                            }
                            isChoiceMade = true;
                        }
                        else if (memberTypeChoice.equals("2")) {
                            membershipType = "Adult";
                            maxBooksAllowed = 5;
                            if (libraryManager.findMemberById(id) == null){
                            libraryManager.addMember(new Member(id, name, email, membershipType, maxBooksAllowed));
                            }
                            else {
                            System.out.println("Member with the same ID already exists within the system.");
                            System.out.println();
                            }
                            isChoiceMade = true;
                        }
                        else {
                            System.out.println("Invalid input, please try again!");
                            memberTypeChoice = scan.nextLine();
                        }
                    }
                }
                case "2" -> {
                    if (libraryManager.getMembers().size() == 0){
                        System.out.println("No members found.");
                    }
                    else {
                        libraryManager.viewAllMembers();
                    }
                }
                case "3" -> {
                    System.out.println("Enter the ID of the member to be updated: ");
                    int id = scan.nextInt();

                    if (libraryManager.findMemberById(id) == null){
                        System.out.println("Member could not be found!");
                    }
                    else {
                    System.out.println();
                    System.out.println("Enter the updated name for the member: ");
                    String name = scan.nextLine();
                    name = scan.nextLine();
                    System.out.println("Enter the updated email for the member: ");
                    String email = scan.nextLine();
                    System.out.println("If the updated member is a student type 1 / If the updated member is an adult type 2: ");
                    String memberTypeChoice = scan.nextLine();
                    boolean isChoiceMade = false;
                    String membershipType;
                    int maxBooksAllowed;
                    while (isChoiceMade == false) {
                        if (memberTypeChoice.equals("1")){
                            membershipType = "Student";
                            maxBooksAllowed = 3;
                            libraryManager.updateMember(id, name, email, membershipType, maxBooksAllowed);
                            isChoiceMade = true;
                        }
                        else if (memberTypeChoice.equals("2")) {
                            membershipType = "Adult";
                            maxBooksAllowed = 5;
                            libraryManager.updateMember(id, name, email, membershipType, maxBooksAllowed);
                            isChoiceMade = true;
                        }
                        else {
                            System.out.println("Invalid input, please try again!");
                            memberTypeChoice = scan.nextLine();
                        }
                    }
                    }
                }
                case "4" ->{
                    System.out.println("Enter the ID of the member that will be deleted: ");
                    int id = scan.nextInt();

                    if (libraryManager.findMemberById(id) == null){
                        System.out.println();
                        System.out.println("Member with the given ID already doesn't exist!");
                    }

                    else {
                        libraryManager.deleteMember(id);
                        System.out.println("The member whose ID was " + id + " deleted from the members list.");
                    }
                }
                case "5" ->{
                    System.out.println("Enter the ID for the new book: ");
                    int bookId = scan.nextInt();
                    System.out.println("Enter the title for the new book: ");
                    String name = scan.nextLine();
                    name = scan.nextLine();
                    System.out.println("Enter the name of the author of the new book: ");
                    String author = scan.nextLine();
                    if (libraryManager.findBookById(bookId) == null){
                        libraryManager.addBook(new Book(bookId, name, author, true));
                    }
                    else {
                        System.out.println("The book already exists in the library.");
                    }
                }
                case "6" ->{
                    if (libraryManager.getBooks().size() == 0){
                        System.out.println("There are no books.");
                    }
                    else {
                        libraryManager.viewAllBooks();
                    }
                }
                case "7" ->{
                    System.out.println("Enter the ID of the book to be updated: ");
                    int bookId = scan.nextInt();

                    if (libraryManager.findBookById(bookId) == null){
                        System.out.println("Book not found.");
                    }
                    System.out.println("Enter the new title for the book: ");
                    String newTitle = scan.nextLine();
                    newTitle = scan.nextLine();
                    System.out.println("Enter the new author for the book: ");
                    String newAuthor = scan.nextLine();

                    libraryManager.updateBook(bookId, newTitle, newAuthor);
                    System.out.println("Book successfully updated.");
                }
                case "8" -> {
                    System.out.println("Enter the ID of the book to be deleted: ");
                    int bookId = scan.nextInt();

                    if (libraryManager.findBookById(bookId) == null){
                        System.out.println("Book not found.");
                    }
                    else {
                        libraryManager.deleteBook(bookId);
                        System.out.println("Book successfully deleted.");
                    }
                }
                case "9" -> {
                    System.out.println("Enter the book ID to be borrowed: ");
                    int bookId = scan.nextInt();

                    if (libraryManager.findBookById(bookId) == null){
                        System.out.println("Book not found.");
                    }

                    else if (libraryManager.findBookById(bookId).isAvailable() == false){
                        System.out.println("Book is not available.");
                    }

                    else {
                        libraryManager.findBookById(bookId).borrowBook();
                        System.out.println("The book has been borrowed.");
                    }
                }

                case "10" -> {
                    System.out.println("Enter the book ID to be returned: ");
                    int bookId = scan.nextInt();

                    if (libraryManager.findBookById(bookId) == null){
                        System.out.println("Book not found.");
                    }

                    else if (libraryManager.findBookById(bookId).isAvailable() == true){
                        System.out.println("The book has already been returned.");
                    }

                    else {
                        libraryManager.findBookById(bookId).returnBook();
                        System.out.println("The book has been successfully returned.");
                    }
                }

                case "11" -> {
                    running = false;
                    break;
                }

                default -> {
                    System.out.println("Invalid input, please try again!");
                }
            }
        }
    }
    

    public static String menu(){
        Scanner scan = new Scanner(System.in);
        
        System.out.println("Press 1 to add member:\nPress 2 to view all members:\nPress 3 to update a member:\nPress 4 to delete a member:\nPress 5 to add a book:\nPress 6 to view all books:\nPress 7 to update a book:\nPress 8 to delete a book:\nPress 9 if a member wants to borrow a book:\nPress 10 if a member wants to return a book:\nPress 11 to exit program:");

        return scan.nextLine();
    }
}


