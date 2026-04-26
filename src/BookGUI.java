import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Main application window for the Book Catalogue desktop application.
 *
 * <p><strong>Architecture:</strong> {@code BookGUI} is the <em>View + Controller</em> layer
 * of a simple two-tier MVC design. All data operations are delegated to {@link Catalogue};
 * this class is responsible only for rendering state and dispatching user actions.
 *
 * <p><strong>UI layout:</strong>
 * <ul>
 *   <li><em>NORTH</em> — form panel (input fields + search/filter row)</li>
 *   <li><em>CENTER</em> — sortable {@link JTable} backed by {@link DefaultTableModel}</li>
 *   <li><em>SOUTH</em> — action buttons + status bar</li>
 * </ul>
 *
 * <p><strong>Sortable columns:</strong> The {@link DefaultTableModel} overrides
 * {@link DefaultTableModel#getColumnClass(int)} so that the Year column reports
 * {@link Integer#TYPE}, enabling numeric (rather than lexicographic) sorting via
 * {@link TableRowSorter}.
 *
 * <p>The application entry point is {@link #main(String[])}.
 *
 * @see Catalogue
 * @see Book
 */
public class BookGUI extends JFrame {

    private static final Logger LOGGER = AppLogger.getLogger(BookGUI.class);

    private final Catalogue catalogue = new Catalogue();

    /** Input field for the book title used in add/remove/update operations. */
    private final JTextField titleField = new JTextField(15);
    private final JTextField authorField = new JTextField(15);
    private final JTextField publisherField = new JTextField(15);
    private final JTextField yearField = new JTextField(6);
    private final JTextField genreField = new JTextField(10);

    /** Search field for title substring queries. */
    private final JTextField searchTitleField = new JTextField(12);
    /** Search field for author substring queries. */
    private final JTextField searchAuthorField = new JTextField(12);
    /** Combo box for genre-based filtering; populated from {@link Catalogue#getDistinctGenres()}. */
    private final JComboBox<String> genreCombo = new JComboBox<>();

    /**
     * Table model with two behavioural overrides:
     * <ul>
     *   <li>Cells are not directly editable — all edits go through the form.</li>
     *   <li>Column 3 (Year) reports {@link Integer} class for numeric sort order.</li>
     * </ul>
     */
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Title", "Author", "Publisher", "Year", "Genre"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 3 ? Integer.class : String.class;
        }
    };
    private final JTable table = new JTable(tableModel);

    /** Status bar at the bottom of the window; displays operation results. */
    private final JLabel statusLabel = new JLabel(" ");

    private static final String KEY_INVALID_YEAR = "error.invalid.year";

    /**
     * Constructs and initialises the main application window.
     *
     * <p>Sets up the layout, attaches the {@link TableRowSorter}, and arranges all
     * panels. The window is centred on screen and uses the system look-and-feel.
     */
    public BookGUI() {
        setTitle("Book Catalogue");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        genreCombo.addItem("All genres");
        table.setRowSorter(new TableRowSorter<>(tableModel));

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buildButtonPanel(), BorderLayout.CENTER);
        southPanel.add(statusLabel, BorderLayout.SOUTH);

        add(buildFormPanel(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        // Log application shutdown when window is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                LOGGER.info("Application shutting down — user closed the window.");
                dispose();
            }
        });

        pack();
        setMinimumSize(new Dimension(950, 500));
        setLocationRelativeTo(null);

        LOGGER.info("BookGUI initialised. Application ready.");
    }

    /**
     * Builds the top form panel containing two rows of controls:
     * <ol>
     *   <li>Input fields for Title, Author, Publisher, Year, Genre.</li>
     *   <li>Search-by-title field, search-by-author field, genre combo, and Reset button.</li>
     * </ol>
     *
     * @return a configured {@link JPanel} using {@link GridBagLayout}
     */
    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Book details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        String[] labels = {"Title:", "Author:", "Publisher:", "Year:", "Genre:"};
        JTextField[] fields = {titleField, authorField, publisherField, yearField, genreField};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = i * 2;
            gbc.gridy = 0;
            panel.add(new JLabel(labels[i]), gbc);
            gbc.gridx = i * 2 + 1;
            panel.add(fields[i], gbc);
        }

        gbc.gridy = 1;

        gbc.gridx = 0;
        panel.add(new JLabel("By title:"), gbc);
        gbc.gridx = 1;
        panel.add(searchTitleField, gbc);

        JButton searchTitleBtn = new JButton("Search");
        searchTitleBtn.addActionListener(e -> onSearchByTitle());
        gbc.gridx = 2;
        panel.add(searchTitleBtn, gbc);

        gbc.gridx = 3;
        panel.add(new JLabel("By author:"), gbc);
        gbc.gridx = 4;
        panel.add(searchAuthorField, gbc);

        JButton searchAuthorBtn = new JButton("Search");
        searchAuthorBtn.addActionListener(e -> onSearchByAuthor());
        gbc.gridx = 5;
        panel.add(searchAuthorBtn, gbc);

        gbc.gridx = 6;
        panel.add(new JLabel("Genre:"), gbc);
        gbc.gridx = 7;
        genreCombo.setPreferredSize(new Dimension(110, genreCombo.getPreferredSize().height));
        panel.add(genreCombo, gbc);
        genreCombo.addActionListener(e -> onFilterByGenre());

        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e -> onResetSearch());
        gbc.gridx = 8;
        panel.add(resetBtn, gbc);

        return panel;
    }

    /**
     * Builds the bottom button panel with action buttons for CRUD and utility operations.
     *
     * @return a configured {@link JPanel} using {@link FlowLayout}
     */
    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));

        JButton addBtn = new JButton("Add book");
        JButton removeBtn = new JButton("Remove book");
        JButton updateBtn = new JButton("Update book");
        JButton saveBtn = new JButton("Save to file");
        JButton loadBtn = new JButton("Load from file");
        JButton exportBtn = new JButton("Export CSV");
        JButton statsBtn = new JButton("Statistics");

        addBtn.addActionListener(e -> onAdd());
        removeBtn.addActionListener(e -> onRemove());
        updateBtn.addActionListener(e -> onUpdate());
        saveBtn.addActionListener(e -> onSave());
        loadBtn.addActionListener(e -> onLoad());
        exportBtn.addActionListener(e -> onExportCSV());
        statsBtn.addActionListener(e -> onShowStatistics());

        panel.add(addBtn);
        panel.add(removeBtn);
        panel.add(updateBtn);
        panel.add(saveBtn);
        panel.add(loadBtn);
        panel.add(exportBtn);
        panel.add(statsBtn);

        return panel;
    }

    /**
     * Handles the "Add book" action.
     *
     * <p>Validates that title and author are non-empty and that year is a valid integer,
     * then delegates to {@link Catalogue#addPublication(Publication)}.
     * Refreshes the table and genre combo after a successful add.
     */
    private void onAdd() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String publisher = publisherField.getText().trim();
        String genre = genreField.getText().trim();
        int year;

        if (title.isEmpty() || author.isEmpty()) {
            LOGGER.warning("Add rejected: title or author is empty. title='"
                    + title + "' author='" + author + "'");
            setStatus("Title and Author are required.", true);
            return;
        }
        try {
            year = Integer.parseInt(yearField.getText().trim());
        } catch (NumberFormatException ex) {
            ErrorHandler.warn(Messages.get(KEY_INVALID_YEAR), "onAdd",
                    "yearInput='" + yearField.getText().trim() + "'");
            setStatus(Messages.get(KEY_INVALID_YEAR), true);
            return;
        }

        Book book = new Book(title, year, author, publisher, genre);
        catalogue.addPublication(book);
        refreshTable(catalogue.getAllPublications());
        refreshGenreCombo();
        clearFields();
        setStatus("Book '" + title + "' added.", false);
    }

    /**
     * Handles the "Remove book" action.
     *
     * <p>Reads the title field and calls {@link Catalogue#removePublicationByTitle(String)}.
     * Shows a status error if the title field is empty or the book is not found.
     */
    private void onRemove() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            LOGGER.warning("Remove rejected: title field is empty.");
            setStatus("Enter title to remove.", true);
            return;
        }
        try {
            catalogue.removePublicationByTitle(title);
            refreshTable(catalogue.getAllPublications());
            refreshGenreCombo();
            clearFields();
            setStatus("Book '" + title + "' removed.", false);
        } catch (BookNotFoundException ex) {
            ErrorHandler.warn(ex.getMessage(), "onRemove", "title='" + title + "'");
            setStatus(ex.getMessage(), true);
        }
    }

    /**
     * Handles the "Update book" action.
     *
     * <p>Looks up the book by title and applies any non-blank field values as updates.
     * Fields left blank are not modified, allowing partial updates.
     */
    private void onUpdate() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            LOGGER.warning("Update rejected: title field is empty.");
            setStatus("Enter title to update.", true);
            return;
        }
        try {
            Publication existing = catalogue.findPublicationByTitle(title);
            if (existing instanceof Book book) {
                if (!authorField.getText().isBlank()) book.setAuthor(authorField.getText().trim());
                if (!publisherField.getText().isBlank()) book.setPublisher(publisherField.getText().trim());
                if (!genreField.getText().isBlank()) book.setGenre(genreField.getText().trim());
                if (!yearField.getText().isBlank()) {
                    try {
                        book.setYear(Integer.parseInt(yearField.getText().trim()));
                    } catch (NumberFormatException ex) {
                        ErrorHandler.warn(Messages.get(KEY_INVALID_YEAR), "onUpdate",
                                "yearInput='" + yearField.getText().trim() + "'");
                        setStatus(Messages.get(KEY_INVALID_YEAR), true);
                        return;
                    }
                }
                LOGGER.info("Updated book: '" + title + "'");
            }
            refreshTable(catalogue.getAllPublications());
            refreshGenreCombo();
            clearFields();
            setStatus("Book '" + title + "' updated.", false);
        } catch (BookNotFoundException ex) {
            ErrorHandler.warn(ex.getMessage(), "onUpdate", "title='" + title + "'");
            setStatus(ex.getMessage(), true);
        }
    }

    /**
     * Handles the "Save to file" action.
     *
     * <p>Opens a {@link JFileChooser} and delegates serialisation to
     * {@link Catalogue#saveToFile(String)}.
     * On failure shows a {@link UserErrorDialog} with the error ID.
     */
    private void onSave() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            try {
                catalogue.saveToFile(path);
                setStatus("Catalogue saved.", false);
            } catch (IOException ex) {
                ErrorHandler.ErrorReport report =
                        ErrorHandler.handle(ex, "saveToFile", "path=" + path);
                setStatus("[" + report.id() + "] Save failed.", true);
                UserErrorDialog.show(this, report, Messages.get("error.save.failed"));
            }
        }
    }

    /**
     * Handles the "Load from file" action.
     *
     * <p>Opens a {@link JFileChooser} and delegates deserialisation to
     * {@link Catalogue#loadFromFile(String)}. Refreshes the table and genre combo
     * after a successful load.
     * On failure shows a {@link UserErrorDialog} with the error ID.
     */
    private void onLoad() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            try {
                catalogue.loadFromFile(path);
                refreshTable(catalogue.getAllPublications());
                refreshGenreCombo();
                setStatus("Catalogue loaded.", false);
            } catch (IOException | ClassNotFoundException ex) {
                ErrorHandler.ErrorReport report =
                        ErrorHandler.handle(ex, "loadFromFile", "path=" + path);
                setStatus("[" + report.id() + "] Load failed.", true);
                UserErrorDialog.show(this, report, Messages.get("error.load.failed"));
            }
        }
    }

    /**
     * Filters the table by title substring taken from {@link #searchTitleField}.
     *
     * <p>If the field is empty all books are shown; otherwise delegates to
     * {@link Catalogue#searchByTitle(String)}.
     */
    private void onSearchByTitle() {
        String query = searchTitleField.getText().trim();
        refreshTable(query.isEmpty() ? catalogue.getAllPublications() : catalogue.searchByTitle(query));
        setStatus(query.isEmpty() ? "Showing all books." : "Search by title: " + query, false);
    }

    /**
     * Filters the table by author substring taken from {@link #searchAuthorField}.
     *
     * <p>If the field is empty all books are shown; otherwise delegates to
     * {@link Catalogue#searchByAuthor(String)}.
     */
    private void onSearchByAuthor() {
        String query = searchAuthorField.getText().trim();
        refreshTable(query.isEmpty() ? catalogue.getAllPublications() : catalogue.searchByAuthor(query));
        setStatus(query.isEmpty() ? "Showing all books." : "Search by author: " + query, false);
    }

    /**
     * Filters the table by the genre selected in {@link #genreCombo}.
     *
     * <p>Selecting "All genres" resets the filter and shows every publication.
     */
    private void onFilterByGenre() {
        String selected = (String) genreCombo.getSelectedItem();
        if (selected == null || selected.equals("All genres")) {
            refreshTable(catalogue.getAllPublications());
        } else {
            refreshTable(catalogue.filterByGenre(selected));
            setStatus("Filtered by genre: " + selected, false);
        }
    }

    /**
     * Handles the "Export CSV" action.
     *
     * <p>Opens a save dialog pre-filled with {@code catalogue.csv} and delegates to
     * {@link Catalogue#exportToCSV(String)}.
     * On failure shows a {@link UserErrorDialog} with the error ID.
     */
    private void onExportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("catalogue.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            try {
                catalogue.exportToCSV(path);
                setStatus("Exported to " + chooser.getSelectedFile().getName(), false);
            } catch (IOException ex) {
                ErrorHandler.ErrorReport report =
                        ErrorHandler.handle(ex, "exportToCSV", "path=" + path);
                setStatus("[" + report.id() + "] Export failed.", true);
                UserErrorDialog.show(this, report, Messages.get("error.export.failed"));
            }
        }
    }

    /**
     * Displays a modal statistics dialog with catalogue summary data.
     *
     * <p>The dialog shows total book count, unique author count, unique genre count,
     * and a per-genre breakdown obtained from {@link Catalogue#countByGenre()}.
     * A monospaced font is used to align the columnar output.
     */
    private void onShowStatistics() {
        int total = catalogue.getAllPublications().size();
        long authors = catalogue.countUniqueAuthors();
        java.util.Map<String, Long> byGenre = catalogue.countByGenre();

        StringBuilder sb = new StringBuilder();
        sb.append("Total books:      ").append(total).append("\n");
        sb.append("Unique authors:   ").append(authors).append("\n");
        sb.append("Unique genres:    ").append(byGenre.size()).append("\n");

        if (!byGenre.isEmpty()) {
            sb.append("\nBooks by genre:\n");
            byGenre.forEach((genre, count) ->
                    sb.append("  ").append(genre).append(": ").append(count).append("\n"));
        }

        LOGGER.fine("Statistics dialog opened: total=" + total
                + " authors=" + authors + " genres=" + byGenre.size());

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JOptionPane.showMessageDialog(this, area, "Catalogue Statistics",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Clears all search fields and the genre combo selection, restoring the full table view.
     */
    private void onResetSearch() {
        searchTitleField.setText("");
        searchAuthorField.setText("");
        genreCombo.setSelectedIndex(0);
        refreshTable(catalogue.getAllPublications());
        setStatus("Search cleared.", false);
    }

    /**
     * Repopulates the genre combo box from the current catalogue state.
     *
     * <p>The "All genres" sentinel is always present at index 0. The previously
     * selected item is restored after the rebuild to avoid losing the filter state.
     */
    private void refreshGenreCombo() {
        String current = (String) genreCombo.getSelectedItem();
        genreCombo.removeAllItems();
        genreCombo.addItem("All genres");
        for (String genre : catalogue.getDistinctGenres()) {
            genreCombo.addItem(genre);
        }
        if (current != null) genreCombo.setSelectedItem(current);
    }

    /**
     * Clears the table and repopulates it from the provided list of publications.
     *
     * <p>Non-{@code Book} publications fill only the Title and Year columns;
     * author, publisher and genre are left blank.
     *
     * @param list the publications to display; must not be {@code null}
     */
    private void refreshTable(List<Publication> list) {
        tableModel.setRowCount(0);
        for (Publication p : list) {
            if (p instanceof Book b) {
                tableModel.addRow(new Object[]{b.getTitle(), b.getAuthor(), b.getPublisher(), b.getYear(), b.getGenre()});
            } else {
                tableModel.addRow(new Object[]{p.getTitle(), "", "", p.getYear(), ""});
            }
        }
    }

    /**
     * Clears all data entry fields in the form panel.
     */
    private void clearFields() {
        titleField.setText("");
        authorField.setText("");
        publisherField.setText("");
        yearField.setText("");
        genreField.setText("");
    }

    /**
     * Updates the status bar with a message styled according to the error flag.
     *
     * @param message the status message to display
     * @param error   {@code true} to render the message in red; {@code false} for green
     */
    private void setStatus(String message, boolean error) {
        statusLabel.setForeground(error ? Color.RED : new Color(0, 120, 0));
        statusLabel.setText(" " + message);
        if (error) {
            LOGGER.warning("Status (error): " + message);
        } else {
            LOGGER.fine("Status: " + message);
        }
    }

    /**
     * Application entry point.
     *
     * <p>Applies the system look-and-feel and opens the main window on the
     * Event Dispatch Thread (EDT) as required by Swing's threading model.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        AppLogger.configure();
        Logger log = AppLogger.getLogger(BookGUI.class);
        log.info("=== Book Catalogue starting up ==="
                + " | java=" + System.getProperty("java.version")
                + " | os=" + System.getProperty("os.name")
                + " | level=" + Logger.getLogger("").getLevel());

        // Shutdown hook — logs application stop even on abnormal exit
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                log.info("=== Book Catalogue stopped ==="), "shutdown-logger"));

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new BookGUI().setVisible(true);
        });
    }
}
