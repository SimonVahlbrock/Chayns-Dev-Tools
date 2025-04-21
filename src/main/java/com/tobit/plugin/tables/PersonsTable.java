package com.tobit.plugin.tables;

import com.intellij.icons.AllIcons;
import com.tobit.plugin.controller.PersonsController;
import com.tobit.plugin.models.data.Person;
import com.tobit.plugin.views.PersonsPanel.PersonTableModel;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class PersonsTable extends DataTable<Person, PersonTableModel> {
    private final PersonsController controller;

    public PersonsTable(PersonTableModel model, PersonsController controller) {
        super(model);
        this.controller = controller;
    }

    @Override
    protected JPopupMenu createContextMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        // Save menu item
        JMenuItem saveMenuItem = new JMenuItem("Save");
        saveMenuItem.setName("saveItem");
        saveMenuItem.setIcon(AllIcons.Actions.MenuSaveall);
        saveMenuItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && !isSeparator(row)) {
                Person person = getItemAt(row);
                controller.savePerson(person);
            }
        });

        // Remove menu item
        JMenuItem removeMenuItem = new JMenuItem("Remove");
        removeMenuItem.setName("removeItem");
        removeMenuItem.setIcon(AllIcons.Actions.Cancel);
        removeMenuItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && !isSeparator(row)) {
                Person person = getItemAt(row);
                controller.removePerson(person.personId());
            }
        });

        // Copy selected column values
        JMenuItem copySelectedColumnMenuItem = new JMenuItem("Copy Selected Values");
        copySelectedColumnMenuItem.setName("columnCopyItem");
        copySelectedColumnMenuItem.setIcon(AllIcons.Actions.Copy);
        copySelectedColumnMenuItem.addActionListener(e -> copySelectedValues());

        popupMenu.add(saveMenuItem);
        popupMenu.add(removeMenuItem);
        popupMenu.addSeparator();
        popupMenu.add(copySelectedColumnMenuItem);

        return popupMenu;
    }

    @Override
    protected boolean isSeparator(int row) {
        return model.isSeparator(row);
    }

    @Override
    protected Person getItemAt(int row) {
        return model.getPersonAt(row);
    }

    @Override
    protected boolean isItemSaved(Person item) {
        return controller.isPersonSaved(item.personId());
    }

    @Override
    protected void copyColumnValues(int columnIndex) {
        List<String> columnValues = controller.getColumnValues(columnIndex);
        String formattedValues = columnValues.stream()
                .map(value -> value.matches("\\d+") ? value : "'" + value + "'")
                .collect(Collectors.joining(", "));
        StringSelection selection = new StringSelection(formattedValues);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    @Override
    protected void copyValueToClipboard(String value) {
        controller.copyToClipboard(value);
    }
}