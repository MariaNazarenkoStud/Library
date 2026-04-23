import java.io.Serializable;

/**
 * Abstract base class representing a general publication in the catalogue.
 *
 * <p>A {@code Publication} stores the minimal shared data for any catalogue entry:
 * a title and a publication year. The class implements {@link Serializable} so that
 * instances can be persisted to binary files via {@link Catalogue#saveToFile(String)}.
 *
 * <p>Concrete subclasses (e.g. {@link Book}) extend this class with type-specific fields.
 *
 * @see Book
 * @see Catalogue
 */
public class Publication implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private int year;

    /**
     * Constructs a new {@code Publication} with the given title and year.
     *
     * @param title the title of the publication; must not be {@code null}
     * @param year  the four-digit publication year (e.g. {@code 2024})
     */
    public Publication(String title, int year) {
        this.title = title;
        this.year = year;
    }

    /**
     * Returns the title of this publication.
     *
     * @return the title; never {@code null}
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets a new title for this publication.
     *
     * @param title the new title; must not be {@code null}
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the publication year.
     *
     * @return a four-digit year value
     */
    public int getYear() {
        return year;
    }

    /**
     * Sets the publication year.
     *
     * @param year the new four-digit year value
     */
    public void setYear(int year) {
        this.year = year;
    }

    /**
     * Returns a human-readable string representation of this publication.
     *
     * @return a string in the format {@code Publication{title='...', year=...}}
     */
    @Override
    public String toString() {
        return "Publication{title='" + title + "', year=" + year + "}";
    }
}
