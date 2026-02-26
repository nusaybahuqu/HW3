
class BookCatalogException extends Exception {
    public BookCatalogException(String message) { super(message); }
}


class InvalidISBNException extends BookCatalogException {
    public InvalidISBNException(String message) { super(message); }
}

class DuplicateISBNException extends BookCatalogException {
    public DuplicateISBNException(String message) { super(message); }
}

class MalformedBookEntryException extends BookCatalogException {
    public MalformedBookEntryException(String message) { super(message); }
}

class InsufficientArgumentsException extends BookCatalogException {
    public InsufficientArgumentsException(String message) { super(message); }
}

class InvalidFileNameException extends BookCatalogException {
    public InvalidFileNameException(String message) { super(message); }
}
