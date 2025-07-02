import java.lang.Math;
public class App {
    
    public static void main(String[] args) throws Exception {
        System.out.println(CaesarEncrypter("z", 3));        System.out.println(CaesarDecoder("c", 3));

        System.out.println(BinaryToDecimal("1101"));
        System.out.println(DecimalToBinary(13));
    }

    /*
     * Question-1: Implement a method that takes a string encrypts it using a simple
     * Caesar cipher with a shift of given number N.
     */

    /*
     * Question-2: Implement a method that takes a string and decodes it using a
     * simple Caesar cipher with a shift of given number N.
     */
    static String CaesarEncrypter(String string, int caesarShift) {
        char[] arr = string.toCharArray();
        int count = 0;
        for (char ch : arr) {
            if (64 < ch && ch < 91) {
                ch = (char) (ch + caesarShift);
                if (ch > 91) {
                    int diff = ch - 91;
                    ch = (char) (64 + diff);
                }

                if (ch < 65) {
                    int diff = ch - 65;
                    ch = (char) (91 + diff);
                }
            }

            if (96 < ch && ch < 123) {
                ch += caesarShift;
                if (ch > 123) {
                    int diff = ch - 122;
                    ch = (char) (96 + diff);
                }

                if (ch < 97) {
                    int diff = ch - 97;
                    ch = (char) (123 + diff);
                }
            }
            arr[count] = ch;
            count++;

        }
        return String.valueOf(arr);
    }

    static String CaesarDecoder(String string, int caesarShift) {
        char[] arr = string.toCharArray();
        int count = 0;
        for (char ch : arr) {
            if (64 < ch && ch < 91) {
                ch = (char) (ch - caesarShift);
                if (ch > 91) {
                    int diff = ch - 91;
                    ch = (char) (65 + diff);
                }

                if (ch < 65) {
                    int diff = ch - 65;
                    ch = (char) (91 + diff);
                }
            }

            if (96 < ch && ch < 123) {
                ch -= caesarShift;
                if (ch > 123) {
                    int diff = ch - 122;
                    ch = (char) (96 + diff);
                }

                if (ch < 97) {
                    int diff = ch - 97;
                    ch = (char) (123 + diff);
                }
            }
            arr[count] = ch;
            count++;

        }
        return String.valueOf(arr);
    }


    /*
        Question-3: Implement a method that takes a binary string and converts it to a decimal number.

        Example:
        Input: "1101" 1011
        Output: 13
    */

    static int BinaryToDecimal(String binary) {
        // 1101
        char[] arr = binary.toCharArray();
        int finalNumber = 0;
        int decimalPoint = arr.length - 1;
        for (int i = 0; i < arr.length; i++){
            finalNumber = finalNumber + (int)Math.pow(2, decimalPoint - i) * (arr[i] - 48);
        }  
        return finalNumber;
    }

    static String DecimalToBinary(int decimal) {
        String finalString = "";
        while (decimal > 0) {
            if (decimal % 2 == 0){
                finalString = "0" + finalString;
            }
            else  if (decimal % 2 == 1){
                finalString = "1" + finalString;
            }
            decimal /= 2;
        } 

        return finalString;
    }
}
