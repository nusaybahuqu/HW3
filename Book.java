public class Book {
    String title, author, isbn;
    int copies;

    public Book(String title, String author, String isbn, int copies) {
        this.title  = title;
        this.author = author;
        this.isbn   = isbn;
        this.copies = copies;
    }

    // Used when writing back to the file
    @Override
    public String toString() {
        return title + ":" + author + ":" + isbn + ":" + copies;
    }
}
