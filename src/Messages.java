import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Localization helper for user-facing messages.
 *
 * <p>Messages are loaded from {@code messages_uk.properties} (Ukrainian) or
 * {@code messages.properties} (English, default).  Both files are read as
 * <strong>UTF-8</strong> via {@link InputStreamReader}, so Cyrillic text works
 * without unicode escapes.
 *
 * <h2>Language resolution order</h2>
 * <ol>
 *   <li>JVM property: {@code -Dapp.lang=uk}</li>
 *   <li>Environment variable: {@code APP_LANG=uk}</li>
 *   <li>Default: {@code uk} (Ukrainian)</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * String msg = Messages.get("error.save.failed");
 * String fmt = Messages.get("error.some.format", arg1, arg2);
 * }</pre>
 */
public final class Messages {

    private static final Logger LOGGER = AppLogger.getLogger(Messages.class);
    private static final ResourceBundle BUNDLE;

    static {
        String lang = System.getProperty("app.lang",
                Optional.ofNullable(System.getenv("APP_LANG")).orElse("uk"));
        BUNDLE = loadBundle(lang);
    }

    private Messages() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the localized message for the given key.
     *
     * @param key the message key (e.g. {@code "error.save.failed"})
     * @return the localized string; falls back to {@code [key]} if not found
     */
    public static String get(String key) {
        try {
            return BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            LOGGER.fine("Missing message key: " + key);
            return "[" + key + "]";
        }
    }

    /**
     * Returns the localized message formatted with the given arguments.
     *
     * @param key  message key
     * @param args format arguments passed to {@link String#format}
     * @return formatted localized string
     */
    public static String get(String key, Object... args) {
        return String.format(get(key), args);
    }

    // ── Bundle loading ────────────────────────────────────────────────────────

    /**
     * Loads a {@link ResourceBundle} from the classpath as UTF-8.
     *
     * <p>Tries the locale-specific file first ({@code messages_uk.properties}),
     * then falls back to the default ({@code messages.properties}).
     */
    private static ResourceBundle loadBundle(String lang) {
        String localeName = "messages_" + lang + ".properties";
        ResourceBundle rb = tryLoad(localeName);
        if (rb == null) {
            LOGGER.config("No bundle for lang='" + lang + "'; falling back to messages.properties");
            rb = tryLoad("messages.properties");
        }
        if (rb == null) {
            // Last resort — use JDK built-in (may fail for Cyrillic)
            LOGGER.warning("Could not load any messages bundle; using empty fallback.");
            rb = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        }
        return rb;
    }

    private static ResourceBundle tryLoad(String resourceName) {
        InputStream is = Messages.class.getClassLoader().getResourceAsStream(resourceName);
        if (is == null) return null;
        try (Reader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return new PropertyResourceBundle(r);
        } catch (IOException e) {
            LOGGER.warning("Failed to read " + resourceName + ": " + e.getMessage());
            return null;
        }
    }
}
