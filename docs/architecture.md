# Architecture Overview

## Design pattern

The application follows a lightweight **two-tier MVC** (Model-View-Controller) pattern:

```
┌────────────┐        delegates        ┌───────────┐
│  BookGUI   │ ───────────────────────▶│ Catalogue │
│ (View +    │                         │  (Model)  │
│ Controller)│ ◀─── returns lists ──── │           │
└────────────┘                         └───────────┘
                                            │
                                    Publication (abstract)
                                            │
                                          Book
```

`BookGUI` is the only class that knows about Swing.
`Catalogue` knows nothing about the UI — it can be tested or reused independently.

## Class responsibilities

| Class | Layer | Responsibility |
|---|---|---|
| `Publication` | Model | Abstract base — title + year, `Serializable` |
| `Book` | Model | Concrete publication — adds author, publisher, genre |
| `Catalogue` | Model | In-memory storage, CRUD, search, CSV export, file I/O |
| `BookNotFoundException` | Model | Signals a missing-title lookup failure |
| `BookGUI` | View/Controller | Swing window, form input, table display, event handling |

## Key design decisions

### Checked exception for missing books
`BookNotFoundException` is a **checked** exception rather than a runtime exception.
This forces all callers to explicitly handle the "not found" case, preventing silent failures in remove/update flows.

### Defensive copy in `getAllPublications()`
`Catalogue.getAllPublications()` returns `new ArrayList<>(publications)`.
This prevents the UI layer from accidentally mutating the catalogue's internal list.

### Serialisation strategy
The entire `publications` list is serialised as one object graph via `ObjectOutputStream`.
This keeps the persistence layer dependency-free (no database, no external libraries).
The trade-off is that the binary format is JVM-specific and not human-readable; CSV export exists as a human-readable alternative.

### Numeric column sorting
`DefaultTableModel.getColumnClass(int)` is overridden to return `Integer.class` for the Year column.
`TableRowSorter` uses this type information to apply numeric (not lexicographic) comparison,
so `1965 < 2008` sorts correctly.

### Genre combo refresh
After every add/remove/update/load operation, `refreshGenreCombo()` is called to keep
the filter combo box in sync with the current data. The previously selected value is
restored to avoid losing the active filter state.

## Component interaction diagram

```
User action
     │
     ▼
 BookGUI event handler (e.g. onAdd)
     │  validates input
     │
     ▼
 Catalogue.addPublication(book)        ← mutates internal list
     │
     ▼
 BookGUI.refreshTable(getAllPublications())  ← re-renders table
 BookGUI.refreshGenreCombo()               ← re-syncs filter
```

## Business logic location

All business logic (search, filter, statistics) lives in `Catalogue`.
`BookGUI` contains only display/validation logic.
This separation ensures the model can be unit-tested without starting the Swing event loop.
