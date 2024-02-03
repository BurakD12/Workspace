#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct{
    int authorID;
    char name[50];
}Author;

typedef struct{
    int bookID;
    char title[50];
    Author author;
    int yearPublished;
}Book;

typedef struct {
    char name[50];
    Book *books;
    int bookCount;
}Library;

void initialiseLibrary(Library *lib){
    printf("Enter the name of the library: ");
    fgets(lib->name, sizeof(lib->name), stdin);

    printf("Enter the amount of books in this library: ");
    scanf("%d", &lib->bookCount);

    lib->books = (Book *)malloc(lib->bookCount * sizeof(Book));

    for (int i = 0; i < lib->bookCount; i++)
    {
        printf("\nEnter the ID for book-%d: ", i + 1);
        scanf("%d", &lib->books[i].bookID);

        printf("\nEnter the title of this book: ");
        fgets(lib->books[i].title, sizeof(lib->books[i].title), stdin);
        fgets(lib->books[i].title, sizeof(lib->books[i].title), stdin);

        printf("Enter the year this book was published in: ");
        scanf("%d", &lib->books[i].yearPublished);

        printf("\nEnter the name of the author: ");
        fgets(lib->books[i].author.name, sizeof(lib->books[i].author.name), stdin);
        fgets(lib->books[i].author.name, sizeof(lib->books[i].author.name), stdin);

        printf("Enter the ID of this author: ");
        scanf("%d", &lib->books[i].author.authorID);
    }
}

void addBook(Library *lib, Book book){
    Book* newArray = (Book *)realloc(lib->books, (lib->bookCount + 1) * sizeof(Book));
    if (newArray == NULL){
        printf("\n\nError reallocating memory!");
        return;
    }
    lib->books = newArray;
    lib->books[lib->bookCount] = book;
    lib->bookCount++;
}

void printLibraryDetails(Library *lib){
    printf("\n\n----------------\n\n");
    printf("Welcome to %s", lib->name);
    for (int i = 0; i < lib->bookCount; i++){
        printf("\nBook number:%d", lib->books[i].bookID);
        printf("\nBook name: %s", lib->books[i].title);
        printf("\nYear published: %d", lib->books[i].yearPublished);
        printf("\nThe Author: %s", lib->books[i].author.name);
        printf("\nAuthor ID: %d", lib->books[i].author.authorID);
    }
}

void freeLibrary(Library *lib){
    free(lib->books);
    free(lib);
}

