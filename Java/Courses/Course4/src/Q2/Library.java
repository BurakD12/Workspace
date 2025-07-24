package Q2;

public class Library {
    private Book[] books;
    private int size;
    private int capacity;

    Library() {
        books = new Book[2];
        size = 0;
        capacity = 2;
    }

    public void addBook(Book book) {
        for (int i = 0; i < size; i++) {
            if (books[i].getTitle() == book.getTitle()) {
                return; // Book already exists, do not add it again
            }
        }

        if (size == capacity) {
            capacity *= 2;
            Book[] newBooks = new Book[capacity];

            for (int i = 0; i < size; i++) {
                newBooks[i] = books[i];
            }

            books = newBooks;
        }

        books[size] = book;
        size++;
    }

    public boolean removeBook(String title) {
        int originalSize = size;

        for (int i = 0; i < size; i++) {
            if (books[i].getTitle() == title) {
                for (int j = i; j < size; j++) {
                    books[j] = books[j + 1];
                }
                books[size - 1] = null;
                size--;

            }
        }
        if (size < capacity / 3) {
            capacity /= 2;
            Book[] newBooks = new Book[capacity];
            for (int i = 0; i < newBooks.length; i++) {
                newBooks[i] = books[i];
            }
            books = newBooks;

        }
        if (size != originalSize) {
            return true;
        } else {
            return false;
        }
    }

    public boolean updateBook(String title, Book newBook){
        for (int i = 0; i < size; i++) {
            if (newBook.getTitle() == books[i].getTitle()){
                return false;
            }
        }
        
        for (int i = 0; i < size; i++) {
            if (title == books[i].getTitle()){
                books[i] = newBook;
                return true;
            }
        }
        return false;
    }

    public Book getBook(String title){
        for (int i = 0; i < size; i++) {
            if (title == books[i].getTitle()){
                return books[i];
            }
        }
        return null;
    }

    public void displayAllBooks(){
        for (Book book : books) {
            book.displayBookDetails();
        }
    }

    private void resize(int newCapacity){
        if (newCapacity >= size){
            Book[] newBooks = new Book[newCapacity];
            for (int i = 0; i < size; i++) {
                newBooks[i] = books[i];
            }
            books = newBooks;
        }
    }

    public Book[] getBooks() {
        return books;
    }

    public void setBooks(Book[] books) {
        this.books = books;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    

}
