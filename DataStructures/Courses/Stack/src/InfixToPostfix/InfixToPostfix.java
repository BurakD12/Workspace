package InfixToPostfix;

import java.util.Stack;

public class InfixToPostfix {
    public static String infixToPostfix(String infix){
        Stack<Character> operatorStack = new Stack<>();
        String ans = "";
        char[] infixArray = infix.toCharArray();

        for (int i = 0; i < infixArray.length; i++) {
            if (infixArray[i] >= 'A' && infixArray[i] <= 'Z' 
            || infixArray[i] >= 'a' && infixArray[i] <= 'z' 
            || infixArray[i] >= '0' && infixArray[i] <= '9'){
                ans += infixArray[i];
            }
            else if (infixArray[i] == '('){
                operatorStack.push(infixArray[i]);
            }
            else if (infixArray[i] == ')'){
                while (!operatorStack.isEmpty() && operatorStack.peek() != '(') {
                    ans += operatorStack.pop();
                }
                operatorStack.pop();
            }
            else {
                while (!operatorStack.isEmpty() &&
                 priority(infixArray[i]) <= priority(operatorStack.peek())) {
                    ans += operatorStack.pop();
                }
                operatorStack.push(infixArray[i]);
            }
            while (!operatorStack.isEmpty()) {
                ans += operatorStack.pop();
            }
        }
        return ans;
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
