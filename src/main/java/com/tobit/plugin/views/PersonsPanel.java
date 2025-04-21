package com.tobit.plugin.views;

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.tobit.plugin.controller.PersonsController;
import com.tobit.plugin.models.data.Person;
import com.tobit.plugin.tables.PersonsTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for searching and managing chayns persons.
 */
public class PersonsPanel {
    private final JPanel panel = new JPanel(new BorderLayout());
    private final PersonTableModel resultModel = new PersonTableModel();
    private final PersonsController controller;
    private SearchTextField searchField;

    public PersonsPanel(PersonsController controller) {
        this.controller = controller;
        setupUI();
        setupKeyboardShortcuts();
    }

    /**
     * Returns the configured panel for display
     */
    public JPanel getPanel() {
        return panel;
    }

    /**
     * Updates the UI with fresh data
     */
    public void updateData(List<Person> savedPersons, List<Person> searchResults) {
        resultModel.updateData(savedPersons, searchResults);
    }

    /**
     * Performs a search operation using the provided text.
     * Used by external actions like SearchPersonAction.
     */
    public void searchForText(String text) {
        // Select Persons tab in the parent tabbed pane
        Container parent = SwingUtilities.getAncestorOfClass(JBTabbedPane.class, panel);
        if (parent instanceof JBTabbedPane tabbedPane) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if ("Persons".equals(tabbedPane.getTitleAt(i))) {
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

    /**
     * Shows a warning message dialog
     */
    public void showWarning(String message) {
        JOptionPane.showMessageDialog(panel, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Shows an error message dialog
     */
    public void showError(String message) {
        JOptionPane.showMessageDialog(panel, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Registers keyboard shortcuts for common operations
     */
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

    /**
     * Initializes and configures UI components
     */
    private void setupUI() {
        // Create search controls panel at top
        JPanel topPanel = createSearchPanel();

        // Create hint label panel
        JPanel hintPanel = createHintPanel();

        // Create result table with context menu
        JBTable resultTable = createResultTable();

        // Assemble main panel
        JPanel searchContainer = new JPanel(new BorderLayout());
        searchContainer.add(topPanel, BorderLayout.NORTH);
        searchContainer.add(hintPanel, BorderLayout.CENTER);

        panel.add(searchContainer, BorderLayout.NORTH);
        panel.add(new JBScrollPane(resultTable), BorderLayout.CENTER);
    }

    /**
     * Creates search panel with input field and action buttons
     */
    private JPanel createSearchPanel() {
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

        // Add filter button
        JCheckBox filterDuplicatesCheckBox = new JCheckBox("Remove Duplicates");
        filterDuplicatesCheckBox.setToolTipText("Filter out duplicate person entries from search results");
        filterDuplicatesCheckBox.addItemListener(e -> {
            boolean filterEnabled = e.getStateChange() == ItemEvent.SELECTED;
            controller.setFilterDuplicates(filterEnabled);
            // Re-run the search with the current filter setting if there's search text
            if (!searchField.getText().isEmpty()) {
                controller.performSearch(searchField.getText());
            }
        });

        // Add custom person button
        JButton addCustomButton = new JButton();
        addCustomButton.setIcon(com.intellij.icons.AllIcons.General.Add);
        addCustomButton.setToolTipText("Add custom person");
        addCustomButton.setPreferredSize(new Dimension(28, 28));
        addCustomButton.setMargin(JBUI.insets(2));
        addCustomButton.addActionListener(e -> showAddCustomPersonDialog());

        // Add components to panel
        topPanel.add(searchField);
        topPanel.add(filterDuplicatesCheckBox);
        topPanel.add(addCustomButton);

        // Add action listener for Enter key in search field
        searchField.getTextEditor().addActionListener(e -> controller.performSearch(searchField.getText()));

        return topPanel;
    }

    /**
     * Shows a dialog for adding a custom person
     */
    private void showAddCustomPersonDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 1));

        // Create text fields with labels
        JTextField fullNameField = new JTextField(20);
        JTextField personIdField = new JTextField(20);
        JTextField userIdField = new JTextField(20);

        // Add components with labels to panel
        panel.add(new JLabel("Full Name:"));
        panel.add(fullNameField);
        panel.add(new JLabel("Person ID:"));
        panel.add(personIdField);
        panel.add(new JLabel("User ID:"));
        panel.add(userIdField);

        // Show dialog
        int result = JOptionPane.showConfirmDialog(
                this.panel,
                panel,
                "Add Custom Person",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        // Process result if OK was clicked
        if (result == JOptionPane.OK_OPTION) {
            String fullName = fullNameField.getText().trim();
            String personId = personIdField.getText().trim();
            String userIdText = userIdField.getText().trim();

            // Validate input
            if (fullName.isEmpty() || personId.isEmpty() || userIdText.isEmpty()) {
                JOptionPane.showMessageDialog(this.panel,
                        "All fields are required",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Parse userId
            int userId;
            try {
                userId = Integer.parseInt(userIdText);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this.panel,
                        "User ID must be a valid number",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Split full name into first and last name
            String firstName, lastName;
            int spaceIndex = fullName.indexOf(' ');
            if (spaceIndex > 0) {
                firstName = fullName.substring(0, spaceIndex);
                lastName = fullName.substring(spaceIndex + 1);
            } else {
                firstName = fullName;
                lastName = "";
            }

            // Create and add person
            controller.addCustomPerson(firstName, lastName, personId, userId);
        }
    }

    /**
     * Creates a hint label panel with usage instructions
     */
    private JPanel createHintPanel() {
        JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel hintLabel = new JLabel(
                "Search for persons (Ctrl+F). Click on cells to copy values. Right-click for options."
        );
        hintLabel.setForeground(JBColor.GRAY);
        Font currentFont = hintLabel.getFont();
        hintLabel.setFont(currentFont.deriveFont(currentFont.getSize() - 1f));
        hintPanel.add(hintLabel);
        return hintPanel;
    }

    /**
     * Creates and configures the result table with cell renderer and mouse listeners
     */
    // Update the createResultTable method to enable column selection
    private JBTable createResultTable() {
        PersonsTable personsTable = new PersonsTable(resultModel, controller);
        return personsTable.getTable();
    }

    /**
     * Table model for displaying person data with saved status
     */
    public class PersonTableModel extends AbstractTableModel {
        private final List<Pair<Person, Boolean>> persons = new ArrayList<>(); // Person and saved status (null for separator)
        private final String[] columns = {"User Name", "Person ID", "User ID"};

        /**
         * Updates the model with new data
         */
        public void updateData(List<Person> savedPersons, List<Person> searchResults) {
            persons.clear();

            // Add search results if available
            for (Person person : searchResults) {
                persons.add(new Pair<>(person, controller.isPersonSaved(person.personId())));
            }

            // Find remaining saved persons not in search results
            List<Person> remainingSavedPersons = new ArrayList<>();
            for (Person savedPerson : savedPersons) {
                boolean found = false;
                for (Person searchResult : searchResults) {
                    if (savedPerson.personId().equals(searchResult.personId())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    remainingSavedPersons.add(savedPerson);
                }
            }

            // Add separator if both lists have content
            if (!searchResults.isEmpty() && !remainingSavedPersons.isEmpty()) {
                persons.add(new Pair<>(null, false)); // Separator
            }

            // Add saved persons not in search results
            for (Person person : remainingSavedPersons) {
                persons.add(new Pair<>(person, true));
            }

            fireTableDataChanged();
        }

        /**
         * Checks if a row is a separator
         */
        public boolean isSeparator(int rowIndex) {
            return persons.get(rowIndex).first == null;
        }

        /**
         * Returns person object at the given row
         */
        public Person getPersonAt(int rowIndex) {
            Person person = persons.get(rowIndex).first;
            if (person == null) {
                throw new IllegalStateException("No person at index " + rowIndex);
            }
            return person;
        }

        @Override
        public int getRowCount() {
            return persons.size();
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
            Pair<Person, Boolean> pair = persons.get(rowIndex);
            Person person = pair.first;

            // Handle separator row
            if (person == null) {
                return columnIndex == 0 ? "Saved Persons" : "";
            }

            return switch (columnIndex) {
                case 0 -> person.userName();
                case 1 -> person.personId();
                case 2 -> person.userId();
                default -> "";
            };
        }
    }

    // Simple Pair class implementation
        private record Pair<F, S>(F first, S second) {
    }

    public void reloadData() {
        controller.reloadPersons();
    }
}