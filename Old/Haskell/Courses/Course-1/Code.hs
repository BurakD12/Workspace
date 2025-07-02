{- 

Numbers
Haskell is intelligent enough to decode some number as a number. Therefore, you need not mention its type externally as we usually do in case of other programing languages. As per example go to your prelude command prompt and just run "2+2" and hit enter.

sh-4.3$ ghci 
GHCi, version 7.6.3: http://www.haskell.org/ghc/  :? for help 
Loading package ghc-prim ... linking ... done. 
Loading package integer-gmp ... linking ... done. 
Loading package base ... linking ... done. 
Prelude> 2+2
You will receive the following output as a result.

4
In the above code, we just passed two numbers as arguments to the GHCI compiler without predefining their type, but compiler could easily decode these two entries as numbers.

Now, let us try a little more complex mathematical calculation and see whether our intelligent compiler give us the correct output or not. Try with "15+(5*5)-40"

Prelude> 15+(5*5)-40 
The above expression yields "0" as per the expected output.

0

-----------------------------------------------------------------------------------------------

Characters
Like numbers, Haskell can intelligently identify a character given in as an input to it. Go to your Haskell command prompt and type any character with double or single quotation.

Let us provide following line as input and check its output.

Prelude> :t "a" 
It will produce the following output −

"a" :: [Char] 
Remember you use (:t) while supplying the input. In the above example, (:t) is to include the specific type related to the inputs. We will learn more about this type in the upcoming chapters.

Take a look at the following example where we are passing some invalid input as a char which in turn leads to an error.

Prelude> :t a 
<interactive>:1:1: Not in scope: 'a'  

Prelude> a 
<interactive>:4:1: Not in scope: 'a' 
By the error message "<interactive>:4:1: Not in scope: `a'" the Haskell compiler is warning us that it is not able to recognize your input. Haskell is a type of language where everything is represented using a number.

Haskell follows conventional ASCII encoding style. Let us take a look at the following example to understand more −

Prelude> '\97' 
'a'  
Prelude> '\67' 
'C' 
Look how your input gets decoded into ASCII format.

-----------------------------------------------------------------------------------------------

String
A string is nothing but a collection of characters. There is no specific syntax for using string, but Haskell follows the conventional style of representing a string with double quotation.

Take a look at the following example where we are passing the string “Tutorialspoint.com”.

Prelude> :t "tutorialspoint.com" 
It will produce the following output on screen −

"tutorialspoint.com" :: [Char] 
See how the entire string has been decoded as an array of Char only. Let us move to the other data type and its syntax. Once we start our actual practice, we will be habituated with all the data type and its use.

-----------------------------------------------------------------------------------------------

Boolean
Boolean data type is also pretty much straightforward like other data type. Look at the following example where we will use different Boolean operations using some Boolean inputs such as "True" or "False".

Prelude> True && True 
True  
Prelude> True && False 
False   
Prelude> True || True 
True  
Prelude> True || False 
True
In the above example, we need not mention that "True" and "False" are the Boolean values. Haskell itself can decode it and do the respective operations. Let us modify our inputs with "true" or "false".

Prelude> true 
It will produce the following output −

<interactive>:9:1: Not in scope: 'true' 
In the above example, Haskell could not differentiate between "true" and a number value, hence our input "true" is not a number. Hence, the Haskell compiler throws an error stating that our input is not its scope.

-----------------------------------------------------------------------------------------------

List and List Comprehension
Like other data types, List is also a very useful data type used in Haskell. As per example, [a,b,c] is a list of characters, hence, by definition, List is a collection of same data type separated by comma.

Like other data types, you need not declare a List as a List. Haskell is intelligent enough to decode your input by looking at the syntax used in the expression.

Take a look at the following example which shows how Haskell treats a List.

Prelude> [1,2,3,4,5] 
It will produce the following output −

[1,2,3,4,5] 
Lists in Haskell are homogeneous in nature, which means they won’t allow you to declare a list of different kind of data type. Any list like [1,2,3,4,5,a,b,c,d,e,f] will produce an error.

Prelude> [1,2,3,4,5,a,b,c,d,e,f] 
This code will produce the following error −

<interactive>:17:12: Not in scope: 'a' 
<interactive>:17:14: Not in scope: 'b' 
<interactive>:17:16: Not in scope: 'c' 
<interactive>:17:18: Not in scope: 'd' 
<interactive>:17:20: Not in scope: 'e' 
<interactive>:17:22: Not in scope: 'f'

-----------------------------------------------------------------------------------------------

List Comprehension
List comprehension is the process of generating a list using mathematical expression. Look at the following example where we are generating a list using mathematical expression in the format of [output | range ,condition].

Prelude> [x*2| x<-[1..10]] 
[2,4,6,8,10,12,14,16,18,20]  
Prelude> [x*2| x<-[1..5]] 
[2,4,6,8,10]  
Prelude> [x| x<-[1..5]] 
[1,2,3,4,5]
This method of creating one List using mathematical expression is called as List Comprehension.

-----------------------------------------------------------------------------------------------

Tuple
Haskell provides another way to declare multiple values in a single data type. It is known as Tuple. A Tuple can be considered as a List, however there are some technical differences in between a Tuple and a List.

A Tuple is an immutable data type, as we cannot modify the number of elements at runtime, whereas a List is a mutable data type.

On the other hand, List is a homogeneous data type, but Tuple is heterogeneous in nature, because a Tuple may contain different type of data inside it.

Tuples are represented by single parenthesis. Take a look at the following example to see how Haskell treats a Tuple.

Prelude> (1,1,'a') 
It will produce the following output −

(1,1,'a') 
In the above example, we have used one Tuple with two number type variables, and a char type variable.

-}

main = do
    let var1 = 10
    let var2 = 3 
    putStr "The division of the two numbers is: "
    print(var1 / var2)