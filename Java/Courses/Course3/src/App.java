import java.util.Arrays;
import java.util.Scanner;

public class App {
    public static void main(String[] args) throws Exception {

        // Kullanicinin girdigi sayiya kadar olan sayilarin toplamini bulan programi yaz
        // int sum = 0, x;
        // Scanner scan = new Scanner(System.in);
        // System.out.print("Enter a number: ");
        // x = scan.nextInt();
        // for (int i = 0; i <= x; i++){
        // sum = i + sum;
        // }
        // System.out.print("The sum of the numbers up until the given value is: " +
        // sum);

        // Kullanicinin girdigi sayinin faktoriyelini bulan programi yaz

        // int fac = 1, x;
        // Scanner scan = new Scanner(System.in);
        // System.out.print("Enter a number: ");
        // x = scan.nextInt();

        // for (int i = 1; i <= x; i++){
        // fac = fac * i;
        // }
        // System.out.print("The factorial of the given number is: " + fac);

        // Kullanicidan 'taban' ve 'kuvvet' bilgisi iste. taban^kuvvet i hesapla

        // int base, pow, fin = 1;
        // Scanner scan = new Scanner(System.in);
        // System.out.print("Enter the base number: ");
        // base = scan.nextInt();
        // System.out.println("Enter the power: ");
        // pow = scan.nextInt();

        // for (int i = 0; i < pow; i++){
        // fin = fin * base;
        // }

        // System.out.println("The final product is: " + fin);

        // Kullanicidan bir sayi al ve bu sayi asal mi degil mi printle

        // Scanner scan = new Scanner(System.in);

        // System.out.print("Enter a number to ask: ");
        // int x = scan.nextInt();

        // boolean isPrime = true;
        // if (x < 2) {
        // isPrime = false;
        // }

        // // N ==> sqrt(N) * sqrt(N)

        // for (int i = 2; i <= (int)Math.sqrt(x); i++) {
        // if (x % i == 0) {
        // isPrime = false;
        // break;
        // }
        // }

        // if (isPrime == true) {
        // System.out.println("The number is prime!");
        // } else {
        // System.out.println("The number is not prime!");
        // }

        // Kullanicidan 2 sayi al ve alinan sayilari bolebilen en buyuk sayiyi bulup
        // printle

        // Scanner scan = new Scanner(System.in);
        // System.out.println("Enter 2 numbers: ");

        // System.out.print("The 1st one: ");
        // int x = scan.nextInt(), fin = 0, min = 0;

        // System.out.print("The 2nd one: ");
        // int y = scan.nextInt();

        // if (x < y) {
        // min = x;
        // }
        // else {
        // min = y;
        // }

        // for (int i = min; i > 0; i--) {
        // if (x % i == 0 && y % i == 0) {
        // fin = i;
        // break;
        // }
        // }

        // System.out.println("The greatest common diviser is: " + fin);

        // Kullanicidan 2 sayi al ve alinan sayilarin en kucuk ortak katini bulup
        // printle (LCM) (EKOK)

        // Scanner scan = new Scanner(System.in);
        // System.out.print("Enter 2 numbers: ");
        // int x = scan.nextInt();
        // int y = scan.nextInt(), max = 0;

        // if (x >= y){
        // max = x;
        // }
        // else {
        // max = y;
        // }

        // for (int i = max; ; i += max){
        // if (i % x == 0 && i % y == 0){
        // System.out.println("The least common multiplier for the 2 numbers is: " + i);
        // break;
        // }
        // }

        // Kullanicinin doldurugu array'i sort eden kody yaz.

        // Scanner scan = new Scanner(System.in);
        // int[] arr = new int[10]; // int arr[5];

        // System.out.println("Insert numbers into the 10 wide array:");

        // for (int i = 0; i < 10; i++){
        // arr[i] = scan.nextInt();
        // }

        // for (int i = 0; i < 10; i++){
        // for (int j = 0; j < 9 - i; j++){
        // if (arr[j] > arr[j + 1]){
        // int x = arr[j];
        // arr[j] = arr[j + 1];
        // arr[j + 1] = x;
        // }
        // }
        // }

        // System.out.println(Arrays.toString(arr));

        // Let user fill an array of integers and then find out wether given array is
        // sorted or not!

        // boolean ascsorted = true;
        // boolean dessorted = true;
        // Scanner scan = new Scanner(System.in);
        // int[] arr = new int[10];

        // System.out.println("Enter 10 numbers into the array:");

        // for (int i = 0; i < 10; i++) {
        // arr[i] = scan.nextInt();
        // }

        // for (int i = 0; i < 9; i++){
        // if (arr [i] > arr[i + 1]){
        // dessorted = false;
        // }
        // if (arr[i] < arr[i + 1]) {
        // ascsorted = false;
        // }
        // }
        // if (dessorted == false && ascsorted == false){
        // System.out.println("The array is not sorted!");
        // }
        // else{
        // System.out.println("The array is sorted!");
        // }

        // Write a java code to merge 2 sorted arrays into merged array as sorted.

        // int[] arr1 = { 3, 7, 12, 18, 25 };
        // int[] arr2 = { 2, 4, 6, 8, 10 };
        // int[] arrfin = new int[arr1.length + arr2.length];

        // int x = arr1.length, y = arr2.length;

        // for (int i = 0, j = 0, k = 0; i < arrfin.length; i++){
        // if (k < x && j < y){
        // if (arr1[k] < arr2[j]){
        // arrfin[i] = arr1[k];
        // k++;
        // }
        // else{
        // arrfin[i] = arr2[j];
        // j++;
        // }
        // }
        // else if (k == x && j < y){
        // arrfin[i] = arr2[j];
        // j++;
        // }
        // else if (k < x && j == y){
        // arrfin[i] = arr1[k];
        // k++;
        // }
        // }

        // int i = 0, j = 0, k = 0;
        // while (i < arr1.length && j < arr2.length) {
        // if (arr1[i] < arr2[j]) {
        // arrfin[k] = arr1[i++];
        // }
        // else {
        // arrfin[k] = arr2[j++];
        // }
        // k++;
        // }

        // while (i < arr1.length) {
        // arrfin[k++] = arr1[i++];
        // }

        // while (j < arr2.length) {
        // arrfin[k++] = arr2[j++];
        // }

        // System.out.println(Arrays.toString(arrfin));

        // x x x x x x x x x x x x x x
        // S E

        // Scanner scan = new Scanner(System.in);
        // int[] mainArr = { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };

        // int start = 0, end = mainArr.length - 1;

        // int target = 10;
        // int found = -1;

        // while (start <= end) {
        //     int midIdx = start + ((end - start) / 2);
        //     if (mainArr[midIdx] == target) {
        //         found = midIdx;
        //         break;
        //     }
        //     if (target > mainArr[midIdx]) {
        //         start = midIdx + 1;
        //     } else {
        //         end = midIdx - 1;
        //     }
        // }

        // if (found != -1) {
        //     System.out.println("Element found at position " + found);
        // }
        // else {
        //     System.out.println("Element is not found");
        // }
    }
}
