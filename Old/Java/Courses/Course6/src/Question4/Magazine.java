package Question4;

public class Magazine extends Item{
    private int issueNumber;

    Magazine(int id, String title, double price, int issueNumber){
        super(id, title, price);
        this.issueNumber = issueNumber;
    }

    @Override
    String getDetails() {
        return "ID: " + this.getId() + ", Title: " + this.getTitle() + ", Price: " + this.getPrice() + ", Issue Number: " + this.issueNumber;
    }
}
