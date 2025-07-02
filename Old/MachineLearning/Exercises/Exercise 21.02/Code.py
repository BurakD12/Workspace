# -----------------------------------------------------------------------------------------------------------------------
# Sum of Two Numbers: Write a program to take two integers as input from the user and print the sum of those numbers.

# x, y = int(input('Enter your 1st number: ')), int(input('Enter your 2nd number: '))
# sum = x + y

# print(sum)

# -----------------------------------------------------------------------------------------------------------------------
# Maximum of Three Numbers: Write a Python script to find the maximum of three numbers entered by the user.

# x, y, z = int(input('Enter number 1: ')), int(input('Enter number 2: ')), int(input('Enter number 3: '))

# if x > y and x > z:
#     if y > z:
#         print(f'{x} > {y} > {z}') 
#     else:
#         print('{} > {} > {}'.format(x, z, y))
# elif y > z:
#     if x > z:
#         print('{} > {} > {}'.format(y, x, z))
#     else:
#         print('{} > {} > {}'.format(y, z, x))
# else:
#     if x > y:
#         print('{} > {} > {}'.format(z, x, y))
#     else:
#         print('{} > {} > {}'.format(z, y, x))        

# -----------------------------------------------------------------------------------------------------------------------
# Factorial Calculation: Write a program to compute the factorial of a number provided by the user.

# x, product = int(input('Enter the number: ')), 1
# for i in range(x, 1, -1):
#     product *= i
    
# print(product)

# -----------------------------------------------------------------------------------------------------------------------
#Check Prime Number: Write a program that asks the user for a number and prints whether it is a prime number or not.

# x = int(input('Enter your number: '))
# prime = True
# for i in range(x // 2, 2, -1):
#     if x % i == 0:
#         prime = False
#         break
    
# if x <= 1 or not prime:
#     print('The number is not prime!')
# else: 
#     print('The number is prime!')

# -----------------------------------------------------------------------------------------------------------------------
# Print Fibonacci Series: Write a program to display the Fibonacci series up to the nth term (n being entered by the user).

# n = int(input('Enter the sequence term: '))
# x, y, z = 1, 1, 0

# for i in range(1, n - 1):
#     if i == 1:
#         print(f'{y}, {x}', end=', ')
#     z = x
#     x += y
#     y = z
#     print(x, end=', ')

# f,s = 1, 1
# nxt = f + s

# for i in range(0, n):

#     if i == 0:
#         print(1,end=' ')
#     elif i == 1:
#         print(1,end=' ')
#         f = s
#         s = nxt
#     else:
#         print(nxt,end=' ')
#         nxt = f + s
#         f = s
#         s = nxt

# -----------------------------------------------------------------------------------------------------------------------
# Find the Largest Number in a List: Write a program to find the largest number in a list given by the user.

# num_list = []

# for i in range(0, 9):
#     x = int(input('Enter a number into the list: '))
#     num_list.append(x)
    
# largest_num = num_list[0]

# for i in range(0, 9):
#     if largest_num < num_list[i]:
#         largest_num = num_list[i]

# print(largest_num)

# -----------------------------------------------------------------------------------------------------------------------
# Linear Search: Implement linear search in Python, where the program searches for a specified element in a list provided by the user.

# num_list = []
# exist = False
# target_num = 3

# for i in range(0, 4):
#     x = int(input('Enter a number for the list: '))
#     num_list.append(x)
    
# for i in range(0, 4):
#     if num_list[i] == target_num:
#         exist = True
#         print(f'The target value exists in the index {i}')
        
# if exist == False:
#     print('The target value does not exist')

#Sum of elements

# num_list = []
# x, y = 0, int(input("How many numbers will be there: "))

# for i in range(0, y):
#     x = int(input("Enter a number to add: "))
#     num_list.append(x)

# sum = 0
# for i in range(0, y ):
#     sum += num_list[i]
# print(sum)

# ----------------------------------------------------------------------

# Reverse a string

# x = input('Enter a string:')
# ans = x[::-1]
# print(ans)

# ----------------------------------------------------------------------

#Palindrome check: Write a program to check if the given string by the user is a palindrome or not.

# x = input('Enter a word for the palindrome check: ').lower()
# ans = x[::-1]
# if (x == ans):
#     print('This is a palindrome!')
# else:
#     print('This is not a palindrome!')

#-----------------------------------------------------------------------

#Count vowels in a string

# x = input('Enter a string:')
# y = x.lower()
# count = 0

# for i in range(0, len(y)):
#     if y[i] in 'aeiou':
#         count += 1

# print(f'There are {count} vowels in the string')

# ------------------------------------------------------------------------------

#Merge Two Lists: Write a program to merge two lists input by the user and sort the merged list


# x, y = [], []

# print('Fill the first list: ')
# for i in range(0, 5):
#     x.append(int(input(f'Enter value number {i + 1}: ')))

