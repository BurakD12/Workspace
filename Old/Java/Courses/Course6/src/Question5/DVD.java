package Question5;

public class DVD extends Item{
    private String director;
    private int runtime;

    DVD(String title, String director, int runtime){
        super(title);
        this.director = director;
        this.runtime = runtime;
    }

    String getDVDInfo(){
        return "Title: " + this.getTitle() + ", Director: " + this.director + ", Runtime: " + this.runtime;
    }

    @Override
    void displayDetails() {
        System.out.println(this.getDVDInfo() + ", Available: " + this.isAvailable());
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public int getRuntime() {
        return runtime;
    }

    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }
    
    
}
