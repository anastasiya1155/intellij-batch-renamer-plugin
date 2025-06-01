package solop.cc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nullable;
import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.KeyStroke;
import javax.swing.table.TableCellEditor;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RenameSymbolsDialog extends DialogWrapper {
    private final Project project;
    private final List<SymbolRenameInfo> symbols;
    private JBTable symbolsTable;
    private SymbolTableModel tableModel;
    private JBLabel statusLabel;
    private JTextField searchField;

    public static class SymbolRenameInfo {
        private final PsiNamedElement element;
        private final String originalName;
        private String newName;
        private final int offset;

        public SymbolRenameInfo(PsiNamedElement element, int offset) {
            this.element = element;
            this.originalName = element.getName();
            this.newName = "";
            this.offset = offset;
        }

        public PsiNamedElement getElement() {
            return element;
        }

        public String getOriginalName() {
            return originalName;
        }

        public String getNewName() {
            return newName;
        }

        public void setNewName(String newName) {
            this.newName = newName;
        }

        public int getOffset() {
            return offset;
        }

        public boolean hasNewName() {
            return newName != null && !newName.isEmpty() && !newName.equals(originalName);
        }
    }

    public RenameSymbolsDialog(Project project, List<SymbolRenameInfo> symbols) {
        super(project);
        this.project = project;
        this.symbols = symbols;
        init();
        setTitle("Rename Symbols");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create header with search field
        JPanel headerPanel = new JPanel(new BorderLayout());
        JBLabel headerLabel = new JBLabel("Specify new names for symbols in the current file:");
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Add search field
        searchField = new JTextField();
        searchField.setToolTipText("Search symbols");
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(new JBLabel("Search: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filterSymbols();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filterSymbols();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filterSymbols();
            }
        });

        // Add action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton copyToClipboardButton = new JButton("Copy Selected Symbol");
        JButton copyAllButton = new JButton("Copy All Symbols");
        JButton bulkReplaceButton = new JButton("Bulk Find/Replace");

        copyToClipboardButton.addActionListener(e -> copySelectedSymbolToClipboard());
        copyAllButton.addActionListener(e -> copyAllSymbolsToClipboard());
        bulkReplaceButton.addActionListener(e -> showBulkReplaceDialog());

        actionPanel.add(copyToClipboardButton);
        actionPanel.add(copyAllButton);
        actionPanel.add(bulkReplaceButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(headerLabel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);
        topPanel.add(actionPanel, BorderLayout.SOUTH);
        headerPanel.add(topPanel, BorderLayout.NORTH);

        // Create table
        tableModel = new SymbolTableModel(symbols);
        symbolsTable = new JBTable(tableModel);
        symbolsTable.getColumnModel().getColumn(0).setPreferredWidth(200); // Symbol name
        symbolsTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Type
        symbolsTable.getColumnModel().getColumn(2).setPreferredWidth(200); // New name
        symbolsTable.setRowHeight(24);
        symbolsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Custom editor for the new name column to handle enter key properly
        symbolsTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JTextField()) {
            @Override
            public boolean stopCellEditing() {
                // Only accept non-empty values
                String value = (String) getCellEditorValue();
                if (value != null && !value.trim().isEmpty()) {
                    return super.stopCellEditing();
                }
                super.cancelCellEditing();
                return true;
            }
        });

        // Enable copy from original column with Ctrl+C
        symbolsTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("control C"), "copy");
        symbolsTable.getActionMap().put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                copySelectedSymbolToClipboard();
            }
        });

        // Make Tab and Enter keys move between cells more intuitively
        InputMap im = symbolsTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = symbolsTable.getActionMap();

        // Enter key should move down to next row's editable cell
        im.put(KeyStroke.getKeyStroke("ENTER"), "moveDown");
        am.put("moveDown", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int row = symbolsTable.getSelectedRow();
                int col = symbolsTable.getSelectedColumn();

                if (symbolsTable.isEditing()) {
                    symbolsTable.getCellEditor().stopCellEditing();
                }

                if (row < symbolsTable.getRowCount() - 1) {
                    symbolsTable.changeSelection(row + 1, 2, false, false); // 2 is the editable column
                    symbolsTable.editCellAt(row + 1, 2);
                    Component editorComponent = symbolsTable.getEditorComponent();
                    if (editorComponent instanceof JTextField) {
                        ((JTextField) editorComponent).selectAll();
                    }
                }
            }
        });

        // Tab key should move to next row's editable cell after the last column
        symbolsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        symbolsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        JBScrollPane scrollPane = new JBScrollPane(symbolsTable);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        // Create status label
        statusLabel = new JBLabel();
        statusLabel.setForeground(JBColor.RED);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        return mainPanel;
    }

    @Override
    protected void doOKAction() {
        // Stop any active cell editing and commit the value
        if (symbolsTable.isEditing()) {
            int editingRow = symbolsTable.getEditingRow();
            int editingColumn = symbolsTable.getEditingColumn();

            // Get the cell editor and commit its value
            TableCellEditor editor = symbolsTable.getCellEditor(editingRow, editingColumn);
            editor.stopCellEditing();
        }

        if (isValid()) {
            super.doOKAction();
        }
    }

    private boolean isValid() {
        ValidationInfo validationInfo = doValidate();
        if (validationInfo != null) {
            statusLabel.setText(validationInfo.message);
            return false;
        }
        return true;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        for (int i = 0; i < symbols.size(); i++) {
            SymbolRenameInfo symbol = symbols.get(i);
            if (symbol.hasNewName()) {
                String newName = symbol.getNewName();
                if (newName.contains(" ") || newName.contains("\t")) {
                    return new ValidationInfo("New name cannot contain whitespace");
                }
                // Validate symbol names based on Java identifier rules
                if (!newName.matches("[a-zA-Z_$][a-zA-Z0-9_$]*")) {
                    return new ValidationInfo("Invalid symbol name: " + newName);
                }
            }
        }
        return null;
    }

    /**
     * Returns the list of symbols that have a new name specified
     */
    public List<SymbolRenameInfo> getSymbolsToRename() {
        List<SymbolRenameInfo> result = new ArrayList<>();
        for (SymbolRenameInfo symbol : symbols) {
            if (symbol.hasNewName()) {
                result.add(symbol);
            }
        }
        return result;
    }

    /**
     * Copies the selected symbol to clipboard
     */
    private void copySelectedSymbolToClipboard() {
        int[] selectedRows = symbolsTable.getSelectedRows();
        if (selectedRows.length == 0) return;

        StringBuilder clipboardText = new StringBuilder();
        for (int row : selectedRows) {
            clipboardText.append(symbols.get(row).getOriginalName()).append("\n");
        }

        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
            new java.awt.datatransfer.StringSelection(clipboardText.toString()), null
        );
        statusLabel.setForeground(JBColor.GREEN);
        statusLabel.setText("Symbol" + (selectedRows.length > 1 ? "s" : "") + " copied to clipboard");
    }

    /**
     * Copies all symbols to clipboard
     */
    private void copyAllSymbolsToClipboard() {
        StringBuilder clipboardText = new StringBuilder();
        for (SymbolRenameInfo symbol : symbols) {
            clipboardText.append(symbol.getOriginalName()).append("\n");
        }

        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
            new java.awt.datatransfer.StringSelection(clipboardText.toString()), null
        );
        statusLabel.setForeground(JBColor.GREEN);
        statusLabel.setText("All symbols copied to clipboard");
    }

    /**
     * Filters the symbols based on search text
     */
    private void filterSymbols() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            for (int i = 0; i < symbolsTable.getRowCount(); i++) {
                symbolsTable.getSelectionModel().removeSelectionInterval(i, i);
            }
            return;
        }

        for (int i = 0; i < symbols.size(); i++) {
            String symbolName = symbols.get(i).getOriginalName().toLowerCase();
            if (symbolName.contains(searchText)) {
                symbolsTable.getSelectionModel().addSelectionInterval(i, i);
                symbolsTable.scrollRectToVisible(symbolsTable.getCellRect(i, 0, true));
            } else {
                symbolsTable.getSelectionModel().removeSelectionInterval(i, i);
            }
        }
    }

    /**
     * Shows a dialog for bulk find/replace operations
     */
    private void showBulkReplaceDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField findField = new JTextField(20);
        JTextField replaceField = new JTextField(20);
        panel.add(new JLabel("Find pattern:"));
        panel.add(findField);
        panel.add(new JLabel("Replace with:"));
        panel.add(replaceField);

        int result = JOptionPane.showConfirmDialog(this.getRootPane(), panel, "Bulk Find/Replace", 
                                             JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String findPattern = findField.getText();
            String replaceWith = replaceField.getText();

            if (findPattern.isEmpty()) return;

            int count = 0;
            for (int i = 0; i < symbols.size(); i++) {
                SymbolRenameInfo symbol = symbols.get(i);
                String originalName = symbol.getOriginalName();

                if (originalName.contains(findPattern)) {
                    String newName = originalName.replace(findPattern, replaceWith);
                    symbol.setNewName(newName);
                    count++;
                }
            }

            tableModel.fireTableDataChanged();
            statusLabel.setForeground(JBColor.GREEN);
            statusLabel.setText(count + " symbol(s) updated with bulk replace");
        }
    }

    private class SymbolTableModel extends AbstractTableModel {
        private final List<SymbolRenameInfo> symbols;
        private final String[] columnNames = {"Symbol", "Type", "New Name"};

        public SymbolTableModel(List<SymbolRenameInfo> symbols) {
            this.symbols = symbols;
        }

        @Override
        public int getRowCount() {
            return symbols.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2; // Only new name column is editable
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SymbolRenameInfo symbol = symbols.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> symbol.getOriginalName();
                case 1 -> getSymbolTypeName(symbol.getElement());
                case 2 -> symbol.getNewName();
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 2) {
                SymbolRenameInfo symbol = symbols.get(rowIndex);
                symbol.setNewName((String) value);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        /**
         * Determines the symbol type name from the PsiElement
         */
        private String getSymbolTypeName(PsiNamedElement element) {
            String className = element.getClass().getSimpleName();

            if (className.contains("Class")) return "Class";
            if (className.contains("Method")) return "Method";
            if (className.contains("Field")) return "Field";
            if (className.contains("Variable")) return "Variable";
            if (className.contains("Parameter")) return "Parameter";
            if (className.contains("Interface")) return "Interface";
            if (className.contains("Enum")) return "Enum";
            if (className.contains("Constant")) return "Constant";

            return className;
        }
    }
}
