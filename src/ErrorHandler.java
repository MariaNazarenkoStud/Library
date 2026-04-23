import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized error handling for the Book Catalogue application.
 *
 * <p>Every error that surfaces to the user gets a <strong>unique identifier</strong>
 * in the format {@code ERR-YYYYMMDD-HHMMSS-XXXX}.  This ID ties together the
 * log entry and the user-facing dialog, so a support request ("I got error
 * ERR-20260424-021530-4821") can be matched to the exact log line instantly.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * try {
 *     catalogue.saveToFile(path);
 * } catch (IOException e) {
 *     ErrorHandler.ErrorReport report = ErrorHandler.handle(e, "saveToFile", "path=" + path);
 *     UserErrorDialog.show(parent, report, Messages.get("error.save.failed"));
 * }
 * }</pre>
 *
 * @see UserErrorDialog
 * @see AppLogger
 */
public final class ErrorHandler {

    private static final Logger LOGGER = AppLogger.getLogger(ErrorHandler.class);
    private static final Random RAND = new Random();
    private static final DateTimeFormatter ID_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ErrorHandler() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Handles a throwable by generating a unique error ID, logging the full
     * context at SEVERE level, and returning a structured {@link ErrorReport}.
     *
     * @param t         the exception or error to handle
     * @param operation the name of the operation that failed (e.g. {@code "saveToFile"})
     * @param context   additional diagnostic context (parameters, state)
     * @return a {@link ErrorReport} ready to display to the user
     */
    public static ErrorReport handle(Throwable t, String operation, String context) {
        String id = generateId();
        LOGGER.log(Level.SEVERE,
                String.format("[%s] operation=%s | context={%s} | %s: %s",
                        id, operation, context,
                        t.getClass().getSimpleName(), t.getMessage()),
                t);
        return new ErrorReport(id, operation, context, t, LocalDateTime.now());
    }

    /**
     * Logs a warning-level issue that does not need a user dialog
     * (e.g. user typed an incorrect title).
     *
     * @param message   description of the issue
     * @param operation the operation where the issue occurred
     * @param context   additional context information
     * @return a unique ID for this warning event
     */
    public static String warn(String message, String operation, String context) {
        String id = generateId();
        LOGGER.warning(String.format("[%s] operation=%s | context={%s} | %s",
                id, operation, context, message));
        return id;
    }

    /**
     * Generates a unique error identifier.
     *
     * <p>Format: {@code ERR-YYYYMMDD-HHMMSS-XXXX} where XXXX is a random 4-digit number.
     * The timestamp component makes the ID directly traceable in log files.
     *
     * @return unique error ID string
     */
    public static String generateId() {
        return "ERR-" + LocalDateTime.now().format(ID_FMT)
                + "-" + String.format("%04d", RAND.nextInt(10_000));
    }

    // ── ErrorReport record ────────────────────────────────────────────────────

    /**
     * Immutable snapshot of a handled error, including all data needed
     * for the user-facing dialog and the crash-report file.
     *
     * @param id        unique error identifier
     * @param operation name of the failing operation
     * @param context   diagnostic context string
     * @param cause     the original exception
     * @param timestamp when the error was handled
     */
    public record ErrorReport(
            String id,
            String operation,
            String context,
            Throwable cause,
            LocalDateTime timestamp
    ) {
        /**
         * Produces a multi-line crash-report string that can be saved to a file
         * and sent to the developer.
         *
         * <p>Contains: error ID, timestamp, operation, context, exception class,
         * message, Java version, OS, and full stack trace.
         *
         * @return formatted crash report text
         */
        public String toReportString() {
            return String.format(
                    "=== Book Catalogue Error Report ===%n" +
                    "Error ID  : %s%n" +
                    "Timestamp : %s%n" +
                    "Operation : %s%n" +
                    "Context   : %s%n" +
                    "Exception : %s%n" +
                    "Message   : %s%n" +
                    "Java      : %s%n" +
                    "OS        : %s %s%n" +
                    "Stack trace:%n%s",
                    id, timestamp, operation, context,
                    cause.getClass().getName(),
                    cause.getMessage(),
                    System.getProperty("java.version"),
                    System.getProperty("os.name"),
                    System.getProperty("os.version"),
                    stackTraceAsString());
        }

        /** Saves the crash report to the given file path. */
        public void saveToFile(Path path) throws IOException {
            Files.writeString(path, toReportString());
        }

        private String stackTraceAsString() {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement el : cause.getStackTrace()) {
                sb.append("  at ").append(el).append(System.lineSeparator());
            }
            return sb.toString();
        }
    }
}
