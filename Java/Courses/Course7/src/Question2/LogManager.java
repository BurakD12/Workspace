package Question2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {
    public static void logInfo(String message){
        File infoFile = new File("src/Question2/info.log");
        FileWriter fileWriter;

        Date today = new Date();
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd:MM:yyyy 'T' HH:mm:ss");
        String date = DATE_FORMAT.format(today);
        try {
            fileWriter = new FileWriter(infoFile, true);
            fileWriter.write("[" + date + "] " + message + "\n");
            fileWriter.close();
        } catch (IOException e) {
            e.getMessage();
        }
    }
    public static void logError(String message){
        File infoFile = new File("src/Question2/error.log");
        FileWriter fileWriter;

        Date today = new Date();
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd:MM:yyyy 'T' HH:mm:ss");
        String date = DATE_FORMAT.format(today);
        try {
            fileWriter = new FileWriter(infoFile, true);
            fileWriter.write("[" + date + "] " + message + "\n");
            fileWriter.close();
        } catch (IOException e) {
            e.getMessage();
        }
    }
    public static void logTransaction(String message){
        File infoFile = new File("src/Question2/transactions.log");
        FileWriter fileWriter;

        Date today = new Date();
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd:MM:yyyy 'T' HH:mm:ss");
        String date = DATE_FORMAT.format(today);
        try {
            fileWriter = new FileWriter(infoFile, true);
            fileWriter.write("[" + date + "] " + message + "\n");
            fileWriter.close();
        } catch (IOException e) {
            e.getMessage();
        }
    }
}
