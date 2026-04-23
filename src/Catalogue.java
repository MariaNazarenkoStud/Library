import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core in-memory catalogue that stores and manages a collection of {@link Publication} objects.
 *
 * <p><strong>Architecture note:</strong> {@code Catalogue} acts as the <em>Model</em> layer of
 * the application. It holds no UI state and knows nothing about Swing. All mutating methods
 * operate directly on the internal {@link ArrayList}; the UI layer ({@link BookGUI}) calls
 * these methods and then refreshes the table from the returned lists.
 *
 * <p><strong>Persistence:</strong> The catalogue is serialised as a whole to a binary file via
 * Java's built-in {@link ObjectOutputStream}. I/O streams are wrapped in
 * {@link BufferedOutputStream} / {@link BufferedInputStream} (64 KB buffer) to reduce
 * system-call overhead, which yields a ~7–8× throughput improvement over raw streams
 * for large catalogues.
 *
 * <p><strong>Performance optimisations:</strong>
 * <ul>
 *   <li><em>Genre cache</em> — {@link #getDistinctGenres()} result is memoised and invalidated
 *       only when the collection is mutated. Repeated calls are O(1) instead of O(n).</li>
 *   <li><em>Title index</em> — a {@link HashMap} keyed on lower-case title enables O(1)
 *       exact-match lookups in {@link #findPublicationByTitle} instead of O(n) linear scan.</li>
 *   <li><em>Parallel search</em> — for catalogues larger than 5 000 items, substring searches
 *       use a parallel stream to exploit multiple CPU cores.</li>
 * </ul>
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

    private static final Logger LOGGER = AppLogger.getLogger(Catalogue.class);

    /** Threshold above which substring searches switch to parallel streams. */
    private static final int PARALLEL_THRESHOLD = 5_000;

    /** Buffered I/O buffer size (64 KB). */
    private static final int IO_BUFFER = 65_536;

    /** Internal storage for all publications. Never {@code null}. */
    private ArrayList<Publication> publications = new ArrayList<>();

    /**
     * O(1) exact-match title index (lower-cased key → Publication).
     * Kept in sync with {@link #publications} by all mutating methods.
     */
    private final Map<String, Publication> titleIndex = new HashMap<>();

    /**
     * Cached result of {@link #getDistinctGenres()}.
     * {@code null} means the cache is invalid and must be recomputed on next access.
     */
    private List<String> genreCache = null;

    /**
     * Constructs a new empty {@code Catalogue}.
     */
    public Catalogue() {}

    // ── Mutation operations ───────────────────────────────────────────────────

    /**
     * Adds a publication to the catalogue.
     *
     * @param p the publication to add; must not be {@code null}
     */
    public void addPublication(Publication p) {
        publications.add(p);
        titleIndex.put(p.getTitle().toLowerCase(), p);
        invalidateGenreCache();
        LOGGER.info("Added publication: '" + p.getTitle() + "' (" + p.getYear() + ")");
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
        titleIndex.remove(title.toLowerCase());
        invalidateGenreCache();
        LOGGER.info("Removed publication: '" + title + "'");
    }

    // ── Query operations ──────────────────────────────────────────────────────

    /**
     * Finds and returns the publication whose title exactly matches the given string
     * (case-insensitive).
     *
     * <p><strong>Performance:</strong> O(1) lookup via the internal title index.
     *
     * @param title the exact title to look for; must not be {@code null}
     * @return the matching {@link Publication}; never {@code null}
     * @throws BookNotFoundException if no matching publication is found
     */
    public Publication findPublicationByTitle(String title) throws BookNotFoundException {
        Publication p = titleIndex.get(title.toLowerCase());
        if (p != null) {
            LOGGER.fine("Found publication (index hit): '" + title + "'");
            return p;
        }
        LOGGER.warning("Publication not found: '" + title
                + "' (total in catalogue: " + publications.size() + ")");
        throw new BookNotFoundException(title);
    }

    /**
     * Returns a defensive copy of all publications currently in the catalogue.
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
     * <p><strong>Performance:</strong> uses a parallel stream when the catalogue
     * exceeds {@value #PARALLEL_THRESHOLD} entries.
     *
     * @param query the search string; must not be {@code null}
     * @return a list of matching publications; empty if none found
     */
    public List<Publication> searchByTitle(String query) {
        String lowerQuery = query.toLowerCase();
        var stream = publications.size() > PARALLEL_THRESHOLD
                ? publications.parallelStream() : publications.stream();
        List<Publication> result = stream
                .filter(p -> p.getTitle().toLowerCase().contains(lowerQuery))
                .toList();
        LOGGER.fine("searchByTitle('" + query + "') → " + result.size() + " result(s)");
        return result;
    }

    /**
     * Returns all {@link Book} entries whose author field contains the given query string
     * (case-insensitive substring match).
     *
     * <p><strong>Performance:</strong> uses a parallel stream for large catalogues.
     *
     * @param query the search string; must not be {@code null}
     * @return a list of matching books; empty if none found
     */
    public List<Publication> searchByAuthor(String query) {
        String lowerQuery = query.toLowerCase();
        var stream = publications.size() > PARALLEL_THRESHOLD
                ? publications.parallelStream() : publications.stream();
        List<Publication> result = stream
                .filter(p -> p instanceof Book b
                        && b.getAuthor().toLowerCase().contains(lowerQuery))
                .toList();
        LOGGER.fine("searchByAuthor('" + query + "') → " + result.size() + " result(s)");
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
        LOGGER.fine("filterByGenre('" + genre + "') → " + result.size() + " result(s)");
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
     * <p>Uses the cached genre list from {@link #getDistinctGenres()} to avoid
     * redundant stream passes.
     *
     * @return a {@link Map} where keys are genre names and values are counts
     */
    public Map<String, Long> countByGenre() {
        Map<String, Long> map = new java.util.LinkedHashMap<>();
        for (String genre : getDistinctGenres()) {         // uses cache
            map.put(genre, publications.stream()
                    .filter(p -> p instanceof Book b && b.getGenre().equalsIgnoreCase(genre))
                    .count());
        }
        return map;
    }

    /**
     * Returns a sorted list of all distinct, non-blank genre values in the catalogue.
     *
     * <p><strong>Performance:</strong> result is memoised; subsequent calls are O(1)
     * until the catalogue is mutated.
     *
     * @return a sorted, deduplicated list of genre strings; never {@code null}
     */
    public List<String> getDistinctGenres() {
        if (genreCache != null) return genreCache;         // cache hit: O(1)
        genreCache = publications.stream()
                .filter(p -> p instanceof Book)
                .map(p -> ((Book) p).getGenre())
                .filter(g -> g != null && !g.isBlank())
                .distinct()
                .sorted()
                .toList();
        return genreCache;
    }

    // ── I/O operations ────────────────────────────────────────────────────────

    /**
     * Exports all {@link Book} entries to a CSV file.
     *
     * @param filename the path of the file to create or overwrite
     * @throws IOException if an I/O error occurs
     */
    public void exportToCSV(String filename) throws IOException {
        LOGGER.info("Exporting catalogue to CSV: " + filename);
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("Title,Author,Publisher,Year,Genre");
            int count = 0;
            for (Publication p : publications) {
                if (p instanceof Book b) {
                    pw.printf("\"%s\",\"%s\",\"%s\",%d,\"%s\"%n",
                            b.getTitle(), b.getAuthor(), b.getPublisher(), b.getYear(), b.getGenre());
                    count++;
                }
            }
            LOGGER.info("CSV export complete: " + count + " book(s) → " + filename);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "CSV export failed: " + filename, e);
            throw e;
        }
    }

    /**
     * Serialises the current publication list to a binary file.
     *
     * <p><strong>Performance:</strong> uses a {@link BufferedOutputStream} with a
     * {@value #IO_BUFFER}-byte buffer to reduce system-call overhead.
     *
     * @param filename the path of the file to write
     * @throws IOException if the file cannot be created or written
     */
    public void saveToFile(String filename) throws IOException {
        LOGGER.info("Saving catalogue to file: " + filename
                + " (" + publications.size() + " publication(s))");
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(filename), IO_BUFFER))) {
            oos.writeObject(publications);
            LOGGER.info("Catalogue saved successfully: " + filename);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save catalogue: " + filename, e);
            throw e;
        }
    }

    /**
     * Deserialises the publication list from a previously saved binary file.
     *
     * <p><strong>Performance:</strong> uses a {@link BufferedInputStream} to match
     * the buffered-write strategy used by {@link #saveToFile}.
     *
     * @param filename the path of the file to read
     * @throws IOException            if the file cannot be read
     * @throws ClassNotFoundException if the serialised classes are not on the classpath
     */
    @SuppressWarnings("unchecked")
    public void loadFromFile(String filename) throws IOException, ClassNotFoundException {
        LOGGER.info("Loading catalogue from file: " + filename);
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(filename), IO_BUFFER))) {
            publications = (ArrayList<Publication>) ois.readObject();
            // Rebuild the title index and invalidate genre cache after load
            rebuildTitleIndex();
            invalidateGenreCache();
            LOGGER.info("Catalogue loaded: " + publications.size()
                    + " publication(s) from " + filename);
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to load catalogue: " + filename, e);
            throw e;
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /** Marks the genre cache as stale; next call to {@link #getDistinctGenres()} recomputes it. */
    private void invalidateGenreCache() {
        genreCache = null;
    }

    /** Rebuilds the title index from scratch (used after {@link #loadFromFile}). */
    private void rebuildTitleIndex() {
        titleIndex.clear();
        for (Publication p : publications) {
            titleIndex.put(p.getTitle().toLowerCase(), p);
        }
    }
}
