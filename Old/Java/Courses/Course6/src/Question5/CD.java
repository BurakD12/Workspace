package Question5;

public class CD extends Item{
    private String artist;
    private int numOfTracks;

    CD(String title, String artist, int numOfTracks){
        super(title);
        this.artist = artist;
        this.numOfTracks = numOfTracks;
    }

    String getCDInfo(){
        return "Title: " + this.getTitle() + ", Artist: " + this.artist + ", Number of Tracks: " + this.numOfTracks;
    }

    @Override
    void displayDetails() {
        System.out.println(this.getCDInfo() + ", Available: " + this.isAvailable());
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getNumOfTracks() {
        return numOfTracks;
    }

    public void setNumOfTracks(int numOfTracks) {
        this.numOfTracks = numOfTracks;
    }

    
}
