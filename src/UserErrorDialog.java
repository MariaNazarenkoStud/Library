import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * User-friendly error dialog for the Book Catalogue application.
 *
 * <p>Shows a localized, non-technical error message alongside the unique error ID
 * so the user can:
 * <ul>
 *   <li>understand what went wrong without seeing a Java stack trace;</li>
 *   <li>copy the error ID to quote it in a support request;</li>
 *   <li>save a full technical crash report to a file and send it to the developer.</li>
 * </ul>
 *
 * <p>All text is loaded from {@link Messages}, so the dialog is automatically
 * shown in the active locale (Ukrainian by default).
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * try {
 *     catalogue.saveToFile(path);
 * } catch (IOException e) {
 *     ErrorHandler.ErrorReport report = ErrorHandler.handle(e, "saveToFile", "path=" + path);
 *     UserErrorDialog.show(parentComponent, report, Messages.get("error.save.failed"));
 * }
 * }</pre>
 *
 * @see ErrorHandler
 * @see Messages
 */
public final class UserErrorDialog {

    private static final Logger LOGGER = AppLogger.getLogger(UserErrorDialog.class);

    private UserErrorDialog() {}

    /**
     * Shows the error dialog with a generic message.
     *
     * @param parent the parent component for positioning (may be {@code null})
     * @param report the error report produced by {@link ErrorHandler#handle}
     */
    public static void show(Component parent, ErrorHandler.ErrorReport report) {
        show(parent, report, Messages.get("error.generic"));
    }

    /**
     * Shows the error dialog with a specific localized user message.
     *
     * @param parent      the parent component for positioning (may be {@code null})
     * @param report      the error report produced by {@link ErrorHandler#handle}
     * @param userMessage the localized, non-technical message to display to the user
     */
    public static void show(Component parent, ErrorHandler.ErrorReport report, String userMessage) {
        // Must run on the EDT
        if (SwingUtilities.isEventDispatchThread()) {
            buildAndShow(parent, report, userMessage);
        } else {
            SwingUtilities.invokeLater(() -> buildAndShow(parent, report, userMessage));
        }
    }

    // ── Dialog construction ───────────────────────────────────────────────────

    private static void buildAndShow(Component parent, ErrorHandler.ErrorReport report,
                                     String userMessage) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        JDialog dialog = new JDialog(owner,
                Messages.get("error.dialog.title"),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // ── Top panel: icon + message ─────────────────────────────────────────
        JPanel topPanel = new JPanel(new BorderLayout(12, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 10, 18));

        JLabel errorIcon = new JLabel(UIManager.getIcon("OptionPane.errorIcon"));
        errorIcon.setVerticalAlignment(SwingConstants.TOP);
        topPanel.add(errorIcon, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel mainLabel = new JLabel("<html><b>" + escapeHtml(userMessage) + "</b></html>");
        mainLabel.setFont(mainLabel.getFont().deriveFont(13f));

        JLabel retryLabel = new JLabel("<html><font color='#555555'>"
                + escapeHtml(Messages.get("error.action.retry")) + "</font></html>");
        retryLabel.setFont(retryLabel.getFont().deriveFont(12f));

        JLabel contactLabel = new JLabel("<html><font color='#555555'>"
                + escapeHtml(Messages.get("error.action.contact")) + "</font></html>");
        contactLabel.setFont(contactLabel.getFont().deriveFont(12f));

        textPanel.add(mainLabel);
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(retryLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(contactLabel);
        topPanel.add(textPanel, BorderLayout.CENTER);

        // ── Middle panel: error ID ────────────────────────────────────────────
        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        idPanel.setBorder(BorderFactory.createEmptyBorder(4, 18, 10, 18));

        JLabel idLabel = new JLabel(Messages.get("error.id.label") + "  ");
        idLabel.setFont(idLabel.getFont().deriveFont(Font.BOLD, 12f));

        JTextField idField = new JTextField(report.id(), 28);
        idField.setEditable(false);
        idField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        idField.setBackground(new Color(245, 245, 245));
        idField.setBorder(BorderFactory.createCompoundBorder(
                idField.getBorder(),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));

        idPanel.add(idLabel);
        idPanel.add(idField);

        // ── Bottom panel: action buttons ─────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 10));
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)));

        JButton copyBtn  = new JButton(Messages.get("error.copy.id"));
        JButton saveBtn  = new JButton(Messages.get("error.save.report"));
        JButton closeBtn = new JButton(Messages.get("error.close"));

        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(report.id()), null);
            copyBtn.setText("\u2713 " + Messages.get("error.copy.id"));
            copyBtn.setEnabled(false);
        });

        saveBtn.addActionListener(e -> saveReport(dialog, report));
        closeBtn.addActionListener(e -> dialog.dispose());

        getRootPane(dialog).setDefaultButton(closeBtn);

        btnPanel.add(copyBtn);
        btnPanel.add(saveBtn);
        btnPanel.add(closeBtn);

        // ── Assemble ──────────────────────────────────────────────────────────
        dialog.add(topPanel,  BorderLayout.NORTH);
        dialog.add(idPanel,   BorderLayout.CENTER);
        dialog.add(btnPanel,  BorderLayout.SOUTH);

        dialog.pack();
        dialog.setMinimumSize(new Dimension(460, dialog.getHeight()));
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void saveReport(Component parent, ErrorHandler.ErrorReport report) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("error-report-" + report.id() + ".txt"));
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            Path target = chooser.getSelectedFile().toPath();
            try {
                report.saveToFile(target);
                LOGGER.info("Error report saved to: " + target);
                JOptionPane.showMessageDialog(parent,
                        Messages.get("error.report.saved") + "\n" + target,
                        Messages.get("error.save.report"),
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                LOGGER.warning("Could not save error report: " + ex.getMessage());
            }
        }
    }

    private static JRootPane getRootPane(JDialog dialog) {
        return dialog.getRootPane();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
