Question:

Create a Java class named Book that represents a book in a library. The Book class should have the following private attributes:

String title: the title of the book.
String author: the author of the book.
int yearPublished: the year the book was published.
boolean isAvailable: a flag indicating whether the book is available for checkout.

Implement the following constructors in the Book class:

A no-argument constructor that initializes the title and author to "Unknown", the yearPublished to the current year, and isAvailable to true.

A parameterized constructor that takes title, author, and yearPublished as parameters and initializes the corresponding attributes. The isAvailable attribute should be set to true by default in this constructor.

Additionally, implement borrowBook() that sets isAvailable to false, indicating that the book has been borrowed.

Finally, in your main method, create an object of the Book class using each constructor, display the details of each book object, borrow the book using the borrowBook() method, and then display the details again to show the change in availability.