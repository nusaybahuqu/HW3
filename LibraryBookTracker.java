import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LibraryBookTracker {

    static final DateTimeFormatter DT_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    static int validCount  = 0;
    static int searchCount = 0;
    static int addedCount  = 0;
    static int errorCount  = 0;

    static PrintWriter errorLog = null;
    
    static List<Book> sharedBooks = new ArrayList<>();

    public static void main(String[] args) {
        try {
            // 1. Argument Validation
            if (args.length < 2)
                throw new InsufficientArgumentsException("Need 2 arguments: <file.txt> <operation>");

            if (!args[0].endsWith(".txt"))
                throw new InvalidFileNameException("Catalog file must end with .txt, got: " + args[0]);

            File catalog = new File(args[0]);
            File logFile = new File(catalog.getAbsoluteFile().getParentFile(), "errors.log");
            errorLog = new PrintWriter(new FileWriter(logFile, true));

            // Thread 1: FileReader 
            // Tasked with reading the catalog file args[0]
            Thread fileThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        sharedBooks = loadCatalog(catalog);
                    } catch (IOException e) {
                        System.out.println("File Error: " + e.getMessage());
                        errorCount++;
                    }
                }
            });

            // Thread 2: OperationAnalyzer
            // Tasked with processing the operation in args[1]
            Thread opThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String op = args[1];
                    try {
                        if (op.matches("\\d{13}")) {
                            // ISBN search if input is exactly 13 digits
                            isbnSearch(sharedBooks, op);
                        } else if (op.chars().filter(c -> c == ':').count() == 3) {
                            // Add new book if input contains 3 colons
                            addBook(sharedBooks, catalog, op);
                        } else {
                            // Keyword search for any other input
                            titleSearch(sharedBooks, op);
                        }
                    } catch (BookCatalogException | IOException e) {
                        System.out.println("Operation ERROR: " + e.getMessage());
                        errorCount++;
                    }
                }
            });

            // Execution Flow using start() and join() as required  
            
            fileThread.start();      // Start Thread 1
            fileThread.join();       // WAIT - Thread 1 finishes completely before proceeding

            opThread.start();        // Start Thread 2 only after Thread 1 finishes
            opThread.join();         // WAIT - Thread 2 finishes completely

        } catch (BookCatalogException | IOException | InterruptedException e) {
            System.out.println("SYSTEM ERROR: " + e.getMessage());
            errorCount++;

        } finally {
            // Print statistics after all threads are done
            System.out.println();
            System.out.println("Valid records processed : " + validCount);
            System.out.println("Search results          : " + searchCount);
            System.out.println("Books added             : " + addedCount);
            System.out.println("Errors encountered      : " + errorCount);

            if (errorLog != null) errorLog.close();
            System.out.println("Thank you for using the Library Book Tracker.");
        }
    }

    // Load and validate all books from the catalog file
    static List<Book> loadCatalog(File file) throws IOException {
        List<Book> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    list.add(parseLine(line));
                    validCount++;
                } catch (BookCatalogException e) {
                    logError(line, e);
                    errorCount++;
                }
            }
        }
        return list;
    }

    // Parse one line into a Book object
    static Book parseLine(String line) throws BookCatalogException {
        String[] p = line.split(":", -1);
        if (p.length != 4) throw new MalformedBookEntryException("Expected 4 fields, got " + p.length);
        
        String title = p[0].trim(), author = p[1].trim(), isbn = p[2].trim(), copiesTxt = p[3].trim();
        if (title.isEmpty() || author.isEmpty()) throw new MalformedBookEntryException("Empty fields");
        if (!isbn.matches("\\d{13}")) throw new InvalidISBNException("ISBN must be 13 digits");

        int copies;
        try {
            copies = Integer.parseInt(copiesTxt);
        } catch (NumberFormatException e) {
            throw new MalformedBookEntryException("Invalid copies number");
        }
        if (copies <= 0) throw new MalformedBookEntryException("Copies must be > 0");

        return new Book(title, author, isbn, copies);
    }

    // Perform title search
    static void titleSearch(List<Book> books, String keyword) {
        printHeader();
        boolean found = false;
        for (Book b : books) {
            if (b.title.toLowerCase().contains(keyword.toLowerCase())) {
                printBook(b);
                searchCount++;
                found = true;
            }
        }
        if (!found) System.out.println("No books found matching: " + keyword);
    }

    // Perform exact ISBN search
    static void isbnSearch(List<Book> books, String isbn) throws DuplicateISBNException {
        List<Book> results = new ArrayList<>();
        for (Book b : books) {
            if (b.isbn.equals(isbn)) results.add(b);
        }
        if (results.size() > 1) {
            DuplicateISBNException ex = new DuplicateISBNException("Duplicate ISBN found: " + isbn);
            logError(isbn, ex);
            errorCount++;
            throw ex;
        }
        printHeader();
        if (results.isEmpty()) System.out.println("No book found with ISBN: " + isbn);
        else { printBook(results.get(0)); searchCount++; }
    }

    // Add a new book and rewrite the catalog file
    static void addBook(List<Book> books, File catalog, String record) throws IOException {
        try {
            Book newBook = parseLine(record);
            books.add(newBook);
            books.sort((a, b) -> a.title.compareToIgnoreCase(b.title));
            try (PrintWriter writer = new PrintWriter(new FileWriter(catalog, false))) {
                for (Book b : books) writer.println(b);
            }
            printHeader();
            printBook(newBook);
            addedCount++;
            System.out.println("Book added successfully.");
        } catch (BookCatalogException e) {
            logError(record, e);
            errorCount++;
            System.out.println("ERROR adding book: " + e.getMessage());
        }
    }

    // Print helper for table header
    static void printHeader() {
        System.out.printf("%-30s %-20s %-15s %5s%n", "Title", "Author", "ISBN", "Copies");
        System.out.println("-".repeat(73));
    }

    // Print helper for single book entry
    static void printBook(Book b) {
        System.out.printf("%-30s %-20s %-15s %5d%n", b.title, b.author, b.isbn, b.copies);
    }

    // Log errors to console and errors.log file
    static void logError(String text, BookCatalogException e) {
        String time = LocalDateTime.now().format(DT_FORMAT);
        String entry = "[" + time + "] INVALID: \"" + text + "\" -> " + e.getMessage();
        System.out.println(entry);
        if (errorLog != null) { errorLog.println(entry); errorLog.flush(); }
    }
}