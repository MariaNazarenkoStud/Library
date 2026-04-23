import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Single-line log formatter producing human-readable, grep-friendly output.
 *
 * <h2>Output format</h2>
 * <pre>
 * [2026-04-24 02:15:30.123] [INFO   ] [Catalogue           ] Added publication: 'Clean Code'
 * [2026-04-24 02:15:31.045] [SEVERE ] [ErrorHandler        ] [ERR-20260424-021531-4821] ...
 * </pre>
 *
 * Exceptions are appended on subsequent indented lines.
 */
public final class LogFormatter extends Formatter {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public String format(LogRecord record) {
        LocalDateTime time = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(record.getMillis()), ZoneId.systemDefault());

        String line = String.format("[%s] [%-7s] [%-20s] %s%n",
                DT_FMT.format(time),
                record.getLevel().getName(),
                truncate(record.getLoggerName(), 20),
                formatMessage(record));

        Throwable t = record.getThrown();
        if (t != null) {
            StringBuilder sb = new StringBuilder(line);
            sb.append("  ").append(t.getClass().getName())
              .append(": ").append(t.getMessage()).append(System.lineSeparator());
            for (StackTraceElement el : t.getStackTrace()) {
                sb.append("    at ").append(el).append(System.lineSeparator());
            }
            return sb.toString();
        }
        return line;
    }

    private static String truncate(String s, int max) {
        if (s == null || s.isEmpty()) return "";
        return s.length() <= max ? s : "…" + s.substring(s.length() - (max - 1));
    }
}
