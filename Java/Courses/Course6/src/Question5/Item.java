package Question5;

public abstract class Item {
    private String title;
    private boolean available;

    Item(String title){
        this.title = title;
        this.available = true;
    }

    void checkOut(){
        this.available = false;
    }

    void returnItem(){
        this.available = true;
    }

    abstract void displayDetails();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    
}
