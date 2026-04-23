/**
 * Represents a book entry in the catalogue.
 *
 * <p>A {@code Book} extends {@link Publication} with three additional
 * bibliographic fields: author, publisher and genre.
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 * Book book = new Book("Clean Code", 2008,
 *                      "Robert C. Martin", "Prentice Hall", "Programming");
 * catalogue.addPublication(book);
 * }</pre>
 *
 * @see Publication
 * @see Catalogue
 */
public class Book extends Publication {

    private String author;
    private String publisher;
    private String genre;

    /**
     * Constructs a new {@code Book} with the specified bibliographic data.
     *
     * @param title     the book title; must not be {@code null}
     * @param year      the four-digit publication year
     * @param author    the primary author's full name; must not be {@code null}
     * @param publisher the name of the publishing house
     * @param genre     the literary or thematic genre (e.g. {@code "Fiction"}, {@code "Science"})
     */
    public Book(String title, int year, String author, String publisher, String genre) {
        super(title, year);
        this.author = author;
        this.publisher = publisher;
        this.genre = genre;
    }

    /**
     * Returns the author of this book.
     *
     * @return the author's full name; never {@code null}
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Sets the author of this book.
     *
     * @param author the new author name; must not be {@code null}
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Returns the publisher of this book.
     *
     * @return the publisher name
     */
    public String getPublisher() {
        return publisher;
    }

    /**
     * Sets the publisher of this book.
     *
     * @param publisher the new publisher name
     */
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    /**
     * Returns the genre of this book.
     *
     * @return the genre string
     */
    public String getGenre() {
        return genre;
    }

    /**
     * Sets the genre of this book.
     *
     * @param genre the new genre string
     */
    public void setGenre(String genre) {
        this.genre = genre;
    }

    /**
     * Returns a detailed string representation of this book.
     *
     * @return a string in the format
     *         {@code Book{title='...', year=..., author='...', publisher='...', genre='...'}}
     */
    @Override
    public String toString() {
        return "Book{title='" + getTitle() + "', year=" + getYear()
                + ", author='" + author + "', publisher='" + publisher
                + "', genre='" + genre + "'}";
    }
}
