import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Core in-memory catalogue that stores and manages a collection of {@link Publication} objects.
 *
 * <p><strong>Architecture note:</strong> {@code Catalogue} acts as the <em>Model</em> layer of
 * the application. It holds no UI state and knows nothing about Swing. All mutating methods
 * operate directly on the internal {@link ArrayList}; the UI layer ({@link BookGUI}) calls
 * these methods and then refreshes the table from the returned lists.
 *
 * <p><strong>Persistence:</strong> The catalogue is serialised as a whole to a binary file via
 * Java's built-in {@link ObjectOutputStream}. Because the stored graph consists only of
 * {@link Publication} subclasses that implement {@link java.io.Serializable}, no external
 * libraries are required.
 *
 * <h2>Typical workflow</h2>
 * <pre>{@code
 * Catalogue cat = new Catalogue();
 * cat.addPublication(new Book("Dune", 1965, "Frank Herbert", "Chilton", "Sci-Fi"));
 * List<Publication> results = cat.searchByTitle("dune");
 * cat.saveToFile("my_catalogue.dat");
 * }</pre>
 *
 * @see Publication
 * @see Book
 * @see BookGUI
 */
public class Catalogue {

    /** Internal storage for all publications. Never {@code null}. */
    private ArrayList<Publication> publications = new ArrayList<>();

    /**
     * Constructs a new empty {@code Catalogue}.
     */
    public Catalogue() {}

    /**
     * Adds a publication to the catalogue.
     *
     * @param p the publication to add; must not be {@code null}
     */
    public void addPublication(Publication p) {
        publications.add(p);
    }

    /**
     * Removes the publication whose title exactly matches (case-insensitive) the given string.
     *
     * @param title the title to search for; must not be {@code null}
     * @throws BookNotFoundException if no publication with that title exists
     */
    public void removePublicationByTitle(String title) throws BookNotFoundException {
        Publication found = findPublicationByTitle(title);
        publications.remove(found);
    }

    /**
     * Finds and returns the first publication whose title exactly matches
     * the given string (case-insensitive comparison).
     *
     * @param title the exact title to look for; must not be {@code null}
     * @return the matching {@link Publication}; never {@code null}
     * @throws BookNotFoundException if no matching publication is found
     */
    public Publication findPublicationByTitle(String title) throws BookNotFoundException {
        for (Publication p : publications) {
            if (p.getTitle().equalsIgnoreCase(title)) {
                return p;
            }
        }
        throw new BookNotFoundException(title);
    }

    /**
     * Returns a defensive copy of all publications currently in the catalogue.
     *
     * <p>Modifications to the returned list do not affect the catalogue.
     *
     * @return a new {@link List} containing all publications; never {@code null}
     */
    public List<Publication> getAllPublications() {
        return new ArrayList<>(publications);
    }

    /**
     * Returns all publications whose title contains the given query string
     * (case-insensitive substring match).
     *
     * @param query the search string; must not be {@code null}
     * @return a list of matching publications; empty if none found
     */
    public List<Publication> searchByTitle(String query) {
        List<Publication> result = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (Publication p : publications) {
            if (p.getTitle().toLowerCase().contains(lowerQuery)) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Returns all {@link Book} entries whose author field contains the given query string
     * (case-insensitive substring match).
     *
     * <p>Non-{@code Book} publications are ignored because they do not have an author field.
     *
     * @param query the search string; must not be {@code null}
     * @return a list of matching books; empty if none found
     */
    public List<Publication> searchByAuthor(String query) {
        List<Publication> result = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (Publication p : publications) {
            if (p instanceof Book b && b.getAuthor().toLowerCase().contains(lowerQuery)) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Returns all {@link Book} entries that belong to the specified genre
     * (case-insensitive exact match).
     *
     * @param genre the genre to filter by; must not be {@code null}
     * @return a list of books in that genre; empty if none found
     */
    public List<Publication> filterByGenre(String genre) {
        List<Publication> result = new ArrayList<>();
        for (Publication p : publications) {
            if (p instanceof Book b && b.getGenre().equalsIgnoreCase(genre)) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Counts the number of distinct author names among all {@link Book} entries.
     *
     * @return the count of unique author strings; {@code 0} if there are no books
     */
    public long countUniqueAuthors() {
        return publications.stream()
                .filter(p -> p instanceof Book)
                .map(p -> ((Book) p).getAuthor())
                .distinct()
                .count();
    }

    /**
     * Returns a map from genre name to the count of books in that genre.
     *
     * <p>The map preserves insertion order (genres appear in the same order
     * as returned by {@link #getDistinctGenres()}).
     *
     * @return a {@link java.util.Map} where keys are genre names and values are counts;
     *         never {@code null}
     */
    public java.util.Map<String, Long> countByGenre() {
        java.util.Map<String, Long> map = new java.util.LinkedHashMap<>();
        for (String genre : getDistinctGenres()) {
            map.put(genre, publications.stream()
                    .filter(p -> p instanceof Book b && b.getGenre().equalsIgnoreCase(genre))
                    .count());
        }
        return map;
    }

    /**
     * Returns a sorted list of all distinct genre values present in the catalogue.
     *
     * <p>Blank or {@code null} genre values are excluded. The result is sorted
     * alphabetically for consistent display in UI components.
     *
     * @return a sorted, deduplicated list of genre strings; never {@code null}
     */
    public List<String> getDistinctGenres() {
        return publications.stream()
                .filter(p -> p instanceof Book)
                .map(p -> ((Book) p).getGenre())
                .filter(g -> g != null && !g.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Exports all {@link Book} entries to a CSV file with a header row.
     *
     * <p>Each field is quoted to handle commas within values.
     * The output format is:
     * <pre>Title,Author,Publisher,Year,Genre</pre>
     *
     * <p>Non-{@code Book} publications are silently skipped.
     *
     * @param filename the path of the CSV file to create or overwrite
     * @throws IOException if an I/O error occurs while writing the file
     */
    public void exportToCSV(String filename) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("Title,Author,Publisher,Year,Genre");
            for (Publication p : publications) {
                if (p instanceof Book b) {
                    pw.printf("\"%s\",\"%s\",\"%s\",%d,\"%s\"%n",
                            b.getTitle(), b.getAuthor(), b.getPublisher(), b.getYear(), b.getGenre());
                }
            }
        }
    }

    /**
     * Serialises the current publication list to a binary file using Java object serialisation.
     *
     * <p>The file can later be restored with {@link #loadFromFile(String)}.
     *
     * @param filename the path of the file to write
     * @throws IOException if the file cannot be created or written
     */
    public void saveToFile(String filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(publications);
        }
    }

    /**
     * Deserialises the publication list from a previously saved binary file.
     *
     * <p>The existing in-memory catalogue is replaced entirely by the loaded data.
     *
     * @param filename the path of the file to read
     * @throws IOException            if the file cannot be read
     * @throws ClassNotFoundException if the serialised classes are not on the classpath
     */
    @SuppressWarnings("unchecked")
    public void loadFromFile(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            publications = (ArrayList<Publication>) ois.readObject();
        }
    }
}
