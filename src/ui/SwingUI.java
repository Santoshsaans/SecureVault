package src.ui;

import src.crypto.PasswordGenerator;
import src.crypto.PasswordStrength;
import src.storage.VaultEntry;
import src.storage.VaultFile;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class SwingUI {

    private static final String VAULT_PATH = "vault.dat";
    private static final int AUTO_LOCK_MS = 5 * 60 * 1000;      // 5 minutes
    private static final int CLIPBOARD_CLEAR_MS = 30 * 1000;    // 30 seconds

    private VaultFile vault;
    private List<VaultEntry> entries = new ArrayList<>();
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTable table;
    private JFrame frame;
    private JTextField searchField;

    private Timer autoLockTimer;
    private AWTEventListener activityListener;
    private JLabel statusLabel;

    public void start() {
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); }
        catch (Exception ignored) {}

        vault = new VaultFile(VAULT_PATH);
        if (vault.exists()) showUnlockDialog();
        else showCreateDialog();

        if (entries == null) return;
        showMainWindow();
    }

    // ---------- Create / Unlock dialogs ----------

    private void showCreateDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        JPasswordField p1 = new JPasswordField();
        JPasswordField p2 = new JPasswordField();
        JLabel strength = new JLabel("Strength: —");
        panel.add(new JLabel("Set master password (min 8 chars):"));
        panel.add(p1);
        panel.add(strength);
        panel.add(new JLabel("Confirm master password:"));
        panel.add(p2);

        p1.getDocument().addDocumentListener(simpleDocListener(() -> {
            String s = new String(p1.getPassword());
            PasswordStrength.Level lvl = PasswordStrength.evaluate(s);
            strength.setText("Strength: " + PasswordStrength.describe(lvl));
            strength.setForeground(colorFor(lvl));
        }));

        while (true) {
            int r = JOptionPane.showConfirmDialog(null, panel, "Create SecureVault",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) { entries = null; return; }

            String a = new String(p1.getPassword());
            String b = new String(p2.getPassword());

            if (a.length() < 8) {
                JOptionPane.showMessageDialog(null, "Master password must be at least 8 characters.");
                continue;
            }
            if (!PasswordStrength.isAcceptableForMaster(a)) {
                JOptionPane.showMessageDialog(null,
                    "Master password is too weak ("
                    + PasswordStrength.describe(PasswordStrength.evaluate(a))
                    + "). Use a mix of letters, numbers, and symbols.");
                continue;
            }
            if (!a.equals(b)) {
                JOptionPane.showMessageDialog(null, "Passwords do not match.");
                continue;
            }
            try {
                vault.createNew(a.toCharArray(), new ArrayList<>());
                entries = new ArrayList<>();
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
                return;
            }
        }
    }

    private void showUnlockDialog() {
        JPasswordField pf = new JPasswordField();
        while (true) {
            int r = JOptionPane.showConfirmDialog(null, pf, "Unlock SecureVault",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) { entries = null; return; }
            try {
                entries = vault.unlock(pf.getPassword());
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Incorrect master password. Try again.");
                pf.setText("");
            }
        }
    }

    // ---------- Main window ----------

    private void showMainWindow() {
        frame = new JFrame("SecureVault");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(760, 500);
        frame.setLocationRelativeTo(null);

        String[] cols = {"Site", "Username", "Notes"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        sorter = new TableRowSorter<>(tableModel);
        refreshTable();

        table = new JTable(tableModel);
        table.setRowSorter(sorter);
        table.setRowHeight(24);
        JScrollPane scroll = new JScrollPane(table);

        // Top bar: search + status
        searchField = new JTextField(20);
        searchField.getDocument().addDocumentListener(simpleDocListener(this::applyFilter));
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topBar.add(new JLabel("Search:"));
        topBar.add(searchField);

        // Buttons
        JButton addBtn = new JButton("Add");
        JButton deleteBtn = new JButton("Delete");
        JButton genBtn = new JButton("Generate Password");
        JButton copyBtn = new JButton("Copy Password");
        JButton lockBtn = new JButton("Lock Now");

        addBtn.addActionListener(e -> addEntry());
        deleteBtn.addActionListener(e -> deleteEntry());
        genBtn.addActionListener(e -> showGeneratedPassword());
        copyBtn.addActionListener(e -> copyPassword());
        lockBtn.addActionListener(e -> lockNow());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        buttons.add(addBtn);
        buttons.add(deleteBtn);
        buttons.add(copyBtn);
        buttons.add(genBtn);
        buttons.add(lockBtn);

        // Status bar
        statusLabel = new JLabel("  Auto-lock: 5 min inactivity.  Entries: " + entries.size());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JPanel center = new JPanel(new BorderLayout());
        center.add(topBar, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        frame.add(center, BorderLayout.CENTER);
        frame.add(buttons, BorderLayout.SOUTH);
        frame.add(statusLabel, BorderLayout.NORTH);
        frame.setVisible(true);

        startAutoLockMonitor();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (VaultEntry e : entries) {
            tableModel.addRow(new Object[]{e.getSite(), e.getUsername(), e.getNotes()});
        }
        if (statusLabel != null) {
            statusLabel.setText("  Auto-lock: 5 min inactivity.  Entries: " + entries.size());
        }
    }

    private void applyFilter() {
        String q = searchField.getText().trim();
        if (q.isEmpty()) sorter.setRowFilter(null);
        else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q)));
    }

    // ---------- Auto-lock ----------

    private void startAutoLockMonitor() {
        autoLockTimer = new Timer(AUTO_LOCK_MS, (ActionEvent e) -> lockNow());
        autoLockTimer.setRepeats(false);
        autoLockTimer.restart();

        activityListener = event -> {
            if (autoLockTimer != null) autoLockTimer.restart();
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(activityListener,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
    }

    private void stopAutoLockMonitor() {
        if (autoLockTimer != null) autoLockTimer.stop();
        if (activityListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
            activityListener = null;
        }
    }

    private void lockNow() {
        stopAutoLockMonitor();
        if (frame != null) frame.dispose();
        // Restart the app flow from scratch (shows Unlock dialog)
        SwingUtilities.invokeLater(() -> SwingUI.main(null));
    }

    // ---------- Add / Delete / Copy / Generate ----------

    private void addEntry() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        JTextField site = new JTextField();
        JTextField user = new JTextField();
        JPasswordField pass = new JPasswordField();
        JTextField notes = new JTextField();
        JLabel strength = new JLabel("Strength: —");
        JButton genBtn = new JButton("Generate");

        genBtn.addActionListener(e -> {
            pass.setText(PasswordGenerator.generate(20));
            updateStrengthLabel(strength, new String(pass.getPassword()));
        });
        pass.getDocument().addDocumentListener(simpleDocListener(() ->
            updateStrengthLabel(strength, new String(pass.getPassword()))));

        panel.add(new JLabel("Site:"));       panel.add(site);
        panel.add(new JLabel("Username:"));   panel.add(user);
        panel.add(new JLabel("Password:"));   panel.add(pass);
        panel.add(strength);
        panel.add(genBtn);
        panel.add(new JLabel("Notes:"));      panel.add(notes);

        int r = JOptionPane.showConfirmDialog(frame, panel, "Add Entry",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        if (site.getText().isBlank() || user.getText().isBlank()) {
            JOptionPane.showMessageDialog(frame, "Site and Username are required.");
            return;
        }
        String pw = new String(pass.getPassword());
        if (pw.isBlank()) pw = PasswordGenerator.generate(20);

        entries.add(new VaultEntry(site.getText().trim(), user.getText().trim(), pw, notes.getText().trim()));
        saveAndRefresh();
    }

    private void deleteEntry() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(frame, "Select an entry first."); return; }
        int modelRow = table.convertRowIndexToModel(row);
        int confirm = JOptionPane.showConfirmDialog(frame, "Delete this entry?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        entries.remove(modelRow);
        saveAndRefresh();
    }

    private void copyPassword() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(frame, "Select an entry first."); return; }
        int modelRow = table.convertRowIndexToModel(row);
        String pw = entries.get(modelRow).getPassword();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(pw), null);

        // Auto-clear clipboard after 30s
        Timer t = new Timer(CLIPBOARD_CLEAR_MS, e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(""), null);
        });
        t.setRepeats(false);
        t.start();

        JOptionPane.showMessageDialog(frame,
            "Password copied. Clipboard will clear in 30 seconds.");
    }

    private void showGeneratedPassword() {
        String pw = PasswordGenerator.generate(20);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(pw), null);

        Timer t = new Timer(CLIPBOARD_CLEAR_MS, e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(""), null);
        });
        t.setRepeats(false);
        t.start();

        JOptionPane.showMessageDialog(frame,
            "Generated (strength: " + PasswordStrength.describe(PasswordStrength.evaluate(pw))
            + ")\n\n" + pw + "\n\nCopied to clipboard; will clear in 30s.");
    }

    private void saveAndRefresh() {
        try {
            vault.save(entries);
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Save error: " + ex.getMessage());
        }
    }

    // ---------- Helpers ----------

    private void updateStrengthLabel(JLabel label, String pw) {
        PasswordStrength.Level lvl = PasswordStrength.evaluate(pw);
        label.setText("Strength: " + PasswordStrength.describe(lvl));
        label.setForeground(colorFor(lvl));
    }

    private Color colorFor(PasswordStrength.Level lvl) {
        switch (lvl) {
            case VERY_WEAK:   return new Color(200, 0, 0);
            case WEAK:        return new Color(220, 100, 0);
            case MEDIUM:      return new Color(200, 160, 0);
            case STRONG:      return new Color(0, 150, 0);
            case VERY_STRONG: return new Color(0, 120, 0);
            default:          return Color.BLACK;
        }
    }

    private DocumentListener simpleDocListener(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { r.run(); }
            public void removeUpdate(DocumentEvent e)  { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        };
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SwingUI().start());
    }
}