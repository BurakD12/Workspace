package Q1;

import java.util.Map.Entry;
import java.util.HashMap;

public class Library {
    public static  HashMap<String, Book> books = new HashMap<>();
    public static HashMap<String, Member> members = new HashMap<>();

    public Library(){
    }

    public void addBook(Book book){
        if (books.containsKey(book.getBookId())){
            System.out.println("The book has already been added.");
            return;
        }
        else {
            books.put(book.getBookId(), book);
        }
    }

    public void removeBook(String bookId){
        if (!books.containsKey(bookId)){
            System.out.println("Book couldn't be found.");
            return;
        }
        else {
            books.remove(bookId);
            System.out.println("The book has been successfully removed.");
        }
    }

    public Book getBook(String bookId){
        if (!books.containsKey(bookId)){
            return null;
        }
        else {
            return books.get(bookId);
        }
    }

    public void updateBook(String bookId, String newTitle, String newAuthor){
        if (!books.containsKey(bookId)) {
            System.out.println("Book couldn't be found.");
            return;
        }
        else {
            books.get(bookId).setTitle(newTitle);
            books.get(bookId).setAuthor(newAuthor);
            System.out.println("Book successfully updated.");
        }
    }

    public void addMember(Member member){
        if (!members.containsKey(member.memberId)){
            members.put(member.getMemberId(), member);
            System.out.println("The member has been successfully added.");
            return;
        }
        else {
            System.out.println("The member already exists.");
        }
    }

    public void removeMember(String memberId){
        if (!members.containsKey(memberId)){
            System.out.println("The member couldn't be found.");
            return;
        }
        else {
            members.remove(memberId);
            System.out.println("The member has been successfully removed.");
        }
    }

    public Member getMember(String memberId){
        if (members.containsKey(memberId)){
            return members.get(memberId);
        }
        else {
            return null;
        }
    }

    public void updateMember(String memberId, String newName){
        if (!members.containsKey(memberId)){
            System.out.println("The member couldn't be found.");
            return;
        }
        else {
            members.get(memberId).setName(newName);
            System.out.println("The member has been successfully updated.");
        }
    }

    public boolean borrowBook(String memberId, String bookId){
        if (!books.containsKey(bookId)){
            System.out.println("The book couldn't be found.");
            return false; 
        }
        if (!members.containsKey(memberId)){
            System.out.println("The member couldn't be found.");
            return false; 
        }
        if (!books.get(bookId).isAvailable()){
            System.out.println("The book has already been borrowed.");
            return false;
        }
        members.get(memberId).borrowedBookIds.add(bookId);
        return true;
    }

    public boolean returnBook(String memberId, String bookId){
        if (!books.containsKey(bookId)){
            System.out.println("The book couldn't be found.");
            return false; 
        }
        if (!members.containsKey(memberId)){
            System.out.println("The member couldn't be found.");
            return false; 
        }
        if (books.get(bookId).isAvailable()){
            System.out.println("The book has already been returned.");
            return false;
        }
        members.get(memberId).borrowedBookIds.remove(bookId);
        return true;
    }

    public void printAllBooks(){
        for (Entry<String, Book> book: books.entrySet()) {
            book.toString();
            System.out.println();
        }
    }

    public void printAllMembers(){
        for (Entry<String, Member> member: members.entrySet()) {
            member.toString();
            System.out.println();
        }
    }
}
