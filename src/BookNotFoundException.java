/**
 * Checked exception thrown when a publication with the requested title
 * cannot be found in the {@link Catalogue}.
 *
 * <p>Callers of {@link Catalogue#findPublicationByTitle(String)} and
 * {@link Catalogue#removePublicationByTitle(String)} must either catch this exception
 * or declare it in their own {@code throws} clause.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * try {
 *     Publication p = catalogue.findPublicationByTitle("Unknown Title");
 * } catch (BookNotFoundException e) {
 *     System.err.println(e.getMessage()); // "Publication with title 'Unknown Title' was not found..."
 * }
 * }</pre>
 *
 * @see Catalogue#findPublicationByTitle(String)
 * @see Catalogue#removePublicationByTitle(String)
 */
public class BookNotFoundException extends Exception {

    /**
     * Constructs a new {@code BookNotFoundException} for the given title.
     *
     * @param title the title that was not found; used to compose the detail message
     */
    public BookNotFoundException(String title) {
        super("Publication with title '" + title + "' was not found in the catalogue.");
    }
}
