package Q1;

import java.util.ArrayList;

public class Member {
    String memberId;
    String name;
    ArrayList<String> borrowedBookIds;

    public Member(String memberId, String name){
        this.memberId = memberId;
        this.name = name;
        this.borrowedBookIds = new ArrayList<String>();
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getBorrowedBookIds() {
        return borrowedBookIds;
    }

    public void setBorrowedBookIds(ArrayList<String> borrowedBookIds) {
        this.borrowedBookIds = borrowedBookIds;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("The members ID is: ").append(this.memberId);
        sb.append("\nThe members name is: ").append(this.name);
        if (this.borrowedBookIds.isEmpty()) {
            sb.append("No books borrowed.");
        } else {
            sb.append("\nThe member has borrowed the following books: ");
            for (String bookId : this.borrowedBookIds) {
                Book b = Library.books.get(bookId);
                if (b != null) {
                    sb.append("\n").append(b.getTitle());
                    sb.append("\n").append(b.getAuthor()).append("\n");
                }
            }
        }
        return sb.toString();
    }
}
