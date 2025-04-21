package com.tobit.plugin.views;

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import com.tobit.plugin.controller.SitesController;
import com.tobit.plugin.models.data.LocationItem;
import com.tobit.plugin.tables.SitesTable;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class SitesPanel {
    private final JPanel panel = new JPanel(new BorderLayout());
    private final SiteTableModel resultModel = new SiteTableModel();
    private SearchTextField searchField;
    private final SitesController controller;

    public SitesPanel(SitesController controller) {
        this.controller = controller;
        setupUI();
        setupKeyboardShortcuts();
    }

    public JPanel getPanel() {
        return panel;
    }

    public void updateData(List<LocationItem> savedSites, List<JSONObject> searchResults) {
        resultModel.updateData(savedSites, searchResults);
    }

    public void searchForText(String text) {
        // Select Sites tab in the parent tabbed pane
        Container parent = SwingUtilities.getAncestorOfClass(JBTabbedPane.class, panel);
        if (parent instanceof JBTabbedPane tabbedPane) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if ("Sites".equals(tabbedPane.getTitleAt(i))) {
                    tabbedPane.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Set the search text and trigger search
        searchField.setText(text);
        controller.performSearch(text);

        // Focus the search field
        searchField.getTextEditor().requestFocusInWindow();
    }

    public void showWarning(String message) {
        JOptionPane.showMessageDialog(panel, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(panel, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void setupKeyboardShortcuts() {
        // Register keyboard shortcut for Ctrl+F to focus search field
        InputMap inputMap = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = panel.getActionMap();

        // Register for both Ctrl+F and Cmd+F (for Mac)
        inputMap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                "focusSearch"
        );
        // Also support explicit Ctrl+F for Windows/Linux
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "focusSearch");

        actionMap.put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.getTextEditor().requestFocusInWindow();
                searchField.getTextEditor().selectAll();
            }
        });
    }

    private void setupUI() {
        // Top search panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Replace JTextField with SearchTextField
        searchField = new SearchTextField();
        searchField.setHistorySize(10); // Store recent searches
        searchField.addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                // When text is cleared (either by user or clear button), clear results
                if (searchField.getText().isEmpty()) {
                    controller.clearSearch();
                }
            }
        });

        // Add action listener for Enter key in search field
        searchField.getTextEditor().addActionListener(e -> controller.performSearch(searchField.getText()));

        topPanel.add(searchField);

        // Add filter button
        JCheckBox filterDuplicatesCheckBox = new JCheckBox("Remove Duplicates");
        filterDuplicatesCheckBox.setToolTipText("Filter out duplicate site entries from search results");
        filterDuplicatesCheckBox.addItemListener(e -> {
            boolean filterEnabled = e.getStateChange() == ItemEvent.SELECTED;
            controller.setFilterDuplicates(filterEnabled);
            // Re-run the search with the current filter setting if there's search text
            if (!searchField.getText().isEmpty()) {
                controller.performSearch(searchField.getText());
            }
        });
        topPanel.add(filterDuplicatesCheckBox);

        // Add hint label below search box
        JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel hintLabel = new JLabel(
                "Enter LocationName, SiteId or LocationId (Ctrl+F). Click on cells to copy values. Right-click for options."
        );
        hintLabel.setForeground(JBColor.GRAY);
        Font currentFont = hintLabel.getFont();
        hintLabel.setFont(currentFont.deriveFont(currentFont.getSize() - 1f));
        hintPanel.add(hintLabel);

        // Create result table with context menu
        JBTable resultTable = createResultTable();

        // Assemble main panel
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(topPanel, BorderLayout.NORTH);
        topContainer.add(hintPanel, BorderLayout.CENTER);

        panel.add(topContainer, BorderLayout.NORTH);
        panel.add(new JBScrollPane(resultTable), BorderLayout.CENTER);
    }

    private JBTable createResultTable() {
        SitesTable sitesTable = new SitesTable(resultModel, controller);
        return sitesTable.getTable();
    }

    public static class SiteTableModel extends AbstractTableModel {
        private final List<Pair<JSONObject, Boolean>> sites = new ArrayList<>(); // Site and saved status (null for separator)
        private final String[] columns = {"Location Name", "Site ID", "Location ID", "Location Person ID"};

        public void updateData(List<LocationItem> savedSites, List<JSONObject> searchResults) {
            sites.clear();

            // Add search results if available
            for (JSONObject site : searchResults) {
                int locationId = site.optInt("locationId", 0);
                boolean saved = false;
                for (LocationItem savedSite : savedSites) {
                    if (savedSite.getId() == locationId) {
                        saved = true;
                        break;
                    }
                }
                sites.add(new Pair<>(site, saved));
            }

            // Find remaining saved sites not in search results
            List<LocationItem> remainingSavedSites = new ArrayList<>();
            for (LocationItem savedSite : savedSites) {
                boolean found = false;
                for (JSONObject searchResult : searchResults) {
                    if (searchResult.optInt("locationId", 0) == savedSite.getId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    remainingSavedSites.add(savedSite);
                }
            }

            // Add separator if both lists have content
            if (!searchResults.isEmpty() && !remainingSavedSites.isEmpty()) {
                sites.add(new Pair<>(null, false)); // Separator
            }

            // Add saved sites not in search results
            for (LocationItem locationItem : remainingSavedSites) {
                JSONObject jsonSite = new JSONObject();
                jsonSite.put("locationName", locationItem.getName());
                jsonSite.put("locationId", locationItem.getId());
                jsonSite.put("siteId", locationItem.getSiteId());
                jsonSite.put("locationPersonId", locationItem.getLocationPersonId());
                sites.add(new Pair<>(jsonSite, true));
            }

            fireTableDataChanged();
        }

        public boolean isSeparator(int rowIndex) {
            return sites.get(rowIndex).first == null;
        }

        public JSONObject getSiteAt(int rowIndex) {
            JSONObject site = sites.get(rowIndex).first;
            if (site == null) {
                throw new IllegalStateException("No site at index " + rowIndex);
            }
            return site;
        }

        @Override
        public int getRowCount() {
            return sites.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Pair<JSONObject, Boolean> pair = sites.get(rowIndex);
            JSONObject site = pair.first;

            // Handle separator row
            if (site == null) {
                return columnIndex == 0 ? "Saved Sites" : "";
            }

            return switch (columnIndex) {
                case 0 -> site.optString("locationName", site.optString("name", ""));
                case 1 -> site.optString("siteId", "");
                case 2 -> Integer.toString(site.optInt("locationId", 0));
                case 3 -> site.optString("locationPersonId", "");
                default -> "";
            };
        }
    }

    // Simple Pair class implementation
    private record Pair<F, S>(F first, S second) {
    }

    public void reloadData() {
        controller.reloadSites();
    }
}