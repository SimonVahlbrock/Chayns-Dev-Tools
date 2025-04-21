package com.tobit.plugin.tables;

import com.intellij.ui.Gray;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Abstract class for common table functionality across panels
 */
public abstract class DataTable<T, M extends AbstractTableModel> {
    protected final JBTable table;
    protected final M model;

    public DataTable(M model) {
        this.model = model;
        this.table = createTable();
        setupTable();
    }

    public JBTable getTable() {
        return table;
    }

    protected abstract JPopupMenu createContextMenu();
    protected abstract boolean isSeparator(int row);
    protected abstract T getItemAt(int row);
    protected abstract boolean isItemSaved(T item);

    private void setupTable() {
        table.setModel(model);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setCellSelectionEnabled(true);
        table.setDefaultRenderer(Object.class, createTableCellRenderer());
        table.getTableHeader().setReorderingAllowed(false);

        // Register keyboard shortcut for copy
        table.registerKeyboardAction(
                e -> copySelectedValues(),
                KeyStroke.getKeyStroke(
                        java.awt.event.KeyEvent.VK_C,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
                ),
                JComponent.WHEN_FOCUSED
        );

        setupHeaderContextMenu();
        JPopupMenu popupMenu = createContextMenu();
        table.putClientProperty("popupMenu", popupMenu);
        setupMouseListeners();
    }

    private JBTable createTable() {
        return new JBTable(model) {
            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                // If trying to select a different column while cells are already selected
                if (getSelectedColumn() != -1 && columnIndex != getSelectedColumn() && getSelectedRowCount() > 0) {
                    // Clear current selection and then select the new cell
                    clearSelection();
                    super.changeSelection(rowIndex, columnIndex, false, false);
                    return;
                }
                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            }
        };
    }

    protected void copySelectedValues() {
        int[] selectedRows = table.getSelectedRows();
        int selectedColumn = table.getSelectedColumn();

        if (selectedRows.length > 0 && selectedColumn >= 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < selectedRows.length; i++) {
                int row = selectedRows[i];
                if (isSeparator(row)) {
                    continue;
                }

                Object value = table.getValueAt(row, selectedColumn);
                String valueStr = value != null ? value.toString() : "";

                // Format values - wrap non-numeric values in single quotes
                if (!valueStr.isEmpty()) {
                    if (valueStr.matches("\\d+")) {
                        sb.append(valueStr);
                    } else {
                        sb.append("'").append(valueStr).append("'");
                    }

                    if (i < selectedRows.length - 1) {
                        sb.append(", ");
                    }
                }
            }

            StringSelection selection = new StringSelection(sb.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        }
    }

    private DefaultTableCellRenderer createTableCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column
            ) {
                Component component = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column
                );

                if (isSeparator(row)) {
                    component.setBackground(Gray._230);
                    component.setForeground(Gray._100);
                    ((JLabel) component).setHorizontalAlignment(JLabel.CENTER);
                    component.setFont(component.getFont().deriveFont(Font.BOLD));
                } else {
                    component.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                    component.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                    ((JLabel) component).setHorizontalAlignment(JLabel.LEFT);
                    component.setFont(table.getFont());
                }

                return component;
            }
        };
    }

    private void setupHeaderContextMenu() {
        JPopupMenu headerPopupMenu = new JPopupMenu();
        JMenuItem copyColumnValuesMenuItem = new JMenuItem("Copy Column");
        copyColumnValuesMenuItem.addActionListener(e -> {
            int columnIndex = table.getTableHeader().columnAtPoint(
                    table.getTableHeader().getMousePosition()
            );
            if (columnIndex >= 0) {
                copyColumnValues(columnIndex);
            }
        });

        headerPopupMenu.add(copyColumnValuesMenuItem);

        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            private void showPopup(MouseEvent e) {
                headerPopupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    protected abstract void copyColumnValues(int columnIndex);

    private void setupMouseListeners() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                if (row >= 0 && col >= 0 && !isSeparator(row)
                        && e.getButton() == MouseEvent.BUTTON1
                        && table.getSelectedRowCount() <= 1) {
                    // Left-click - copy value (only when not selecting multiple rows)
                    copyValueToClipboard(table.getValueAt(row, col).toString());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            private void showPopup(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                if (row < 0 || col < 0) {
                    return;
                }

                JPopupMenu popupMenu = (JPopupMenu) table.getClientProperty("popupMenu");

                // Check if multiple rows are selected in the same column
                boolean isColumnSelectionActive = table.getSelectedRowCount() > 1 &&
                        table.getSelectedColumn() == col;

                // Set visibility of menu items
                for (Component component : popupMenu.getComponents()) {
                    if (component instanceof JMenuItem) {
                        // Skip the separator
                        if (component.getName() != null && component.getName().equals("columnCopyItem")) {
                            component.setVisible(isColumnSelectionActive);
                        } else {
                            component.setVisible(!isColumnSelectionActive && !isSeparator(row));
                        }
                    } else if (component instanceof JSeparator) {
                        component.setVisible(!isColumnSelectionActive && !isSeparator(row));
                    }
                }

                if (isColumnSelectionActive) {
                    // Handle column selection popup
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                } else if (!isSeparator(row)) {
                    // Handle regular row popup
                    table.setRowSelectionInterval(row, row);
                    T item = getItemAt(row);
                    boolean saved = isItemSaved(item);

                    // Configure menu items based on saved status
                    JMenuItem saveMenuItem = findMenuItemByName(popupMenu, "saveItem");
                    JMenuItem removeMenuItem = findMenuItemByName(popupMenu, "removeItem");

                    if (saveMenuItem != null) {
                        saveMenuItem.setVisible(!saved);
                    }

                    if (removeMenuItem != null) {
                        removeMenuItem.setVisible(saved);
                    }

                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private JMenuItem findMenuItemByName(JPopupMenu menu, String name) {
        for (Component component : menu.getComponents()) {
            if (component instanceof JMenuItem && name.equals(component.getName())) {
                return (JMenuItem) component;
            }
        }
        return null;
    }

    protected abstract void copyValueToClipboard(String value);
}