import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Lightweight performance instrumentation utility.
 *
 * <p>Wraps {@link System#nanoTime()} to measure elapsed time for named operations.
 * Accumulated samples allow computing mean, median and p95 latencies.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * Profiler.start("searchByTitle");
 * List<Publication> r = catalogue.searchByTitle("Clean");
 * long ns = Profiler.stop("searchByTitle");
 *
 * // after N iterations:
 * Profiler.printSummary();
 * }</pre>
 *
 * <p>All methods are thread-safe.
 */
public final class Profiler {

    private static final Logger LOGGER = AppLogger.getLogger(Profiler.class);

    private static final ConcurrentHashMap<String, Long>       starts  = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<Long>> samples = new ConcurrentHashMap<>();

    private Profiler() {}

    /**
     * Starts (or restarts) the timer for the named operation.
     *
     * @param op operation label; used as key for aggregation
     */
    public static void start(String op) {
        starts.put(op, System.nanoTime());
    }

    /**
     * Stops the timer for the named operation and records the elapsed time.
     *
     * @param op operation label matching the one passed to {@link #start}
     * @return elapsed nanoseconds; 0 if {@code start} was never called
     */
    public static long stop(String op) {
        long start = starts.getOrDefault(op, System.nanoTime());
        long elapsed = System.nanoTime() - start;
        samples.computeIfAbsent(op, k -> Collections.synchronizedList(new ArrayList<>()))
               .add(elapsed);
        LOGGER.fine(String.format("[PERF] %-30s %.3f ms", op, elapsed / 1_000_000.0));
        return elapsed;
    }

    /**
     * Prints a formatted summary table of all recorded operations to stdout.
     *
     * <p>Columns: operation name, call count, mean ms, median ms, p95 ms, total ms.
     */
    public static void printSummary() {
        System.out.println();
        System.out.printf("%-32s %6s %9s %9s %9s %10s%n",
                "Operation", "Calls", "Mean ms", "Med ms", "P95 ms", "Total ms");
        System.out.println("-".repeat(82));
        samples.entrySet().stream()
                .sorted((a, b) -> {
                    double ta = average(a.getValue()), tb = average(b.getValue());
                    return Double.compare(tb, ta);
                })
                .forEach(e -> {
                    List<Long> ns = e.getValue();
                    List<Long> sorted = new ArrayList<>(ns);
                    Collections.sort(sorted);
                    double mean   = average(ns)               / 1_000_000.0;
                    double median = sorted.get(ns.size() / 2) / 1_000_000.0;
                    double p95    = sorted.get((int)(ns.size() * 0.95)) / 1_000_000.0;
                    double total  = ns.stream().mapToLong(Long::longValue).sum() / 1_000_000.0;
                    System.out.printf("%-32s %6d %9.3f %9.3f %9.3f %10.3f%n",
                            e.getKey(), ns.size(), mean, median, p95, total);
                });
        System.out.println();
    }

    /** Clears all recorded samples and timers. */
    public static void reset() {
        starts.clear();
        samples.clear();
    }

    private static double average(List<Long> list) {
        return list.stream().mapToLong(Long::longValue).average().orElse(0);
    }
}
