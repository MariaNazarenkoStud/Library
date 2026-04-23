# Logging & Error Handling — Book Catalogue

## Overview

The application uses **`java.util.logging` (JUL)** — Java's built-in logging framework.
No external libraries are required. The logging infrastructure consists of:

| Class | Role |
|---|---|
| `AppLogger` | Configures handlers, levels, and rotation at startup |
| `LogFormatter` | Single-line human-readable format with millisecond timestamps |
| `ErrorHandler` | Generates unique error IDs; produces structured `ErrorReport` |
| `Messages` | Loads localized strings from `.properties` files |
| `UserErrorDialog` | User-facing dialog with error ID, copy button, and report export |

---

## Log levels

| JUL Level | Numeric | When used |
|---|---|---|
| `SEVERE` | 1000 | Unrecoverable errors: failed I/O, serialization errors |
| `WARNING` | 900 | User errors: book not found, invalid year, missing title |
| `INFO` | 800 | Normal operations: add, remove, save, load, CSV export, startup, shutdown |
| `CONFIG` | 700 | Configuration events: bundle loading, level overrides |
| `FINE` | 500 | Diagnostic: search results, filter results, statistics opened |
| `FINER` / `FINEST` | 400/300 | Reserved for future detailed tracing |

---

## Setting the log level without recompilation (65%)

Three equivalent ways, in priority order:

### 1. JVM system property (highest priority)
```bash
java -Dlevel=FINE -jar dist/BookCatalogue.jar
java -Dlevel=FINEST -jar dist/BookCatalogue.jar
```

### 2. Environment variable
```bash
# macOS / Linux
export LOG_LEVEL=FINE
java -jar dist/BookCatalogue.jar

# Windows (PowerShell)
$env:LOG_LEVEL = "FINE"
java -jar dist/BookCatalogue.jar
```

### 3. `logging.properties` file (in the working directory)
Edit the file in the project root and restart:
```properties
.level = FINE
# Enable FINEST only for Catalogue
Catalogue.level = FINEST
```
AppLogger loads this file automatically on startup. No flag needed.

Alternatively, use the standard JVM property:
```bash
java -Djava.util.logging.config.file=logging.properties -jar dist/BookCatalogue.jar
```

---

## Log format

Every log line follows the pattern:
```
[YYYY-MM-DD HH:mm:ss.SSS] [LEVEL  ] [LoggerName          ] Message text here
```

**Example output:**
```
[2026-04-24 02:15:30.001] [INFO   ] [BookGUI             ] === Book Catalogue starting up === | java=17.0.9 | os=Mac OS X | level=INFO
[2026-04-24 02:15:30.245] [INFO   ] [BookGUI             ] BookGUI initialised. Application ready.
[2026-04-24 02:15:45.812] [INFO   ] [Catalogue           ] Added publication: 'Clean Code' (2008)
[2026-04-24 02:16:02.113] [WARNING] [Catalogue           ] Publication not found: 'Unknown Book' (total in catalogue: 3)
[2026-04-24 02:16:02.115] [WARNING] [ErrorHandler        ] [ERR-20260424-021602-3741] operation=onRemove | context={title='Unknown Book'} | BookNotFoundException: ...
[2026-04-24 02:17:10.440] [SEVERE ] [ErrorHandler        ] [ERR-20260424-021710-9820] operation=saveToFile | context={path=/read-only/cat.dat} | IOException: Permission denied
```

---

## File rotation (85%)

Log files are stored in the `logs/` directory and rotate **by size**:
- Maximum size per file: **5 MB** (`FileHandler.limit = 5242880`)
- Files kept: **5** (`FileHandler.count = 5`)
- Pattern: `logs/bookcatalogue0.log` … `logs/bookcatalogue4.log`
- New entries are always appended to file 0; old files shift up.

**Time-based rotation** (external):  
JUL does not support time-based rotation natively. On Linux/macOS, use `logrotate`:
```
/path/to/Library/logs/bookcatalogue*.log {
    daily
    rotate 7
    compress
    missingok
    notifempty
    copytruncate
}
```
Schedule via `/etc/logrotate.d/bookcatalogue` or a cron job.

---

## Unique error IDs (75%)

Every error that reaches the user gets a unique identifier in the format:

```
ERR-YYYYMMDD-HHMMSS-XXXX
```

Example: `ERR-20260424-021710-9820`

- **Date + time** component allows instant location in log files  
- **4-digit random suffix** disambiguates errors that happen within the same second  

The same ID appears in:
1. The `SEVERE` / `WARNING` log line
2. The user-facing error dialog
3. The saved crash-report file

---

## User-facing error dialog (100%)

When a critical operation fails (save, load, export), the application shows a
non-technical dialog in the active locale:

```
┌─────────────────────────────────────────────┐
│  [!]  Не вдалося зберегти каталог. Перевірте│
│       наявність дозволу на запис.           │
│       Спробуйте виконати дію повторно.      │
│       Якщо помилка повторюється, збережіть  │
│       звіт та надішліть розробнику.         │
│                                             │
│  Ідентифікатор помилки: ERR-20260424-...    │
│                                             │
│  [Копіювати ID]  [Зберегти звіт]  [Закрити]│
└─────────────────────────────────────────────┘
```

"Зберегти звіт" exports a `.txt` crash report containing:
error ID, timestamp, operation, exception class, Java version, OS, and stack trace.

---

## Localization (100%)

Language is resolved from (priority order):

| Source | Example |
|---|---|
| JVM property | `-Dapp.lang=en` |
| Environment variable | `APP_LANG=en` |
| Default | `uk` (Ukrainian) |

Message files:
- `messages.properties` — English (default fallback)
- `messages_uk.properties` — Ukrainian

Both files are read as **UTF-8**, so Cyrillic text is stored directly without unicode escapes.

---

## Configuration files

| File | Location | Purpose |
|---|---|---|
| `logging.properties` | project root | Runtime log level and handler configuration |
| `src/messages.properties` | `src/` / classpath | English messages |
| `src/messages_uk.properties` | `src/` / classpath | Ukrainian messages |
