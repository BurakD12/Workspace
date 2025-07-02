package Question2;

public class DVD extends LibraryItem{
    private String director;

    DVD(String name, int yearPublished, String director){
        super(name, yearPublished);
        this.director = director;
    }

    @Override
    String getInfo() {
        return "Title: " + this.getTitle() + ", Year: " + this.getYearPublished() + ", Director: " + this.director;
    }

    @Override
    public String toString() {
        return "DVD [director=" + director + "]";
    }

    
}
