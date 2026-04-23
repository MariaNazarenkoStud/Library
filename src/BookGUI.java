import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class BookGUI extends JFrame {

    private final Catalogue catalogue = new Catalogue();

    private final JTextField titleField = new JTextField(15);
    private final JTextField authorField = new JTextField(15);
    private final JTextField publisherField = new JTextField(15);
    private final JTextField yearField = new JTextField(6);
    private final JTextField genreField = new JTextField(10);
    private final JTextField searchTitleField = new JTextField(12);
    private final JTextField searchAuthorField = new JTextField(12);
    private final JComboBox<String> genreCombo = new JComboBox<>();

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

    private final JLabel statusLabel = new JLabel(" ");

    public BookGUI() {
        setTitle("Book Catalogue");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        genreCombo.addItem("All genres");
        table.setRowSorter(new TableRowSorter<>(tableModel));

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buildButtonPanel(), BorderLayout.CENTER);
        southPanel.add(statusLabel, BorderLayout.SOUTH);

        add(buildFormPanel(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(950, 500));
        setLocationRelativeTo(null);
    }

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

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));

        JButton addBtn = new JButton("Add book");
        JButton removeBtn = new JButton("Remove book");
        JButton updateBtn = new JButton("Update book");
        JButton saveBtn = new JButton("Save to file");
        JButton loadBtn = new JButton("Load from file");
        JButton exportBtn = new JButton("Export CSV");

        addBtn.addActionListener(e -> onAdd());
        removeBtn.addActionListener(e -> onRemove());
        updateBtn.addActionListener(e -> onUpdate());
        saveBtn.addActionListener(e -> onSave());
        loadBtn.addActionListener(e -> onLoad());
        exportBtn.addActionListener(e -> onExportCSV());

        panel.add(addBtn);
        panel.add(removeBtn);
        panel.add(updateBtn);
        panel.add(saveBtn);
        panel.add(loadBtn);
        panel.add(exportBtn);

        return panel;
    }

    private void onAdd() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String publisher = publisherField.getText().trim();
        String genre = genreField.getText().trim();
        int year;

        if (title.isEmpty() || author.isEmpty()) {
            setStatus("Title and Author are required.", true);
            return;
        }
        try {
            year = Integer.parseInt(yearField.getText().trim());
        } catch (NumberFormatException ex) {
            setStatus("Year must be a valid number.", true);
            return;
        }

        Book book = new Book(title, year, author, publisher, genre);
        catalogue.addPublication(book);
        refreshTable(catalogue.getAllPublications());
        refreshGenreCombo();
        clearFields();
        setStatus("Book '" + title + "' added.", false);
    }

    private void onRemove() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
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
            setStatus(ex.getMessage(), true);
        }
    }

    private void onUpdate() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
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
                        setStatus("Year must be a valid number.", true);
                        return;
                    }
                }
            }
            refreshTable(catalogue.getAllPublications());
            refreshGenreCombo();
            clearFields();
            setStatus("Book '" + title + "' updated.", false);
        } catch (BookNotFoundException ex) {
            setStatus(ex.getMessage(), true);
        }
    }

    private void onSave() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                catalogue.saveToFile(chooser.getSelectedFile().getAbsolutePath());
                setStatus("Catalogue saved.", false);
            } catch (IOException ex) {
                setStatus("Save error: " + ex.getMessage(), true);
            }
        }
    }

    private void onLoad() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                catalogue.loadFromFile(chooser.getSelectedFile().getAbsolutePath());
                refreshTable(catalogue.getAllPublications());
                refreshGenreCombo();
                setStatus("Catalogue loaded.", false);
            } catch (IOException | ClassNotFoundException ex) {
                setStatus("Load error: " + ex.getMessage(), true);
            }
        }
    }

    private void onSearchByTitle() {
        String query = searchTitleField.getText().trim();
        refreshTable(query.isEmpty() ? catalogue.getAllPublications() : catalogue.searchByTitle(query));
        setStatus(query.isEmpty() ? "Showing all books." : "Search by title: " + query, false);
    }

    private void onSearchByAuthor() {
        String query = searchAuthorField.getText().trim();
        refreshTable(query.isEmpty() ? catalogue.getAllPublications() : catalogue.searchByAuthor(query));
        setStatus(query.isEmpty() ? "Showing all books." : "Search by author: " + query, false);
    }

    private void onFilterByGenre() {
        String selected = (String) genreCombo.getSelectedItem();
        if (selected == null || selected.equals("All genres")) {
            refreshTable(catalogue.getAllPublications());
        } else {
            refreshTable(catalogue.filterByGenre(selected));
            setStatus("Filtered by genre: " + selected, false);
        }
    }

    private void onExportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("catalogue.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                catalogue.exportToCSV(chooser.getSelectedFile().getAbsolutePath());
                setStatus("Exported to " + chooser.getSelectedFile().getName(), false);
            } catch (IOException ex) {
                setStatus("Export error: " + ex.getMessage(), true);
            }
        }
    }

    private void onResetSearch() {
        searchTitleField.setText("");
        searchAuthorField.setText("");
        genreCombo.setSelectedIndex(0);
        refreshTable(catalogue.getAllPublications());
        setStatus("Search cleared.", false);
    }

    private void refreshGenreCombo() {
        String current = (String) genreCombo.getSelectedItem();
        genreCombo.removeAllItems();
        genreCombo.addItem("All genres");
        for (String genre : catalogue.getDistinctGenres()) {
            genreCombo.addItem(genre);
        }
        if (current != null) genreCombo.setSelectedItem(current);
    }

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

    private void clearFields() {
        titleField.setText("");
        authorField.setText("");
        publisherField.setText("");
        yearField.setText("");
        genreField.setText("");
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setForeground(error ? Color.RED : new Color(0, 120, 0));
        statusLabel.setText(" " + message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new BookGUI().setVisible(true);
        });
    }
}