# print('Fill the second list: ')
# for i in range(0, 5):
#     y.append(int(input(f'Enter value number {i + 1}: ')))

# z = x
# z.extend(y)
# z.sort()

# print(z)

# first_list_input = input("Enter the elements of the first list seperated by comma: ")
# first_list = [int(item.strip()) for item in first_list_input.split(",")] # 1,2,3,4,5

# second_list_input = input("Enter the elements of the first list seperated by comma: ")
# second_list = [int(item.strip()) for item in second_list_input.split(",")]

# merged_list = first_list + second_list
# sorted_list = sorted(merged_list)
# print(sorted_list)

#-----------------------------------------------------------------------------

#Find the Second Largest Number in a List: Write a program to find the second largest number in a list provided by the user.

# list_input = input('Enter the elements separated by a comma: ')
# main_list = [int(item.strip()) for item in list_input.split(',')]

# main_list.sort()

# count1 = main_list[len(main_list) - 1]
# count2 = main_list[len(main_list) - 2]

# for i in range(1, len(main_list) + 1):
#     if count1 == count2:
#         count2 = main_list[len(main_list) - (2 + i)]
#     else:
#         print(count2)
#         break

# list_input = input('Enter the elements separated by a comma: ')
# main_list = [int(item.strip()) for item in list_input.split(',')]

# if len(set(main_list)) < 2:
#     print("Error")

# else:
#     max_num = max(main_list)
#     while max_num in main_list:
#         main_list.remove(max_num)

#     print("Ans is " + str(max(main_list)))

#----------------------------------------------------------------------

#Sum of Numbers in a Given Range: Write a program that asks the user for two numbers, start and end, and prints the sum of all numbers in that range, inclusive.

# x, y, sum = int(input('Enter number 1: ')), int(input('Enter number 2: ')), 0
# for i in range(x, y + 1):
#     sum += i
# print(sum)

# x = int(input('Enter number 1: '))
# y = int(input('Enter number 2: '))

# sum = sum(range(x, y+1))
# print(sum)

#----------------------------------------------------------------------

#Check Leap Year: Write a program to check if the year entered by the user is a leap year or not.

# x = int(input('Enter the year: '))
# leap = False
# if x % 4 == 0:
#     if x % 100 == 0 and x % 400 == 0:
#         leap = True
#     elif x % 100 == 0 and x % 400 != 0:
#         leap = False
#     else:
#         leap = True
# print(leap)

#------------------------------------------------------------------------

#Convert Celsius to Fahrenheit: Write a program that takes the temperature in Celsius from the user and converts it to Fahrenheit.

# x = int(input('Enter the degree in C:'))
# x *= 9
# x/= 5
# x += 32

# print(f'The temperature in F is: {x}')

#--------------------------------------------------------------------------

#Count the Number of Digits in a Number: Write a program to count the number of digits in a number provided by the user.

# x = int(input('Enter a number: '))
# print(len(str(x)))

#-------------------------------------------------------------------------

#Remove Duplicates from a List: Write a program to remove duplicate elements from a list input by the user.

# numbers_input = input("Enter the elements of the list separated by commas: ")
# numbers = [int(item.strip()) for item in numbers_input.split(',')]

# unique_numbers = list(set(numbers))

# print(unique_numbers)

#-------------------------------------------------------------------------

#Check for Balanced Parentheses: Write a program that checks if the parentheses ((), {}, []) in a string are balanced or not.

# def is_balanced(expression):
#     pairs = {')':'(', '}':'{', ']':'['}
#     stack = []

#     for char in expression:
#         if char in '({[':
#             stack.append(char)

#         elif char in ')}]':
#             if not stack or stack[-1] != pairs[char]:
#                 return False
#             stack.pop()

#     return not stack
    
# x = input('Enter a string: ')

# if is_balanced(x):
#     print("Expression is balanced")
# else:
#     print("Expression is not balanced")

#-----------------------------------------------------------------------------

#Find All Prime Factors of a Number: Write a program that finds all the prime factors of a number provided by the user.

# num = int(input('Enter a number: '))
# factors = []
# i = 2

# while i * i <= num:
#     if num % i != 0:
#         i += 1
#     else:
#         num //= i
#         factors.append(i)

# if num > 1:
#     factors.append(num)

# print(set(factors))

# i = 2
# while num != 1:
#     if num % i == 0:
#         factors.append(i)
#         num //= i
#     else:
#         i += 1

# y = set(factors)
# y = list(y)
# print(y)

#-----------------------------------------------------------------

# Encode and Decode Strings: Write a program that implements a basic algorithm to encode and decode strings.

# ABCDE
# DEFGH

def encode(text:str, key:int):
    ans = ''
    for char in text:
        ans += chr(ord(char) + key)
    return ans

def decode(text:str, key:int):
    ans = ''
    for char in text:
        ans += chr(ord(char) - key)
    return ans

print(encode('ABCDE',3))
print(decode(encode('ABCDE',3),3))