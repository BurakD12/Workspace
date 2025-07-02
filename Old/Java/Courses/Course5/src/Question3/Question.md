Question: Library Management System

Task: You are required to create a basic library management system using Java. Your system will involve multiple classes representing different components of the library system: Book, Library, and LibraryUser.

Book Class:

Attributes: The Book class should have the following private attributes:
    title (String)
    author (String)
    isbn (String)
Constructor: Define a parameterized constructor to initialize the title, author, and isbn.
Getters: Implement getter methods for each attribute.

---------------------------------------------------------------------------------

LibraryUser Class:

Attributes: The LibraryUser class should have the following private attributes:
    name (String)
    userId (String)
Constructor: Define a parameterized constructor to initialize the name and userId.
Getters: Implement getter methods for each attribute.

---------------------------------------------------------------------------------

Library Class:

Attributes: The Library class should have the following private attributes:
    books (a list of Book objects)
    users (a list of LibraryUser objects)
Methods:
addBook(Book book): Adds a new book to the library.
removeBook(String isbn): Removes the book with the given ISBN.
addUser(LibraryUser user): Adds a new user to the library.
findBooksByAuthor(String author): Returns a list of books written by the specified author.
displayAllBooks(): Prints details of all the books in the library.

---------------------------------------------------------------------------------

Implementation in Main:

In your main method, create an instance of Library.
Add a few Book and LibraryUser instances to the library.
Demonstrate the use of findBooksByAuthor and displayAllBooks methods.
Remove a book and show the updated list of books.

---------------------------------------------------------------------------------

Example Output:

Adding books and users to the library...
Displaying all books:
Title: Book1, Author: AuthorA, ISBN: 1111
Title: Book2, Author: AuthorB, ISBN: 2222
Title: Book3, Author: AuthorA, ISBN: 3333

Finding books by AuthorA...
Title: Book1, Author: AuthorA, ISBN: 1111
Title: Book3, Author: AuthorA, ISBN: 3333

Removing Book2...
Updated book list:
Title: Book1, Author: AuthorA, ISBN: 1111
Title: Book3, Author: AuthorA, ISBN: 3333

---------------------------------------------------------------------------------

Notes:

Ensure to handle edge cases, such as trying to remove a book that doesn't exist in the library.
Make use of appropriate data structures to manage the collections of books and users efficiently.
This task focuses on class design, object creation, and basic interactions between different objects within a system.