import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

public class LibraryBookTracker {

    static int validRecords = 0;
    static int searchResults = 0;
    static int booksAdded = 0;
    static int errorCount = 0;

    public static void main(String[] args) {

        try {
            if (args.length < 2)
                throw new InsufficientArgumentsException("Two arguments required.");

            if (!args[0].endsWith(".txt"))
                throw new InvalidFileNameException("File must end with .txt");

            File catalogFile = new File(args[0]);
            createFileIfNotExists(catalogFile);

            File errorLog = new File(catalogFile.getParent(), "errors.log");
            if (errorLog.getParentFile() != null)
                errorLog.getParentFile().mkdirs();
            errorLog.createNewFile();

            List<Book> books = readCatalog(catalogFile, errorLog);

            handleOperation(args[1], books, catalogFile, errorLog);

        } catch (BookCatalogException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO Error: " + e.getMessage());
        } finally {
            printStatistics();
            System.out.println("Thank you for using the Library Book Tracker.");
        }
    }

    private static void createFileIfNotExists(File file) throws IOException {
        if (file.getParentFile() != null)
            file.getParentFile().mkdirs();
        file.createNewFile();
    }

    private static List<Book> readCatalog(File file, File errorLog) throws IOException {
        List<Book> books = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        while ((line = br.readLine()) != null) {
            try {
                Book b = parseBook(line);
                books.add(b);
                validRecords++;
            } catch (BookCatalogException e) {
                logError(errorLog, line, e);
            }
        }
        br.close();
        return books;
    }

    private static Book parseBook(String line) throws BookCatalogException {
        String[] parts = line.split(":");
        if (parts.length != 4)
            throw new MalformedBookEntryException("Incorrect number of fields.");

        String title = parts[0].trim();
        String author = parts[1].trim();
        String isbn = parts[2].trim();
        String copiesStr = parts[3].trim();

        if (title.isEmpty())
            throw new MalformedBookEntryException("Title is empty.");

        if (author.isEmpty())
            throw new MalformedBookEntryException("Author is empty.");

        if (!isbn.matches("\\d{13}"))
            throw new InvalidISBNException("ISBN must be exactly 13 digits.");

        int copies;
        try {
            copies = Integer.parseInt(copiesStr);
            if (copies <= 0)
                throw new MalformedBookEntryException("Copies must be positive.");
        } catch (NumberFormatException e) {
            throw new MalformedBookEntryException("Copies must be integer.");
        }

        return new Book(title, author, isbn, copies);
    }

    private static void handleOperation(String arg, List<Book> books,
                                        File catalogFile, File errorLog)
            throws IOException {

        if (arg.matches("\\d{13}")) {
            // هنا أضفنا try-catch للتعامل مع DuplicateISBNException
            try {
                searchByISBN(arg, books);
            } catch (DuplicateISBNException e) {
                System.out.println("Error: " + e.getMessage());
            }
        } else if (arg.contains(":")) {
            addBook(arg, books, catalogFile, errorLog);
        } else {
            searchByTitle(arg, books);
        }
    }

    private static void searchByTitle(String keyword, List<Book> books) {
        printHeader();
        for (Book b : books) {
            if (b.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
                System.out.println(b);
                searchResults++;
            }
        }
    }

    private static void searchByISBN(String isbn, List<Book> books)
            throws DuplicateISBNException {
        List<Book> found = new ArrayList<>();
        for (Book b : books)
            if (b.getIsbn().equals(isbn))
                found.add(b);

        if (found.size() > 1)
            throw new DuplicateISBNException("Multiple books found with same ISBN.");

        printHeader();
        if (found.size() == 1) {
            System.out.println(found.get(0));
            searchResults = 1;
        }
    }

    private static void addBook(String record, List<Book> books,
                                File catalogFile, File errorLog)
            throws IOException {
        try {
            Book newBook = parseBook(record);
            books.add(newBook);
            booksAdded = 1;

            books.sort(Comparator.comparing(Book::getTitle));

            BufferedWriter bw = new BufferedWriter(new FileWriter(catalogFile));
            for (Book b : books) {
                bw.write(String.format("%s: %s: %s:%d",
                        b.getTitle(), b.getAuthor(),
                        b.getIsbn(), b.getCopies()));
                bw.newLine();
            }
            bw.close();

            printHeader();
            System.out.println(newBook);

        } catch (BookCatalogException e) {
            logError(errorLog, record, e);
        }
    }

    private static void printHeader() {
        System.out.printf("%-30s %-20s %-15s %5s%n",
                "Title", "Author", "ISBN", "Copies");
        System.out.println("---------------------------------------------------------------------");
    }

    private static void logError(File errorLog, String text,
                                 Exception e) throws IOException {
        BufferedWriter bw = new BufferedWriter(
                new FileWriter(errorLog, true));
        bw.write("[" + LocalDateTime.now() + "] \"" +
                text + "\" " + e.getClass().getSimpleName()
                + ": " + e.getMessage());
        bw.newLine();
        bw.close();
        errorCount++;
    }

    private static void printStatistics() {
        System.out.println("\nStatistics:");
        System.out.println("Valid records processed: " + validRecords);
        System.out.println("Search results: " + searchResults);
        System.out.println("Books added: " + booksAdded);
        System.out.println("Errors encountered: " + errorCount);
    }
}