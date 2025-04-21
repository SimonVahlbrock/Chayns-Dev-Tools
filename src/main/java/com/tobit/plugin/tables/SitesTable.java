package com.tobit.plugin.tables;

import com.intellij.icons.AllIcons;
import com.tobit.plugin.controller.SitesController;
import com.tobit.plugin.views.SitesPanel.SiteTableModel;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class SitesTable extends DataTable<JSONObject, SiteTableModel> {
    private final SitesController controller;

    public SitesTable(SiteTableModel model, SitesController controller) {
        super(model);
        this.controller = controller;
    }

    @Override
    protected JPopupMenu createContextMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        // Get Token menu item
        JMenuItem getTokenMenuItem = new JMenuItem("Get Token");
        getTokenMenuItem.setIcon(AllIcons.Actions.Copy);
        getTokenMenuItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && !isSeparator(row)) {
                JSONObject site = getItemAt(row);
                String siteId = site.optString("siteId", "");
                if (!siteId.isEmpty()) {
                    controller.getTokenForSite(siteId);
                } else {
                    // controller.showWarning("No Site ID available for this entry");
                }
            }
        });

        // Open in Browser menu item
        JMenuItem openInBrowserMenuItem = new JMenuItem("Open in Browser");
        openInBrowserMenuItem.setIcon(AllIcons.Nodes.PpWeb);
        openInBrowserMenuItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && !isSeparator(row)) {
                JSONObject site = getItemAt(row);
                String siteId = site.optString("siteId", "");
                if (!siteId.isEmpty()) {
                    controller.openSiteInBrowser(siteId);
                } else {
                    // controller.showWarning("No Site ID available for this entry");
                }
            }
        });

        // Save menu item
        JMenuItem saveMenuItem = new JMenuItem("Save");
        saveMenuItem.setName("saveItem");
        saveMenuItem.setIcon(AllIcons.Actions.MenuSaveall);
        saveMenuItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && !isSeparator(row)) {
                JSONObject site = getItemAt(row);
                controller.saveSite(site);
            }
        });

        // Remove menu item
        JMenuItem removeMenuItem = new JMenuItem("Remove");
        removeMenuItem.setName("removeItem");
        removeMenuItem.setIcon(AllIcons.Actions.Cancel);
        removeMenuItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && !isSeparator(row)) {
                JSONObject site = getItemAt(row);
                controller.removeSite(site.optInt("locationId", 0));
            }
        });

        // Copy selected column values
        JMenuItem copySelectedColumnMenuItem = new JMenuItem("Copy Selected Values");
        copySelectedColumnMenuItem.setName("columnCopyItem");
        copySelectedColumnMenuItem.setIcon(AllIcons.Actions.Copy);
        copySelectedColumnMenuItem.addActionListener(e -> copySelectedValues());

        popupMenu.add(getTokenMenuItem);
        popupMenu.add(openInBrowserMenuItem);
        popupMenu.addSeparator();
        popupMenu.add(saveMenuItem);
        popupMenu.add(removeMenuItem);
        popupMenu.add(copySelectedColumnMenuItem);

        return popupMenu;
    }

    @Override
    protected boolean isSeparator(int row) {
        return model.isSeparator(row);
    }

    @Override
    protected JSONObject getItemAt(int row) {
        return model.getSiteAt(row);
    }

    @Override
    protected boolean isItemSaved(JSONObject item) {
        return controller.isSiteSaved(item.optInt("locationId", 0));
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