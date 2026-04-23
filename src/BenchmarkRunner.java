import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Standalone benchmark for the {@link Catalogue} class.
 *
 * <p>Measures performance of core operations at three data-set sizes:
 * <ul>
 *   <li><strong>Small</strong> — 100 publications</li>
 *   <li><strong>Medium</strong> — 10 000 publications</li>
 *   <li><strong>Large</strong> — 100 000 publications</li>
 * </ul>
 *
 * <p>Each scenario is warmed up with one dry run (JIT compilation), then
 * timed over {@value #ITERATIONS} iterations.
 *
 * <h2>Run</h2>
 * <pre>{@code
 * javac -d out src/*.java
 * java -cp out BenchmarkRunner
 * }</pre>
 */
public class BenchmarkRunner {

    private static final int ITERATIONS = 10;

    private static final String[] GENRES = {
            "Fiction", "Science", "History", "Programming", "Philosophy",
            "Biography", "Fantasy", "Mystery", "Economics", "Psychology"
    };
    private static final String[] AUTHORS = {
            "George Orwell", "Robert Martin", "Kent Beck", "Martin Fowler",
            "Donald Knuth", "Linus Torvalds", "Brian Kernighan", "Fred Brooks",
            "Edsger Dijkstra", "Alan Turing"
    };

    public static void main(String[] args) throws Exception {
        // Suppress logging noise during benchmarks
        System.setProperty("level", "OFF");
        AppLogger.configure();

        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Book Catalogue — Performance Benchmark     ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        for (int[] cfg : new int[][]{{100, 1}, {10_000, 1}, {100_000, 1}}) {
            int size    = cfg[0];
            runScenario(size);
        }
    }

    private static void runScenario(int datasetSize) throws Exception {
        System.out.printf("%n══ Dataset: %,d books ══%n%n", datasetSize);
        Profiler.reset();

        // ── 1. Bulk add ─────────────────────────────────────────────────────
        Catalogue catalogue = new Catalogue();
        Profiler.start("bulkAdd");
        for (int i = 0; i < datasetSize; i++) {
            catalogue.addPublication(makeBook(i));
        }
        Profiler.stop("bulkAdd");

        // Warm-up JIT
        catalogue.searchByTitle("Book");
        catalogue.getDistinctGenres();

        // ── 2. searchByTitle (repeated) ──────────────────────────────────────
        for (int i = 0; i < ITERATIONS; i++) {
            Profiler.start("searchByTitle");
            catalogue.searchByTitle("Book 5");
            Profiler.stop("searchByTitle");
        }

        // ── 3. searchByAuthor ────────────────────────────────────────────────
        for (int i = 0; i < ITERATIONS; i++) {
            Profiler.start("searchByAuthor");
            catalogue.searchByAuthor("George");
            Profiler.stop("searchByAuthor");
        }

        // ── 4. filterByGenre ─────────────────────────────────────────────────
        for (int i = 0; i < ITERATIONS; i++) {
            Profiler.start("filterByGenre");
            catalogue.filterByGenre("Science");
            Profiler.stop("filterByGenre");
        }

        // ── 5. getDistinctGenres (called after every mutating operation) ─────
        for (int i = 0; i < ITERATIONS; i++) {
            Profiler.start("getDistinctGenres");
            catalogue.getDistinctGenres();
            Profiler.stop("getDistinctGenres");
        }

        // ── 6. getAllPublications ────────────────────────────────────────────
        for (int i = 0; i < ITERATIONS; i++) {
            Profiler.start("getAllPublications");
            catalogue.getAllPublications();
            Profiler.stop("getAllPublications");
        }

        // ── 7. countByGenre ──────────────────────────────────────────────────
        for (int i = 0; i < ITERATIONS; i++) {
            Profiler.start("countByGenre");
            catalogue.countByGenre();
            Profiler.stop("countByGenre");
        }

        // ── 8. save/load ─────────────────────────────────────────────────────
        Path tmp = Files.createTempFile("bench-catalogue-", ".dat");
        try {
            for (int i = 0; i < ITERATIONS; i++) {
                Profiler.start("saveToFile");
                catalogue.saveToFile(tmp.toString());
                Profiler.stop("saveToFile");

                Profiler.start("loadFromFile");
                catalogue.loadFromFile(tmp.toString());
                Profiler.stop("loadFromFile");
            }
        } catch (IOException e) {
            System.err.println("I/O error during benchmark: " + e.getMessage());
        } finally {
            Files.deleteIfExists(tmp);
        }

        Profiler.printSummary();
    }

    /** Creates a deterministic {@link Book} for the given index. */
    private static Book makeBook(int idx) {
        String title  = "Book " + idx + ": " + "A".repeat(Math.min(idx % 20 + 5, 40));
        String author = AUTHORS[idx % AUTHORS.length];
        String genre  = GENRES[idx % GENRES.length];
        int    year   = 1900 + (idx % 124);
        return new Book(title, year, author, "Publisher " + (idx % 50), genre);
    }
}
