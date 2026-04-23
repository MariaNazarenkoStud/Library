import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.*;

/**
 * Central logging configuration for the Book Catalogue application.
 *
 * <h2>Log level resolution order (highest priority first)</h2>
 * <ol>
 *   <li>JVM system property: {@code -Dlevel=FINE}</li>
 *   <li>Environment variable: {@code LOG_LEVEL=FINE}</li>
 *   <li>{@code logging.properties} file in the working directory</li>
 *   <li>Default: {@code INFO}</li>
 * </ol>
 *
 * <h2>File rotation</h2>
 * Log files are stored in {@code logs/} and rotate at 5 MB; up to 5 files are kept.
 * The file name pattern is {@code logs/bookcatalogue0.log … bookcatalogue4.log}.
 *
 * @see LogFormatter
 */
public final class AppLogger {

    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE_PATTERN = LOG_DIR + "/bookcatalogue%g.log";
    /** Maximum size of a single log file before rotation (5 MB). */
    private static final int FILE_LIMIT = 5 * 1024 * 1024;
    /** Number of rotating log files to keep. */
    private static final int FILE_COUNT = 5;

    private AppLogger() {}

    /**
     * Configures the global logging system.
     *
     * <p>Must be called once at application startup, before any logger is obtained.
     * If {@code logging.properties} exists in the working directory it is loaded first;
     * otherwise a programmatic configuration is applied.
     */
    public static void configure() {
        // Attempt to load logging.properties from the working directory.
        // This allows ops to change levels without touching code or rebuilding.
        Path propsFile = Path.of("logging.properties");
        if (Files.exists(propsFile)) {
            try (InputStream in = Files.newInputStream(propsFile)) {
                LogManager.getLogManager().readConfiguration(in);
                // After loading the file, still honour -Dlevel / LOG_LEVEL overrides
                applyLevelOverride(Logger.getLogger(""));
                return;
            } catch (IOException e) {
                System.err.println("[AppLogger] Could not read logging.properties: " + e.getMessage());
            }
        }
        configureProgrammatically();
    }

    /** Full programmatic setup used when no properties file is found. */
    private static void configureProgrammatically() {
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);                         // root accepts everything; handlers filter

        for (Handler h : root.getHandlers()) root.removeHandler(h);  // remove default console handler

        Level consoleLevel = resolveLevel();

        // Console handler — filtered to the resolved level
        ConsoleHandler console = new ConsoleHandler();
        console.setLevel(consoleLevel);
        console.setFormatter(new LogFormatter());
        root.addHandler(console);

        // File handler — always captures ALL levels for post-mortem analysis
        try {
            Files.createDirectories(Path.of(LOG_DIR));
            FileHandler file = new FileHandler(LOG_FILE_PATTERN, FILE_LIMIT, FILE_COUNT, true);
            file.setLevel(Level.ALL);
            file.setFormatter(new LogFormatter());
            root.addHandler(file);
        } catch (IOException e) {
            root.warning("Could not create file log handler: " + e.getMessage());
        }
    }

    /**
     * If {@code -Dlevel} or {@code LOG_LEVEL} are set, override the root logger level.
     * This override is applied even after {@code logging.properties} was loaded.
     */
    private static void applyLevelOverride(Logger root) {
        String override = System.getProperty("level");
        if (override == null) override = System.getenv("LOG_LEVEL");
        if (override != null) {
            root.setLevel(parseLevel(override));
            for (Handler h : root.getHandlers()) h.setLevel(parseLevel(override));
        }
    }

    /**
     * Resolves the effective log level from the environment.
     *
     * <p>Supported sources (in priority order):
     * <ul>
     *   <li>{@code -Dlevel=FINE} JVM system property</li>
     *   <li>{@code LOG_LEVEL=FINE} environment variable</li>
     *   <li>Fallback: {@link Level#INFO}</li>
     * </ul>
     */
    private static Level resolveLevel() {
        String prop = System.getProperty("level");
        if (prop != null) return parseLevel(prop);
        String env = System.getenv("LOG_LEVEL");
        if (env != null) return parseLevel(env);
        return Level.INFO;
    }

    private static Level parseLevel(String s) {
        try {
            return Level.parse(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("[AppLogger] Unknown level '" + s + "', using INFO.");
            return Level.INFO;
        }
    }

    /**
     * Returns a named logger for the given class.
     *
     * @param clazz the class requesting a logger
     * @return a {@link Logger} named after the simple class name
     */
    public static Logger getLogger(Class<?> clazz) {
        return Logger.getLogger(clazz.getSimpleName());
    }
}
