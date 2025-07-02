package Question2;

public class LibraryItem {
    private String title;
    private int yearPublished;

    LibraryItem(String title, int yearPublished){
        this.title = title;
        this.yearPublished = yearPublished;
    }

    String getInfo(){
        return "Title: " + this.title + ", Year: " + this.yearPublished;
    } 

    public String getTitle() {
        return title;
    }

    public int getYearPublished() {
        return yearPublished;
    }

    // @Override
    // public String toString() {
    //     return "LibraryItem [title=" + title + ", yearPublished=" + yearPublished + " ";
    // }
    

}
