package Exercise.Ex3;

public class Member extends Person{
    private String membershipType;
    private int maxBooksAllowed;

    public Member(){
        super();
        this.membershipType = "Unknown";
        this.maxBooksAllowed = 5;
    }

    public Member(int id, String name, String email, String membershipType, int maxBooksAllowed){
        super(id, name, email);
        this.membershipType = membershipType;
        this.maxBooksAllowed = maxBooksAllowed;
    }

    public String getMembershipType() {
        return membershipType;
    }

    public void setMembershipType(String membershipType) {
        this.membershipType = membershipType;
    }

    public int getMaxBooksAllowed() {
        return maxBooksAllowed;
    }

    public void setMaxBooksAllowed(int maxBooksAllowed) {
        this.maxBooksAllowed = maxBooksAllowed;
    }
    
    @Override
    public void displayDetails() {
        System.out.println("The ID of the member is: " + this.getId() + 
        "\nThe name of the member is: " + this.getName() + 
        "\nThe email of the member is: " + this.getEmail() + 
        "\nThe membership type of the member is: " + this.membershipType + 
        "\nThey are allowed to lease " + this.maxBooksAllowed + " amount of books.");
    }


}
