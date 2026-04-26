import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Catalogue}.
 *
 * <p>Each test method is isolated — a fresh {@code Catalogue} is created in
 * {@link #setUp()} so no state leaks between tests.
 *
 * <h2>Tested functions</h2>
 * <ol>
 *   <li>{@link Catalogue#addPublication} / {@link Catalogue#getAllPublications}</li>
 *   <li>{@link Catalogue#removePublicationByTitle}</li>
 *   <li>{@link Catalogue#searchByTitle} and {@link Catalogue#searchByAuthor}</li>
 *   <li>{@link Catalogue#findPublicationByTitle}</li>
 *   <li>{@link Catalogue#getDistinctGenres} (caching)</li>
 *   <li>{@link Catalogue#saveToFile} / {@link Catalogue#loadFromFile} (persistence round-trip)</li>
 * </ol>
 */
@DisplayName("Catalogue unit tests")
class CatalogueTest {

    private Catalogue catalogue;

    @BeforeEach
    void setUp() {
        System.setProperty("level", "OFF");
        AppLogger.configure();
        catalogue = new Catalogue();
    }

    // =========================================================================
    // 1. addPublication / getAllPublications
    // =========================================================================

    @Nested
    @DisplayName("1. addPublication")
    class AddPublicationTests {

        @Test
        @DisplayName("TC-01: adding a valid book increases catalogue size by 1")
        void addValidBook_sizeIncreases() {
            catalogue.addPublication(book("Clean Code", "Robert Martin", "Programming"));
            assertEquals(1, catalogue.getAllPublications().size());
        }

        @Test
        @DisplayName("TC-02: added book appears in getAllPublications")
        void addValidBook_appearsInList() {
            Book b = book("Effective Java", "Joshua Bloch", "Programming");
            catalogue.addPublication(b);
            assertTrue(catalogue.getAllPublications().contains(b));
        }

        @Test
        @DisplayName("TC-03: adding multiple books — all are stored")
        void addMultipleBooks_allStored() {
            catalogue.addPublication(book("Book A", "Author A", "Fiction"));
            catalogue.addPublication(book("Book B", "Author B", "Science"));
            catalogue.addPublication(book("Book C", "Author C", "History"));
            assertEquals(3, catalogue.getAllPublications().size());
        }

        @Test
        @DisplayName("TC-04: adding a book invalidates the genre cache")
        void addBook_invalidatesGenreCache() {
            catalogue.addPublication(book("X", "Y", "Fiction"));
            List<String> before = catalogue.getDistinctGenres();
            catalogue.addPublication(book("Z", "W", "Science"));
            List<String> after = catalogue.getDistinctGenres();
            assertNotEquals(before.size(), after.size(),
                    "Genre cache must be invalidated after addPublication");
        }

        @Test
        @DisplayName("TC-05: getAllPublications returns a defensive copy")
        void getAllPublications_returnsDefensiveCopy() {
            catalogue.addPublication(book("A", "B", "C"));
            List<Publication> copy = catalogue.getAllPublications();
            copy.clear();
            assertEquals(1, catalogue.getAllPublications().size(),
                    "Clearing the returned list must not affect the catalogue");
        }
    }

    // =========================================================================
    // 2. removePublicationByTitle
    // =========================================================================

    @Nested
    @DisplayName("2. removePublicationByTitle")
    class RemoveTests {

        @Test
        @DisplayName("TC-06: removing existing book decreases size by 1")
        void removeExisting_sizeDecreases() throws BookNotFoundException {
            catalogue.addPublication(book("Dune", "Herbert", "Sci-Fi"));
            catalogue.removePublicationByTitle("Dune");
            assertEquals(0, catalogue.getAllPublications().size());
        }

        @Test
        @DisplayName("TC-07: removed book is no longer in the list")
        void removeExisting_bookGone() throws BookNotFoundException {
            Book b = book("1984", "Orwell", "Dystopia");
            catalogue.addPublication(b);
            catalogue.removePublicationByTitle("1984");
            assertFalse(catalogue.getAllPublications().contains(b));
        }

        @Test
        @DisplayName("TC-08: removing non-existent book throws BookNotFoundException")
        void removeNonExistent_throwsException() {
            assertThrows(BookNotFoundException.class,
                    () -> catalogue.removePublicationByTitle("Ghost Book"));
        }

        @Test
        @DisplayName("TC-09: remove is case-insensitive — lowercase title still removes the book")
        void removeCaseInsensitive_succeeds() throws BookNotFoundException {
            catalogue.addPublication(book("Brave New World", "Huxley", "Fiction"));
            assertDoesNotThrow(() -> catalogue.removePublicationByTitle("brave new world"),
                    "Title index is case-insensitive — lowercase should find and remove the book");
            assertEquals(0, catalogue.getAllPublications().size());
        }

        @Test
        @DisplayName("TC-10: removing one of many books leaves the rest intact")
        void removeOne_othersRemain() throws BookNotFoundException {
            catalogue.addPublication(book("A", "X", "G1"));
            catalogue.addPublication(book("B", "Y", "G2"));
            catalogue.addPublication(book("C", "Z", "G3"));
            catalogue.removePublicationByTitle("B");
            List<Publication> remaining = catalogue.getAllPublications();
            assertEquals(2, remaining.size());
            assertTrue(remaining.stream().noneMatch(p -> p.getTitle().equals("B")));
        }
    }

    // =========================================================================
    // 3. searchByTitle / searchByAuthor
    // =========================================================================

    @Nested
    @DisplayName("3. searchByTitle and searchByAuthor")
    class SearchTests {

        @BeforeEach
        void populate() {
            catalogue.addPublication(book("Clean Code", "Robert Martin", "Programming"));
            catalogue.addPublication(book("Clean Architecture", "Robert Martin", "Programming"));
            catalogue.addPublication(book("The Pragmatic Programmer", "Andrew Hunt", "Programming"));
            catalogue.addPublication(book("Design Patterns", "Gang of Four", "Software"));
        }

        @Test
        @DisplayName("TC-11: searchByTitle with exact match returns that book")
        void searchByTitle_exactMatch() {
            List<Publication> r = catalogue.searchByTitle("Clean Code");
            assertEquals(1, r.size());
            assertEquals("Clean Code", r.get(0).getTitle());
        }

        @Test
        @DisplayName("TC-12: searchByTitle with partial query returns all matches")
        void searchByTitle_partialMatch() {
            List<Publication> r = catalogue.searchByTitle("Clean");
            assertEquals(2, r.size());
        }

        @Test
        @DisplayName("TC-13: searchByTitle is case-insensitive")
        void searchByTitle_caseInsensitive() {
            List<Publication> r = catalogue.searchByTitle("clean code");
            assertEquals(1, r.size());
        }

        @Test
        @DisplayName("TC-14: searchByTitle with no match returns empty list")
        void searchByTitle_noMatch() {
            List<Publication> r = catalogue.searchByTitle("Nonexistent Title XYZ");
            assertTrue(r.isEmpty());
        }

        @Test
        @DisplayName("TC-15: searchByAuthor finds books by partial author name")
        void searchByAuthor_partialMatch() {
            List<Publication> r = catalogue.searchByAuthor("Robert");
            assertEquals(2, r.size());
        }

        @Test
        @DisplayName("TC-16: searchByAuthor is case-insensitive")
        void searchByAuthor_caseInsensitive() {
            List<Publication> r = catalogue.searchByAuthor("robert martin");
            assertEquals(2, r.size());
        }

        @Test
        @DisplayName("TC-17: searchByAuthor with no match returns empty list")
        void searchByAuthor_noMatch() {
            assertTrue(catalogue.searchByAuthor("Unknown Author").isEmpty());
        }

        @ParameterizedTest(name = "TC-18: empty catalogue search with query \"{0}\" returns empty")
        @ValueSource(strings = {"anything", "   ", "123"})
        @DisplayName("TC-18: searching empty catalogue always returns empty list")
        void searchEmptyCatalogue(String query) {
            Catalogue empty = new Catalogue();
            assertTrue(empty.searchByTitle(query).isEmpty());
            assertTrue(empty.searchByAuthor(query).isEmpty());
        }
    }

    // =========================================================================
    // 4. findPublicationByTitle (O(1) index lookup)
    // =========================================================================

    @Nested
    @DisplayName("4. findPublicationByTitle")
    class FindTests {

        @Test
        @DisplayName("TC-19: find returns the correct publication")
        void findExisting_returnsCorrectBook() throws BookNotFoundException {
            Book expected = book("Domain Driven Design", "Evans", "Architecture");
            catalogue.addPublication(expected);
            Publication found = catalogue.findPublicationByTitle("Domain Driven Design");
            assertSame(expected, found);
        }

        @Test
        @DisplayName("TC-20: find is case-insensitive")
        void findCaseInsensitive() throws BookNotFoundException {
            catalogue.addPublication(book("Refactoring", "Fowler", "Programming"));
            assertDoesNotThrow(() -> catalogue.findPublicationByTitle("refactoring"));
        }

        @Test
        @DisplayName("TC-21: find on missing title throws BookNotFoundException")
        void findMissing_throwsException() {
            assertThrows(BookNotFoundException.class,
                    () -> catalogue.findPublicationByTitle("Does Not Exist"));
        }

        @Test
        @DisplayName("TC-22: after remove, find throws for removed book")
        void afterRemove_findThrows() throws BookNotFoundException {
            catalogue.addPublication(book("Temp Book", "Author", "Genre"));
            catalogue.removePublicationByTitle("Temp Book");
            assertThrows(BookNotFoundException.class,
                    () -> catalogue.findPublicationByTitle("Temp Book"));
        }
    }

    // =========================================================================
    // 5. getDistinctGenres (caching behaviour)
    // =========================================================================

    @Nested
    @DisplayName("5. getDistinctGenres (caching)")
    class GenreTests {

        @Test
        @DisplayName("TC-23: returns empty list for empty catalogue")
        void emptyGenres_whenEmpty() {
            assertTrue(catalogue.getDistinctGenres().isEmpty());
        }

        @Test
        @DisplayName("TC-24: genres are sorted alphabetically")
        void genres_areSorted() {
            catalogue.addPublication(book("B", "X", "Science"));
            catalogue.addPublication(book("A", "Y", "Fiction"));
            catalogue.addPublication(book("C", "Z", "History"));
            List<String> genres = catalogue.getDistinctGenres();
            List<String> sorted = List.copyOf(genres).stream().sorted().toList();
            assertEquals(sorted, genres);
        }

        @Test
        @DisplayName("TC-25: duplicate genres appear only once")
        void genres_noDuplicates() {
            catalogue.addPublication(book("Book1", "A1", "Fiction"));
            catalogue.addPublication(book("Book2", "A2", "Fiction"));
            catalogue.addPublication(book("Book3", "A3", "Science"));
            List<String> genres = catalogue.getDistinctGenres();
            assertEquals(2, genres.size());
        }

        @Test
        @DisplayName("TC-26: second call returns same object reference (cache hit)")
        void genres_cacheHit() {
            catalogue.addPublication(book("X", "Y", "Drama"));
            List<String> first  = catalogue.getDistinctGenres();
            List<String> second = catalogue.getDistinctGenres();
            assertSame(first, second, "Must return the same cached object on second call");
        }

        @Test
        @DisplayName("TC-27: adding a book invalidates genre cache")
        void addBook_genreCacheInvalidated() {
            catalogue.addPublication(book("A", "B", "Fiction"));
            List<String> beforeRef = catalogue.getDistinctGenres();
            catalogue.addPublication(book("C", "D", "Science"));
            List<String> afterRef = catalogue.getDistinctGenres();
            assertNotSame(beforeRef, afterRef, "Cache must be a new object after mutation");
        }
    }

    // =========================================================================
    // 6. saveToFile / loadFromFile (persistence round-trip)
    // =========================================================================

    @Nested
    @DisplayName("6. saveToFile / loadFromFile")
    class PersistenceTests {

        @Test
        @DisplayName("TC-28: saved catalogue can be loaded back with same content")
        void saveAndLoad_sameContent() throws IOException, ClassNotFoundException {
            catalogue.addPublication(book("Book A", "Author A", "Genre A"));
            catalogue.addPublication(book("Book B", "Author B", "Genre B"));

            Path tmp = Files.createTempFile("cat-test-", ".dat");
            try {
                catalogue.saveToFile(tmp.toString());

                Catalogue loaded = new Catalogue();
                loaded.loadFromFile(tmp.toString());

                assertEquals(2, loaded.getAllPublications().size());
                assertEquals("Book A", loaded.getAllPublications().get(0).getTitle());
                assertEquals("Book B", loaded.getAllPublications().get(1).getTitle());
            } finally {
                Files.deleteIfExists(tmp);
            }
        }

        @Test
        @DisplayName("TC-29: loading rebuilds the title index (findPublicationByTitle works)")
        void loadRebuildsIndex() throws Exception {
            catalogue.addPublication(book("Index Book", "Author", "Genre"));
            Path tmp = Files.createTempFile("cat-idx-", ".dat");
            try {
                catalogue.saveToFile(tmp.toString());
                Catalogue loaded = new Catalogue();
                loaded.loadFromFile(tmp.toString());
                assertDoesNotThrow(() -> loaded.findPublicationByTitle("Index Book"),
                        "Title index must be rebuilt after load");
            } finally {
                Files.deleteIfExists(tmp);
            }
        }

        @Test
        @DisplayName("TC-30: loading replaces existing catalogue content")
        void loadReplacesExistingContent() throws Exception {
            catalogue.addPublication(book("Old Book", "Old Author", "Old Genre"));
            Path tmp = Files.createTempFile("cat-rep-", ".dat");
            try {
                Catalogue small = new Catalogue();
                small.addPublication(book("New Book", "New Author", "New Genre"));
                small.saveToFile(tmp.toString());

                catalogue.loadFromFile(tmp.toString());
                List<Publication> pubs = catalogue.getAllPublications();
                assertEquals(1, pubs.size());
                assertEquals("New Book", pubs.get(0).getTitle());
            } finally {
                Files.deleteIfExists(tmp);
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static Book book(String title, String author, String genre) {
        return new Book(title, 2020, author, "Publisher", genre);
    }
}
