public class BookNotFoundException extends Exception {

    public BookNotFoundException(String title) {
        super("Publication with title '" + title + "' was not found in the catalogue.");
    }
}
