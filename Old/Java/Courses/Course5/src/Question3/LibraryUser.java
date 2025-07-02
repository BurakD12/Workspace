package Question3;

public class LibraryUser {
    private String name;
    private String user_ID;

    LibraryUser(String name, String user_ID){
        this.name = name;
        this.user_ID = user_ID;
    }

    String getName(){
        return this.name;
    }

    String getUserID(){
        return this.user_ID;
    }
}
