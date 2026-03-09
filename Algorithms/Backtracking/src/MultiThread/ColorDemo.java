package MultiThread;
public class ColorDemo {

    public static void main(String[] args) {
        
        // Red
        System.out.println(ConsoleColor.RED + "This is red text." + ConsoleColor.RESET);
        // Green
        System.out.println(ConsoleColor.GREEN + "This is green text." + ConsoleColor.RESET);
        // Yellow   
        System.out.println(ConsoleColor.YELLOW + "This is yellow text." + ConsoleColor.RESET);
        // Blue
        System.out.println(ConsoleColor.BLUE + "This is blue text." + ConsoleColor.RESET);
        // Purple
        System.out.println(ConsoleColor.PURPLE + "This is purple text." + ConsoleColor.RESET);
        // Cyan
        System.out.println(ConsoleColor.CYAN + "This is cyan text." + ConsoleColor.RESET);
        // White
        System.out.println(ConsoleColor.WHITE + "This is white text." + ConsoleColor.RESET);
    }
}

enum ConsoleColor {
    RESET("\u001B[0m"),
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    YELLOW("\u001B[33m"),
    BLUE("\u001B[34m"),
    PURPLE("\u001B[35m"),
    CYAN("\u001B[36m"),
    WHITE("\u001B[37m");
    private final String code;
    ConsoleColor(String code) {
        this.code = code;
    }
    @Override
    public String toString() {
        return code;
    }
}