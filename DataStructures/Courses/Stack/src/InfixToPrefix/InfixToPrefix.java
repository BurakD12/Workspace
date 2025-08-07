package InfixToPrefix;

import java.util.Stack;

public class InfixToPrefix {
    
    public static String infixToPrefix(String infix){
        Stack<Character> operatorStack = new Stack<>();
        Stack<Character> operandStack = new Stack<>();
        String ans = "";
        char[] infixArray = infix.toCharArray();
        
        for (int i = 0; i < infixArray.length; i++) {

        }
    }
    
    public static boolean isOperator(char c){
        if (c == '^' || c == '+' || c == '-' || c == '*' || c == '/'){
            return true;
        }
        return false;
    }
    
    public static int priority(char c){
        if (c == '^'){
            return 3;
        }
        if (c == '*' || c == '/'){
            return 2;
        }
        if (c == '+' || c == '-'){
            return 1;
        }
        return -1;
    }
}
